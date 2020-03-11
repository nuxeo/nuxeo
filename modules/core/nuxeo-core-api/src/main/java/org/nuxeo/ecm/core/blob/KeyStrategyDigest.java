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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.mutable.MutableObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Represents computation of blob keys based on a message digest.
 *
 * @since 11.1
 */
public class KeyStrategyDigest implements KeyStrategy {

    public final String digestAlgorithm;

    public KeyStrategyDigest(String digestAlgorithm) {
        Objects.requireNonNull(digestAlgorithm);
        this.digestAlgorithm = digestAlgorithm;
    }

    @Override
    public boolean useDeDuplication() {
        return true;
    }

    @Override
    public String getDigestFromKey(String key) {
        return key;
    }

    @Override
    public BlobWriteContext getBlobWriteContext(BlobContext blobContext)  {
        MutableObject<String> keyHolder = new MutableObject<>();
        WriteObserver writeObserver = new WriteObserverDigest(digestAlgorithm, keyHolder::setValue);
        Supplier<String> keyComputer = keyHolder::getValue;
        return new BlobWriteContext(blobContext, writeObserver, keyComputer, this);
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
        if (!(obj instanceof KeyStrategyDigest)) {
            return false;
        }
        KeyStrategyDigest other = (KeyStrategyDigest) obj;
        return digestAlgorithm.equals(other.digestAlgorithm);
    }

    @Override
    public int hashCode() {
        return digestAlgorithm.hashCode();
    }

}
