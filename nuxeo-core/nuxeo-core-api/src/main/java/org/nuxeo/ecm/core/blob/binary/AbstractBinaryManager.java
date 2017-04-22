/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Abstract BinaryManager implementation that provides a few utilities
 *
 * @author Florent Guillaume
 */
public abstract class AbstractBinaryManager implements BinaryManager {

    public static final String MD5_DIGEST = "MD5";

    public static final String SHA1_DIGEST = "SHA-1";

    public static final String SHA256_DIGEST = "SHA-256";

    public static final int MD5_DIGEST_LENGTH = 32;

    public static final int SHA1_DIGEST_LENGTH = 40;

    public static final int SHA256_DIGEST_LENGTH = 64;

    /**
     * @since 7.4
     */
    public static final HashMap<Integer, String> DIGESTS_BY_LENGTH = new HashMap<>();

    public static final String DEFAULT_DIGEST = MD5_DIGEST; // SHA256_DIGEST

    public static final int DEFAULT_DEPTH = 2;

    protected String blobProviderId;

    protected BinaryManagerRootDescriptor descriptor;

    protected BinaryGarbageCollector garbageCollector;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        this.blobProviderId = blobProviderId;
        DIGESTS_BY_LENGTH.put(MD5_DIGEST_LENGTH, MD5_DIGEST);
        DIGESTS_BY_LENGTH.put(SHA1_DIGEST_LENGTH, SHA1_DIGEST);
        DIGESTS_BY_LENGTH.put(SHA256_DIGEST_LENGTH, SHA256_DIGEST);
    }

    /**
     * Creates a binary value from the given input stream.
     */
    // not in the public API of BinaryManager anymore
    abstract protected Binary getBinary(InputStream in) throws IOException;

    /*
     * This abstract implementation just opens the stream.
     */
    @Override
    public Binary getBinary(Blob blob) throws IOException {
        if (blob instanceof BinaryBlob) {
            Binary binary = ((BinaryBlob) blob).getBinary();
            if (binary.getBlobProviderId().equals(blobProviderId)) {
                return binary;
            }
            // don't reuse the binary if it comes from another blob provider
        }
        try (InputStream stream = blob.getStream()) {
            return getBinary(stream);
        }
    }

    @Override
    abstract public Binary getBinary(String digest);

    @Override
    public void removeBinaries(Collection<String> digests) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets existing descriptor or creates a default one.
     */
    protected BinaryManagerRootDescriptor getDescriptor(File configFile) throws IOException {
        BinaryManagerRootDescriptor desc;
        if (configFile.exists()) {
            XMap xmap = new XMap();
            xmap.register(BinaryManagerRootDescriptor.class);
            desc = (BinaryManagerRootDescriptor) xmap.load(new FileInputStream(configFile));
        } else {
            desc = new BinaryManagerRootDescriptor();
            // TODO fetch from repo descriptor
            desc.digest = getDefaultDigestAlgorithm();
            desc.depth = DEFAULT_DEPTH;
            desc.write(configFile); // may throw IOException
        }
        return desc;
    }

    public static final int MIN_BUF_SIZE = 8 * 1024; // 8 kB

    public static final int MAX_BUF_SIZE = 64 * 1024; // 64 kB

    protected String storeAndDigest(InputStream in, OutputStream out) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(getDigestAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw (IOException) new IOException().initCause(e);
        }

        int size = in.available();
        if (size == 0) {
            size = MAX_BUF_SIZE;
        } else if (size < MIN_BUF_SIZE) {
            size = MIN_BUF_SIZE;
        } else if (size > MAX_BUF_SIZE) {
            size = MAX_BUF_SIZE;
        }
        byte[] buf = new byte[size];

        /*
         * Copy and digest.
         */
        int n;
        while ((n = in.read(buf)) != -1) {
            digest.update(buf, 0, n);
            out.write(buf, 0, n);
        }
        out.flush();

        return toHexString(digest.digest());
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    @Override
    public BinaryGarbageCollector getGarbageCollector() {
        return garbageCollector;
    }

    @Override
    public String getDigestAlgorithm() {
        return descriptor.digest;
    }

    /**
     * Gets the default message digest to use to hash binaries.
     *
     * @since 6.0
     */
    protected String getDefaultDigestAlgorithm() {
        return DEFAULT_DIGEST;
    }

}
