/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     jcarsique
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Supported algorithms (name, keysize):
 * <ul>
 * <li>AES/ECB/PKCS5Padding (128)</li>
 * <li>DES/ECB/PKCS5Padding (64)</li>
 * </ul>
 *
 * @since 7.4
 */
public class Crypto {

    protected static final Pattern CRYPTO_PATTERN = Pattern.compile("\\{\\$(?<algo>.*)\\$(?<value>.+)\\}");

    private static final Log log = LogFactory.getLog(Crypto.class);

    public static final String AES = "AES";

    public static final String AES_ECB_PKCS5PADDING = "AES/ECB/PKCS5Padding";

    public static final String DES = "DES";

    public static final String DES_ECB_PKCS5PADDING = "DES/ECB/PKCS5Padding";

    public static final String[] IMPLEMENTED_ALGOS = { AES, DES, AES_ECB_PKCS5PADDING, DES_ECB_PKCS5PADDING };

    public static final String DEFAULT_ALGO = AES_ECB_PKCS5PADDING;

    private static final String SHA1 = "SHA-1";

    private final byte[] secretKey;

    private final Map<String, SecretKey> secretKeys = new HashMap<>();

    private boolean initialized = true;

    private final byte[] digest;

    public Crypto(byte[] secretKey) {
        this.secretKey = secretKey;
        digest = getSHA1DigestOrEmpty(secretKey);
        if (digest.length == 0) {
            clear();
        }
    }

    /**
     * Initialize cryptography with a map of {@link SecretKey}.
     *
     * @param secretKeys Map of {@code SecretKey} per algorithm
     */
    public Crypto(Map<String, SecretKey> secretKeys) {
        this(secretKeys, Crypto.class.getName().toCharArray());
    }

    /**
     * Initialize cryptography with a map of {@link SecretKey}.
     *
     * @param digest Digest for later use by {@link #verifyKey(byte[])}
     * @param secretKeys Map of {@code SecretKey} per algorithm
     */
    public Crypto(Map<String, SecretKey> secretKeys, char[] digest) {
        secretKey = new byte[0];
        this.digest = getSHA1DigestOrEmpty(getBytes(digest));
        this.secretKeys.putAll(secretKeys);
        if (this.digest.length == 0) {
            clear();
        }
    }

    /**
     * Initialize cryptography with a keystore.
     *
     * @param keystorePath Path to the keystore.
     * @param keystorePass Keystore password. It is also used to generate the digest for {@link #verifyKey(byte[])}
     * @param keyAlias Key alias prefix. It is suffixed with the algorithm.
     * @param keyPass Key password
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Crypto(String keystorePath, char[] keystorePass, String keyAlias, char[] keyPass)
            throws GeneralSecurityException, IOException {
        this(Crypto.getKeysFromKeyStore(keystorePath, keystorePass, keyAlias, keyPass), keystorePass);
    }

    public static final class NoOp extends Crypto {

        public static final Crypto NO_OP = new NoOp();

        private NoOp() {
            super(new byte[0]);
        }

        @Override
        public String encrypt(String algorithm, byte[] bytesToEncrypt) throws GeneralSecurityException {
            return null;
        }

        @Override
        public byte[] decrypt(String strToDecrypt) {
            return strToDecrypt.getBytes();
        }

        @Override
        public void clear() {
            // NO OP
        }
    }

    protected SecretKey getSecretKey(String algorithm, byte[] key) throws NoSuchAlgorithmException {
        if (!initialized) {
            throw new RuntimeException("The Crypto object has been cleared.");
        }
        if (AES_ECB_PKCS5PADDING.equals(algorithm)) {
            algorithm = AES; // AES_ECB_PKCS5PADDING is the default for AES
        } else if (DES_ECB_PKCS5PADDING.equals(algorithm)) {
            algorithm = DES; // DES_ECB_PKCS5PADDING is the default for DES
        }
        if (!secretKeys.containsKey(algorithm)) {
            if (secretKey.length == 0) {
                throw new NoSuchAlgorithmException("Unsupported algorithm: " + algorithm);
            }
            if (AES.equals(algorithm)) { // default for AES
                key = Arrays.copyOf(getSHA1Digest(key), 16); // use a 128 bits secret key
                secretKeys.put(AES, new SecretKeySpec(key, AES));
            } else if (DES.equals(algorithm)) { // default for DES
                key = Arrays.copyOf(getSHA1Digest(key), 8); // use a 64 bits secret key
                secretKeys.put(DES, new SecretKeySpec(key, DES));
            } else {
                throw new NoSuchAlgorithmException("Unsupported algorithm: " + algorithm);
            }
        }
        return secretKeys.get(algorithm);
    }

    public byte[] getSHA1Digest(final byte[] key) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(SHA1);
        return sha.digest(key);
    }

    public byte[] getSHA1DigestOrEmpty(final byte[] bytes) {
        byte[] aDigest = new byte[0];
        try {
            aDigest = getSHA1Digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        return aDigest;
    }

    public String encrypt(byte[] bytesToEncrypt) throws GeneralSecurityException {
        return encrypt(null, bytesToEncrypt);
    }

    /**
     * @param algorithm cipher transformation of the form "algorithm/mode/padding" or "algorithm". See the Cipher
     *            section in the <a
     *            href=http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Cipher>Java
     *            Cryptography Architecture Standard Algorithm Name Documentation</a>.
     * @throws NoSuchPaddingException if {@code algorithm} contains a padding scheme that is not available.
     * @throws NoSuchAlgorithmException if {@code algorithm} is in an invalid or not supported format.
     * @throws GeneralSecurityException
     */
    public String encrypt(String algorithm, byte[] bytesToEncrypt) throws GeneralSecurityException {
        final String encryptedAlgo;
        if (StringUtils.isBlank(algorithm)) {
            algorithm = DEFAULT_ALGO;
            encryptedAlgo = "";
        } else {
            encryptedAlgo = Base64.encodeBase64String(algorithm.getBytes());
        }
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(algorithm, secretKey));
        final String encryptedString = Base64.encodeBase64String(cipher.doFinal(bytesToEncrypt));
        return String.format("{$%s$%s}", encryptedAlgo, encryptedString);
    }

    /**
     * The method returns either the decrypted {@code strToDecrypt}, either the {@code strToDecrypt} itself if it is not
     * recognized as a crypted string or if the decryption fails. The return value is a byte array for security purpose,
     * it is your responsibility to convert it then to a String or not (use of {@code char[]} is recommended).
     *
     * @return the decrypted {@code strToDecrypt} as an array of bytes, never {@code null}
     * @see #getChars(byte[])
     */
    public byte[] decrypt(String strToDecrypt) {
        Matcher matcher = CRYPTO_PATTERN.matcher(strToDecrypt);
        if (!matcher.matches()) {
            return strToDecrypt.getBytes();
        }
        Cipher decipher;
        try {
            String algorithm = new String(Base64.decodeBase64(matcher.group("algo")));
            if (StringUtils.isBlank(algorithm)) {
                algorithm = DEFAULT_ALGO;
            }
            decipher = Cipher.getInstance(algorithm);
            decipher.init(Cipher.DECRYPT_MODE, getSecretKey(algorithm, secretKey));
            return decipher.doFinal(Base64.decodeBase64(matcher.group("value")));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            log.trace("Available algorithms: " + Security.getAlgorithms("Cipher"));
            log.trace("Available security providers: " + Arrays.asList(Security.getProviders()));
            log.debug(e, e);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IllegalArgumentException e) {
            log.debug(e, e);
        }
        return strToDecrypt.getBytes();
    }

    /**
     * Clear sensible values. That makes the current object unusable.
     */
    public void clear() {
        Arrays.fill(secretKey, (byte) 0);
        Arrays.fill(digest, (byte) 0);
        secretKeys.clear();
        initialized = false;
    }

    /**
     * Test the given {@code candidateDigest} against the configured digest. In case of failure, the secret data is
     * destroyed and the object is made unusable.<br>
     * Use that method to check if some code is allowed to request that Crypto object.
     *
     * @return true if {@code candidateDigest} matches the one used on creation.
     * @see #clear()
     * @see #verifyKey(char[])
     */
    public boolean verifyKey(byte[] candidateDigest) {
        boolean success = Arrays.equals(getSHA1DigestOrEmpty(candidateDigest), digest);
        if (!success) {
            clear();
        }
        return success;
    }

    /**
     * Test the given {@code candidateDigest} against the configured digest. In case of failure, the secret data is
     * destroyed and the object is made unusable.<br>
     * Use that method to check if some code is allowed to request that Crypto object.
     *
     * @return true if {@code candidateDigest} matches the one used on creation.
     * @see #clear()
     * @see #verifyKey(byte[])
     */
    public boolean verifyKey(char[] candidateDigest) {
        return verifyKey(getBytes(candidateDigest));
    }

    /**
     * Utility method to get {@code byte[]} from {@code char[]} since it is recommended to store passwords in
     * {@code char[]} rather than in {@code String}.<br>
     * The default charset of this Java virtual machine is used. There can be conversion issue with unmappable
     * characters: they will be replaced with the charset's default replacement string.
     *
     * @param chars char array to convert
     * @return the byte array converted from {@code chars} using the default charset.
     */
    public static byte[] getBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.defaultCharset().encode(charBuffer);
        return Arrays.copyOfRange(byteBuffer.array(), 0, byteBuffer.limit());
    }

    /**
     * Utility method to get {@code char[]} from {@code bytes[]} since it is recommended to store passwords in
     * {@code char[]} rather than in {@code String}.<br>
     * The default charset of this Java virtual machine is used. There can be conversion issue with unmappable
     * characters: they will be replaced with the charset's default replacement string.
     *
     * @param bytes byte array to convert
     * @return the char array converted from {@code bytes} using the default charset.
     */
    public static char[] getChars(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuffer = Charset.defaultCharset().decode(byteBuffer);
        return Arrays.copyOfRange(charBuffer.array(), 0, charBuffer.limit());
    }

    /**
     * @return true if the given {@code value} is encrypted
     */
    public static boolean isEncrypted(String value) {
        return value != null && CRYPTO_PATTERN.matcher(value).matches();
    }

    /**
     * Extract secret keys from a keystore looking for {@code keyAlias + algorithm}
     *
     * @param keystorePath Path to the keystore
     * @param keystorePass Keystore password
     * @param keyAlias Key alias prefix. It is suffixed with the algorithm.
     * @param keyPass Key password
     * @throws GeneralSecurityException
     * @throws IOException
     * @see #IMPLEMENTED_ALGOS
     */
    public static Map<String, SecretKey> getKeysFromKeyStore(String keystorePath, char[] keystorePass, String keyAlias,
            char[] keyPass) throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance("JCEKS");
        try (InputStream keystoreStream = new FileInputStream(keystorePath)) {
            keystore.load(keystoreStream, keystorePass);
        }
        Map<String, SecretKey> secretKeys = new HashMap<>();
        for (String algo : IMPLEMENTED_ALGOS) {
            if (keystore.containsAlias(keyAlias + algo)) {
                SecretKey key = (SecretKey) keystore.getKey(keyAlias + algo, keyPass);
                secretKeys.put(algo, key);
            }
        }
        if (secretKeys.isEmpty()) {
            throw new KeyStoreException(String.format("No alias \"%s<algo>\" found in %s", keyAlias, keystorePath));
        }
        return secretKeys;
    }

    /**
     * Store a key in a keystore.<br>
     * The keystore is created if it doesn't exist.
     *
     * @param keystorePath Path to the keystore
     * @param keystorePass Keystore password
     * @param keyAlias Key alias prefix. It must be suffixed with the algorithm ({@link SecretKey#getAlgorithm()} is
     *            fine).
     * @param keyPass Key password
     * @throws GeneralSecurityException
     * @throws IOException
     * @see #IMPLEMENTED_ALGOS
     */
    public static void setKeyInKeyStore(String keystorePath, char[] keystorePass, String keyAlias, char[] keyPass,
            SecretKey key) throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance("JCEKS");
        if (!new File(keystorePath).exists()) {
            log.info("Creating a new JCEKS keystore at " + keystorePath);
            keystore.load(null);
        } else {
            try (InputStream keystoreStream = new FileInputStream(keystorePath)) {
                keystore.load(keystoreStream, keystorePass);
            }
        }
        KeyStore.SecretKeyEntry keyStoreEntry = new KeyStore.SecretKeyEntry(key);
        PasswordProtection keyPassword = new PasswordProtection(keyPass);
        keystore.setEntry(keyAlias, keyStoreEntry, keyPassword);
        try (OutputStream keystoreStream = new FileOutputStream(keystorePath)) {
            keystore.store(keystoreStream, keystorePass);
        }
    }

}
