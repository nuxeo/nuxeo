/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * A blob store that encrypts binaries on the filesystem using AES.
 *
 * @since 11.1
 */
public class AESBlobStore extends LocalBlobStore {

    protected static final byte[] FILE_MAGIC = "NUXEOCRYPT".getBytes(US_ASCII);

    protected static final int FILE_VERSION_1 = 1;

    protected static final int USE_KEYSTORE = 1;

    protected static final int USE_PBKDF2 = 2;

    // for sanity check during reads
    private static final int MAX_SALT_LEN = 1024;

    // for sanity check during reads
    private static final int MAX_IV_LEN = 1024;

    // Random instances are thread-safe
    protected static final Random RANDOM = new SecureRandom();

    protected final AESBlobStoreConfiguration aesConfig;

    public AESBlobStore(String name, KeyStrategy keyStrategy, PathStrategy pathStrategy,
            AESBlobStoreConfiguration aesConfig) {
        super(name, keyStrategy, pathStrategy);
        this.aesConfig = aesConfig;
    }

    @Override
    protected void write(BlobWriteContext blobWriteContext, Path file) throws IOException {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file));
                EncryptingOutputStream cryptOut = new EncryptingOutputStream(out, aesConfig)) {
            transfer(blobWriteContext, cryptOut);
        }
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        return OptionalOrUnknown.unknown();
    }

    @SuppressWarnings("resource")
    @Override
    public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
        OptionalOrUnknown<InputStream> streamOpt = super.getStream(key);
        if (!streamOpt.isPresent()) {
            return streamOpt;
        }
        try {
            InputStream in = streamOpt.get();
            try {
                return OptionalOrUnknown.of(new DecryptingInputStream(in, aesConfig));
            } catch (IOException e) {
                in.close();
                throw e;
            }
        } catch (NoSuchFileException e) {
            return OptionalOrUnknown.missing();
        }
    }

    @Override
    public boolean readBlob(String key, Path dest) throws IOException {
        OptionalOrUnknown<InputStream> streamOpt = getStream(key);
        if (streamOpt.isPresent()) {
            try (InputStream stream = streamOpt.get()) {
                Files.copy(stream, dest, REPLACE_EXISTING);
            }
            return true;
        } else if (streamOpt.isMissing()) {
            return false;
        } else {
            // this implementation never returns Maybe.unknown()
            throw new IllegalStateException("stream should always be known");
        }
    }

    @Override
    public boolean copyBlobIsOptimized(BlobStore sourceStore) {
        return false;
    }

    @Override
    public boolean copyBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Output stream that encrypts while writing.
     * <p>
     * Stream format version 1 (values are in network order):
     * <ul>
     * <li>10 bytes: magic number "NUXEOCRYPT"
     * <li>1 byte: file format version = 1
     * <li>1 byte: use keystore = 1, use PBKDF2 = 2
     * <li>if use PBKDF2:
     * <ul>
     * <li>4 bytes: salt length = n
     * <li>n bytes: salt data
     * </ul>
     * <li>4 bytes: IV length = p
     * <li>p bytes: IV data
     * <li>x bytes: encrypted stream
     * </ul>
     *
     * @see DecryptingInputStream
     */
    public static class EncryptingOutputStream extends FilterOutputStream {

        protected final AESBlobStoreConfiguration aesConfig;

        public EncryptingOutputStream(OutputStream out, AESBlobStoreConfiguration aesConfig) throws IOException {
            super(out);
            this.aesConfig = aesConfig;
            writeHeader();
        }

        protected void writeHeader() throws IOException {
            // write magic + version
            out.write(FILE_MAGIC);
            DataOutputStream data = new DataOutputStream(out);
            data.writeByte(FILE_VERSION_1);

            Cipher cipher;
            try {
                // secret key
                Key secret;
                if (aesConfig.usePBKDF2) {
                    data.writeByte(USE_PBKDF2);
                    // generate a salt
                    byte[] salt = new byte[16];
                    RANDOM.nextBytes(salt);
                    // generate secret key
                    secret = aesConfig.generateSecretKey(salt);
                    // write salt
                    data.writeInt(salt.length);
                    data.write(salt);
                } else {
                    data.writeByte(USE_KEYSTORE);
                    // find secret key from keystore
                    secret = aesConfig.getSecretKey();
                }

                // cipher
                cipher = aesConfig.getCipher();
                cipher.init(Cipher.ENCRYPT_MODE, secret);

                // write IV
                byte[] iv = cipher.getIV();
                data.writeInt(iv.length);
                data.write(iv);
                data.flush();
            } catch (GeneralSecurityException e) {
                throw new IOException(e);
            }

            // now replace the output stream with the ciphering version
            out = new CipherOutputStream(out, cipher);
        }

        // we don't just delegate to write(int) as it's inefficient (squid:S4349)
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }
    }

    /**
     * Input stream that decrypts while reading.
     * <p>
     * See {@link EncryptingOutputStream} for the stream format.
     *
     * @see EncryptingOutputStream
     */
    public static class DecryptingInputStream extends FilterInputStream {

        protected final AESBlobStoreConfiguration aesConfig;

        public DecryptingInputStream(InputStream in, AESBlobStoreConfiguration aesConfig) throws IOException {
            super(in);
            this.aesConfig = aesConfig;
            readHeader();
        }

        protected void readHeader() throws IOException {
            // read magic
            byte[] magic = new byte[FILE_MAGIC.length];
            IOUtils.read(in, magic);
            if (!Arrays.equals(magic, FILE_MAGIC)) {
                throw new IOException("Invalid file (bad magic)");
            }
            // read version
            DataInputStream data = new DataInputStream(in);
            byte magicvers = data.readByte();
            if (magicvers != FILE_VERSION_1) {
                throw new IOException("Invalid file (bad version)");
            }

            // check use
            byte usepb = data.readByte();
            if (usepb == USE_PBKDF2) {
                if (!aesConfig.usePBKDF2) {
                    throw new IOException("File requires PBKDF2 password");
                }
            } else if (usepb == USE_KEYSTORE) {
                if (aesConfig.usePBKDF2) {
                    throw new IOException("File requires keystore");
                }
            } else {
                throw new IOException("Invalid file (bad use)");
            }

            Cipher cipher;
            try {
                // secret key
                Key secret;
                if (aesConfig.usePBKDF2) {
                    // read salt first
                    int saltLen = data.readInt();
                    if (saltLen <= 0 || saltLen > MAX_SALT_LEN) {
                        throw new IOException("Invalid salt length: " + saltLen);
                    }
                    byte[] salt = new byte[saltLen];
                    data.read(salt, 0, saltLen);
                    secret = aesConfig.generateSecretKey(salt);
                } else {
                    secret = aesConfig.getSecretKey();
                }

                // read IV
                int ivLen = data.readInt();
                if (ivLen <= 0 || ivLen > MAX_IV_LEN) {
                    throw new IOException("Invalid IV length: " + ivLen);
                }
                byte[] iv = new byte[ivLen];
                data.read(iv, 0, ivLen);

                // cipher
                cipher = aesConfig.getCipher();
                cipher.init(Cipher.DECRYPT_MODE, secret, aesConfig.getParameterSpec(iv));
            } catch (GeneralSecurityException e) {
                throw new IOException(e);
            }

            // now replace the input stream with the deciphering version
            in = new CipherInputStream(in, cipher);
        }
    }

}
