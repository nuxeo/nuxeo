/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob.binary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * A binary manager that encrypts binaries on the filesystem using AES.
 * <p>
 * The configuration holds the keystore information to retrieve the AES key, or the password that is used to generate a
 * per-file key using PBKDF2. This configuration comes from the {@code <property name="key">...</property>} of the
 * binary manager configuration.
 * <p>
 * The configuration has the form {@code key1=value1,key2=value2,...} where the possible keys are, for keystore use:
 * <ul>
 * <li>keyStoreType: the keystore type, for instance JCEKS
 * <li>keyStoreFile: the path to the keystore, if applicable
 * <li>keyStorePassword: the keystore password
 * <li>keyAlias: the alias (name) of the key in the keystore
 * <li>keyPassword: the key password
 * </ul>
 * <p>
 * And for PBKDF2 use:
 * <ul>
 * <li>password: the password
 * </ul>
 * <p>
 * To encrypt a binary, an AES key is needed. This key can be retrieved from a keystore, or generated from a password
 * using PBKDF2 (in which case each stored file contains a different salt for security reasons). The file format is
 * described in {@link #storeAndDigest(InputStream, OutputStream)}.
 * <p>
 * While the binary is being used by the application, a temporarily-decrypted file is held in a temporary directory. It
 * is removed as soon as possible.
 * <p>
 * Note: if the Java Cryptographic Extension (JCE) is not configured for 256-bit key length, you may get an exception
 * "java.security.InvalidKeyException: Illegal key size or default parameters". If this is the case, go to
 * <a href="http://www.oracle.com/technetwork/java/javase/downloads/index.html" >Oracle Java SE Downloads</a> and
 * download and install the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files for your JDK.
 *
 * @since 6.0
 */
public class AESBinaryManager extends LocalBinaryManager {

    private static final Log log = LogFactory.getLog(AESBinaryManager.class);

    protected static final byte[] FILE_MAGIC = new byte[] { 'N', 'U', 'X', 'E', 'O', 'C', 'R', 'Y', 'P', 'T' };

    protected static final int FILE_VERSION_1 = 1;

    protected static final int USE_KEYSTORE = 1;

    protected static final int USE_PBKDF2 = 2;

    protected static final String AES = "AES";

    // insecure, see https://find-sec-bugs.github.io/bugs.htm#PADDING_ORACLE
    protected static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";

    protected static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    protected static final String PBKDF2_WITH_HMAC_SHA1 = "PBKDF2WithHmacSHA1";

    protected static final int PBKDF2_ITERATIONS = 10000;

    // AES-256
    protected static final int PBKDF2_KEY_LENGTH = 256;

    protected static final String PARAM_PASSWORD = "password";

    protected static final String PARAM_KEY_STORE_TYPE = "keyStoreType";

    protected static final String PARAM_KEY_STORE_FILE = "keyStoreFile";

    protected static final String PARAM_KEY_STORE_PASSWORD = "keyStorePassword";

    protected static final String PARAM_KEY_ALIAS = "keyAlias";

    protected static final String PARAM_KEY_PASSWORD = "keyPassword";

    /**
     * If {@code true}, use the insecure AES/CBC/PKCS5Padding for encryption. The default is {@code false}, to use
     * AES/GCM/NoPadding.
     *
     * @since 10.3
     */
    protected static final String PARAM_KEY_USE_INSECURE_CIPHER = "useInsecureCipher";

    // for sanity check during reads
    private static final int MAX_SALT_LEN = 1024;

    // for sanity check during reads
    private static final int MAX_IV_LEN = 1024;

    // Random instances are thread-safe
    protected static final Random RANDOM = new SecureRandom();

    // the digest from the root descriptor
    protected String digestAlgorithm;

    protected boolean usePBKDF2;

    protected String password;

    protected String keyStoreType;

    protected String keyStoreFile;

    protected String keyStorePassword;

    protected String keyAlias;

    protected String keyPassword;

    protected boolean useInsecureCipher;

    public AESBinaryManager() {
        setUnlimitedJCEPolicy();
    }

    /**
     * By default the JRE may ship with restricted key length. Instead of having administrators download the Java
     * Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files from
     * http://www.oracle.com/technetwork/java/javase/downloads/index.html, we attempt to directly unrestrict the JCE
     * using reflection.
     * <p>
     * This is not possible anymore since 8u102 and https://bugs.openjdk.java.net/browse/JDK-8149417
     */
    protected static boolean setUnlimitedJCEPolicy() {
        try {
            Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);
            if (Boolean.TRUE.equals(field.get(null))) {
                log.info("Setting JCE Unlimited Strength");
                field.set(null, Boolean.FALSE);
            }
            return true;
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            log.debug("Cannot check/set JCE Unlimited Strength", e);
            return false;
        }
    }

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        digestAlgorithm = getDigestAlgorithm();
        String options = properties.get(BinaryManager.PROP_KEY);
        // TODO parse options from properties directly
        if (StringUtils.isBlank(options)) {
            throw new NuxeoException("Missing key for " + getClass().getSimpleName());
        }
        initializeOptions(options);
    }

    protected void initializeOptions(String options) {
        for (String option : options.split(",")) {
            String[] split = option.split("=", 2);
            if (split.length != 2) {
                throw new NuxeoException("Unrecognized option: " + option);
            }
            String value = StringUtils.defaultIfBlank(split[1], null);
            switch (split[0]) {
            case PARAM_PASSWORD:
                password = value;
                break;
            case PARAM_KEY_STORE_TYPE:
                keyStoreType = value;
                break;
            case PARAM_KEY_STORE_FILE:
                keyStoreFile = value;
                break;
            case PARAM_KEY_STORE_PASSWORD:
                keyStorePassword = value;
                break;
            case PARAM_KEY_ALIAS:
                keyAlias = value;
                break;
            case PARAM_KEY_PASSWORD:
                keyPassword = value;
                break;
            case PARAM_KEY_USE_INSECURE_CIPHER:
                useInsecureCipher = Boolean.parseBoolean(value);
                break;
            default:
                throw new NuxeoException("Unrecognized option: " + option);
            }
        }
        usePBKDF2 = password != null;
        if (usePBKDF2) {
            if (keyStoreType != null) {
                throw new NuxeoException("Cannot use " + PARAM_KEY_STORE_TYPE + " with " + PARAM_PASSWORD);
            }
            if (keyStoreFile != null) {
                throw new NuxeoException("Cannot use " + PARAM_KEY_STORE_FILE + " with " + PARAM_PASSWORD);
            }
            if (keyStorePassword != null) {
                throw new NuxeoException("Cannot use " + PARAM_KEY_STORE_PASSWORD + " with " + PARAM_PASSWORD);
            }
            if (keyAlias != null) {
                throw new NuxeoException("Cannot use " + PARAM_KEY_ALIAS + " with " + PARAM_PASSWORD);
            }
            if (keyPassword != null) {
                throw new NuxeoException("Cannot use " + PARAM_KEY_PASSWORD + " with " + PARAM_PASSWORD);
            }
        } else {
            if (keyStoreType == null) {
                throw new NuxeoException("Missing " + PARAM_KEY_STORE_TYPE);
            }
            // keystore file is optional
            if (keyStoreFile == null && keyStorePassword != null) {
                throw new NuxeoException("Missing " + PARAM_KEY_STORE_PASSWORD);
            }
            if (keyAlias == null) {
                throw new NuxeoException("Missing " + PARAM_KEY_ALIAS);
            }
            if (keyPassword == null) {
                keyPassword = keyStorePassword;
            }
        }
    }

    /**
     * Gets the password for PBKDF2.
     * <p>
     * The caller must clear it from memory when done with it by calling {@link #clearPassword}.
     */
    protected char[] getPassword() {
        return password.toCharArray();
    }

    /**
     * Clears a password from memory.
     */
    protected void clearPassword(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Generates an AES key from the password using PBKDF2.
     *
     * @param salt the salt
     */
    protected Key generateSecretKey(byte[] salt) throws GeneralSecurityException {
        char[] password = getPassword();
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA1);
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        clearPassword(password);
        Key derived = factory.generateSecret(spec);
        spec.clearPassword();
        return new SecretKeySpec(derived.getEncoded(), AES);
    }

    /**
     * Gets the AES key from the keystore.
     */
    protected Key getSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] kspw = keyStorePassword == null ? null : keyStorePassword.toCharArray();
        if (keyStoreFile != null) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile))) {
                keyStore.load(in, kspw);
            }
        } else {
            // some keystores are not backed by a file
            keyStore.load(null, kspw);
        }
        clearPassword(kspw);
        char[] kpw = keyPassword == null ? null : keyPassword.toCharArray();
        Key key = keyStore.getKey(keyAlias, kpw);
        clearPassword(kpw);
        return key;
    }

    @Override
    protected Binary getBinary(InputStream in) throws IOException {
        // write to a tmp file that will be used by the returned Binary
        // TODO if stream source, avoid copy (no-copy optimization)
        File tmp = File.createTempFile("bin_", ".tmp", tmpDir);
        Framework.trackFile(tmp, tmp);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
        IOUtils.copy(in, out);
        in.close();
        out.close();
        // encrypt an digest into final file
        InputStream nin = new BufferedInputStream(new FileInputStream(tmp));
        String digest = storeAndDigest(nin); // calls our storeAndDigest
        // return a binary on our tmp file
        return new Binary(tmp, digest, blobProviderId);
    }

    @Override
    public Binary getBinary(String digest) {
        File file = getFileForDigest(digest, false);
        if (file == null) {
            log.warn("Invalid digest format: " + digest);
            return null;
        }
        if (!file.exists()) {
            return null;
        }
        File tmp;
        try {
            tmp = File.createTempFile("bin_", ".tmp", tmpDir);
            Framework.trackFile(tmp, tmp);
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
                    InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                decrypt(in, out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // return a binary on our tmp file
        return new Binary(tmp, digest, blobProviderId);
    }

    @Override
    protected String storeAndDigest(InputStream in) throws IOException {
        File tmp = File.createTempFile("create_", ".tmp", tmpDir);
        /*
         * First, write the input stream to a temporary file, while computing a digest.
         */
        try {
            String digest;
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
                digest = storeAndDigest(in, out);
            } finally {
                in.close();
            }
            /*
             * Move the tmp file to its destination.
             */
            File file = getFileForDigest(digest, true);
            atomicMove(tmp, file);
            return digest;
        } finally {
            tmp.delete();
        }
    }

    /**
     * Encrypts the given input stream into the given output stream, while also computing the digest of the input
     * stream.
     * <p>
     * File format version 1 (values are in network order):
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
     * @param in the input stream containing the data
     * @param out the output stream into write
     * @return the digest of the input stream
     */
    @Override
    public String storeAndDigest(InputStream in, OutputStream out) throws IOException {
        out.write(FILE_MAGIC);
        DataOutputStream data = new DataOutputStream(out);
        data.writeByte(FILE_VERSION_1);

        try {
            // get digest to use
            MessageDigest messageDigest = MessageDigest.getInstance(digestAlgorithm);

            // secret key
            Key secret;
            if (usePBKDF2) {
                data.writeByte(USE_PBKDF2);
                // generate a salt
                byte[] salt = new byte[16];
                RANDOM.nextBytes(salt);
                // generate secret key
                secret = generateSecretKey(salt);
                // write salt
                data.writeInt(salt.length);
                data.write(salt);
            } else {
                data.writeByte(USE_KEYSTORE);
                // find secret key from keystore
                secret = getSecretKey();
            }

            // cipher
            Cipher cipher = getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            // write IV
            byte[] iv = cipher.getIV();
            data.writeInt(iv.length);
            data.write(iv);

            // digest and write the encrypted data
            CipherAndDigestOutputStream cipherOut = new CipherAndDigestOutputStream(out, cipher, messageDigest);
            IOUtils.copy(in, cipherOut);
            cipherOut.close();
            byte[] digest = cipherOut.getDigest();
            return toHexString(digest);
        } catch (GeneralSecurityException e) {
            throw new NuxeoException(e);
        }

    }

    /**
     * Decrypts the given input stream into the given output stream.
     */
    protected void decrypt(InputStream in, OutputStream out) throws IOException {
        byte[] magic = new byte[FILE_MAGIC.length];
        IOUtils.read(in, magic);
        if (!Arrays.equals(magic, FILE_MAGIC)) {
            throw new IOException("Invalid file (bad magic)");
        }
        DataInputStream data = new DataInputStream(in);
        byte magicvers = data.readByte();
        if (magicvers != FILE_VERSION_1) {
            throw new IOException("Invalid file (bad version)");
        }

        byte usepb = data.readByte();
        if (usepb == USE_PBKDF2) {
            if (!usePBKDF2) {
                throw new NuxeoException("File requires PBKDF2 password");
            }
        } else if (usepb == USE_KEYSTORE) {
            if (usePBKDF2) {
                throw new NuxeoException("File requires keystore");
            }
        } else {
            throw new IOException("Invalid file (bad use)");
        }

        try {
            // secret key
            Key secret;
            if (usePBKDF2) {
                // read salt first
                int saltLen = data.readInt();
                if (saltLen <= 0 || saltLen > MAX_SALT_LEN) {
                    throw new NuxeoException("Invalid salt length: " + saltLen);
                }
                byte[] salt = new byte[saltLen];
                data.read(salt, 0, saltLen);
                secret = generateSecretKey(salt);
            } else {
                secret = getSecretKey();
            }

            // read IV
            int ivLen = data.readInt();
            if (ivLen <= 0 || ivLen > MAX_IV_LEN) {
                throw new NuxeoException("Invalid IV length: " + ivLen);
            }
            byte[] iv = new byte[ivLen];
            data.read(iv, 0, ivLen);

            // cipher
            Cipher cipher = getCipher();
            cipher.init(Cipher.DECRYPT_MODE, secret, getParameterSpec(iv));

            // read the encrypted data
            try (InputStream cipherIn = new CipherInputStream(in, cipher)) {
                IOUtils.copy(cipherIn, out);
            } catch (IOException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof BadPaddingException) {
                    throw new NuxeoException(cause.getMessage(), e);
                }
            }
        } catch (GeneralSecurityException e) {
            throw new NuxeoException(e);
        }
    }

    protected Cipher getCipher() throws GeneralSecurityException {
        if (useInsecureCipher) {
            return Cipher.getInstance(AES_CBC_PKCS5_PADDING); // NOSONAR
        } else {
            return Cipher.getInstance(AES_GCM_NOPADDING);
        }
    }

    protected AlgorithmParameterSpec getParameterSpec(byte[] iv) {
        if (useInsecureCipher) {
            return new IvParameterSpec(iv);
        } else {
            return new GCMParameterSpec(128, iv);
        }
    }

    /**
     * A {@link javax.crypto.CipherOutputStream CipherOutputStream} that also does a digest of the original stream at
     * the same time.
     */
    public static class CipherAndDigestOutputStream extends FilterOutputStream {

        protected Cipher cipher;

        protected OutputStream out;

        protected MessageDigest messageDigest;

        protected byte[] digest;

        public CipherAndDigestOutputStream(OutputStream out, Cipher cipher, MessageDigest messageDigest) {
            super(out);
            this.out = out;
            this.cipher = cipher;
            this.messageDigest = messageDigest;
        }

        public byte[] getDigest() {
            return digest;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            messageDigest.update(b, off, len);
            byte[] bytes = cipher.update(b, off, len);
            if (bytes != null) {
                out.write(bytes);
                bytes = null; // help GC
            }
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            digest = messageDigest.digest();
            try {
                byte[] bytes = cipher.doFinal();
                out.write(bytes);
                bytes = null; // help GC
            } catch (GeneralSecurityException e) {
                throw new NuxeoException(e);
            }
            try {
                flush();
            } finally {
                out.close();
            }
        }
    }

}
