/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

/**
 * Helper to check passwords and generated hashed salted ones.
 */
public class PasswordHelper {

    public static final String SSHA = "SSHA";

    public static final String SMD5 = "SMD5";

    private static final String HSSHA = "{SSHA}";

    private static final String HSMD5 = "{SMD5}";

    private static final String SHA1 = "SHA-1";

    private static final String MD5 = "MD5";

    private static final int SALT_LEN = 8;

    private static final Random random = new SecureRandom();

    // utility class
    private PasswordHelper() {
    }

    /**
     * Checks if a password is already hashed.
     *
     * @return {@code true} if the password is hashed
     */
    public static boolean isHashed(String password) {
        return password.startsWith(HSSHA) || password.startsWith(HSMD5);
    }

    /**
     * Returns the hashed string for a password according to a given hashing algorithm.
     *
     * @param algorithm the algorithm, {@link #SSHA} or {@link #SMD5}, or {@code null} to not hash
     * @param password the password
     * @return the hashed password
     */
    public static String hashPassword(String password, String algorithm) {
        if (algorithm == null || "".equals(algorithm)) {
            return password;
        }
        String digestalg;
        String prefix;
        if (SSHA.equals(algorithm)) {
            digestalg = SHA1;
            prefix = HSSHA;
        } else if (SMD5.equals(algorithm)) {
            digestalg = MD5;
            prefix = HSMD5;
        } else {
            throw new RuntimeException("Unknown algorithm: " + algorithm);
        }

        byte[] salt = new byte[SALT_LEN];
        synchronized (random) {
            random.nextBytes(salt);
        }
        byte[] hash = digestWithSalt(password, salt, digestalg);
        byte[] bytes = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, bytes, 0, hash.length);
        System.arraycopy(salt, 0, bytes, hash.length, salt.length);
        return prefix + Base64.encodeBase64String(bytes);
    }

    /**
     * Verify a password against a hashed password.
     * <p>
     * If the hashed password is {@code null} then the verification always fails.
     *
     * @param password the password to verify
     * @param hashedPassword the hashed password
     * @return {@code true} if the password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        String digestalg;
        int len;
        if (hashedPassword.startsWith(HSSHA)) {
            digestalg = SHA1;
            len = 20;
        } else if (hashedPassword.startsWith(HSMD5)) {
            digestalg = MD5;
            len = 16;
        } else {
            return hashedPassword.equals(password);
        }
        String digest = hashedPassword.substring(6);

        byte[] bytes;
        try {
            bytes = Base64.decodeBase64(digest);
        } catch (IllegalArgumentException e) {
            bytes = null;
        }
        if (bytes == null) {
            // invalid base64
            return false;
        }
        if (bytes.length < len + 2) {
            // needs hash + at least two bytes of salt
            return false;
        }
        byte[] hash = new byte[len];
        byte[] salt = new byte[bytes.length - len];
        System.arraycopy(bytes, 0, hash, 0, hash.length);
        System.arraycopy(bytes, hash.length, salt, 0, salt.length);
        return MessageDigest.isEqual(hash, digestWithSalt(password, salt, digestalg));
    }

    public static byte[] digestWithSalt(String password, byte[] salt, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            if (password == null) {
                password = "";
            }
            md.update(password.getBytes("UTF-8"));
            md.update(salt);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm, e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
