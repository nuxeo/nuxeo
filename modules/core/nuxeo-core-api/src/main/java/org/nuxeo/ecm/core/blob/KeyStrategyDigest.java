/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.fasterxml.uuid.Generators;

/**
 * Represents computation of blob keys based on a message digest.
 * <p>
 * When a {@code maxSize} threshold is configured, blobs strictly larger than the threshold get a UUIDv7 key instead of
 * a content-based digest key. This avoids unbounded digest computation for very large files (which can exceed Kafka
 * poll intervals when computed asynchronously).
 *
 * @since 11.1
 */
public class KeyStrategyDigest implements KeyStrategy {

    /**
     * UUIDv7 pattern: version digit {@code 7}, variant bits {@code 10xx} (hex digit in {@code 8|9|a|b}).
     *
     * @since 2025.19
     */
    protected static final Pattern UUID_V7_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    public final String digestAlgorithm;

    public final Pattern digestPattern;

    /**
     * Size threshold. Blobs strictly larger than this size get a UUIDv7 key instead of a digest key.
     * {@link ByteSize#unlimited()} disables the threshold (always use digest keys).
     *
     * @since 2025.19
     */
    public final ByteSize maxSize;

    public KeyStrategyDigest(String digestAlgorithm) {
        this(digestAlgorithm, ByteSize.unlimited());
    }

    /**
     * @param digestAlgorithm the digest algorithm (e.g. {@code MD5})
     * @param maxSize the size threshold; blobs strictly larger than this get a UUIDv7 key. {@link ByteSize#unlimited()}
     *            disables the threshold (always use digest keys).
     * @since 2025.19
     */
    public KeyStrategyDigest(String digestAlgorithm, ByteSize maxSize) {
        Objects.requireNonNull(digestAlgorithm);
        Objects.requireNonNull(maxSize);
        this.digestAlgorithm = digestAlgorithm;
        this.digestPattern = getDigestPattern(digestAlgorithm);
        this.maxSize = maxSize;
    }

    @Override
    public boolean useDeDuplication() {
        return true;
    }

    @Override
    public String getDigestFromKey(String key) {
        // UUIDv7 keys are not content-based digests, so they are not returned here
        return isValidDigest(key) ? key : null;
    }

    public boolean isValidDigest(String key) {
        return digestPattern.matcher(key).matches();
    }

    protected static Pattern getDigestPattern(String digestAlgorithm) {
        // compute a dummy digest (from 0-length input) to know its length and derive a regexp
        int len = new DigestUtils(digestAlgorithm).digestAsHex(new byte[0]).length();
        return Pattern.compile("[0-9a-f]{" + len + "}");
    }

    @Override
    public BlobWriteContext getBlobWriteContext(BlobContext blobContext) {
        if (isAboveThreshold(blobContext)) {
            // large blob: use UUIDv7 key, no write observer (no digest computation)
            String key = generateUUIDv7Key();
            return new BlobWriteContext(blobContext, null, () -> key, this);
        }
        MutableObject<String> keyHolder = new MutableObject<>();
        WriteObserver writeObserver = new WriteObserverDigest(digestAlgorithm, keyHolder::setValue);
        Supplier<String> keyComputer = keyHolder::getValue;
        return new BlobWriteContext(blobContext, writeObserver, keyComputer, this);
    }

    /**
     * @return {@code true} if a threshold is configured and the blob is strictly larger than it
     * @since 2025.19
     */
    protected boolean isAboveThreshold(BlobContext blobContext) {
        if (!hasThreshold() || blobContext.blob == null) {
            return false;
        }
        long length = blobContext.blob.getLength();
        return length >= 0 && length > maxSize.bytes();
    }

    /**
     * @return {@code true} if a finite size threshold is configured
     * @since 2025.19
     */
    protected boolean hasThreshold() {
        return maxSize.bytes() >= 0;
    }

    /**
     * @since 2025.19
     */
    protected String generateUUIDv7Key() {
        return Generators.timeBasedEpochGenerator().generate().toString();
    }

    /**
     * @return {@code true} if the given key matches the UUIDv7 format
     * @since 2025.19
     */
    public static boolean isUUIDv7(String key) {
        return key != null && UUID_V7_PATTERN.matcher(key).matches();
    }

    /**
     * Write observer computing a digest. The final digest is made available to the key consumer.
     *
     * @since 11.1
     */
    public static class WriteObserverDigest implements WriteObserver {

        protected final MessageDigest messageDigest;

        protected final Consumer<String> keyConsumer;

        protected DigestOutputStream dos;

        public WriteObserverDigest(String digestAlgorithm, Consumer<String> keyConsumer) {
            try {
                messageDigest = MessageDigest.getInstance(digestAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new NuxeoException(e);
            }
            this.keyConsumer = keyConsumer;
        }

        @Override
        public OutputStream wrap(OutputStream out) {
            dos = new DigestOutputStream(out, messageDigest);
            return dos;
        }

        @Override
        public void done() {
            String key = Hex.encodeHexString(dos.getMessageDigest().digest());
            keyConsumer.accept(key);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KeyStrategyDigest other)) {
            return false;
        }
        return digestAlgorithm.equals(other.digestAlgorithm) && Objects.equals(maxSize, other.maxSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digestAlgorithm, maxSize);
    }

    @Override
    public String toString() {
        if (!hasThreshold()) {
            return getClass().getSimpleName() + "(" + digestAlgorithm + ")";
        }
        return getClass().getSimpleName() + "(" + digestAlgorithm + ", maxSize=" + maxSize + ")";
    }

    @Override
    public boolean isValidKey(String key) {
        // accept the content digest, and (when threshold is configured) UUIDv7 keys
        return isValidDigest(key) || (hasThreshold() && isUUIDv7(key));
    }

}
