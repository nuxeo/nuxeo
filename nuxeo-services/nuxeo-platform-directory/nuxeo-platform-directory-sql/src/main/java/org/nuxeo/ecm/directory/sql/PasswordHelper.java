/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.nuxeo.common.utils.Base64;

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
     * @param password
     * @return {@code true} if the password is hashed
     */
    public static boolean isHashed(String password) {
        return password.startsWith(HSSHA)
                || password.startsWith(HSMD5);
    }

    /**
     * Returns the hashed string for a password according to a given hashing
     * algorithm.
     *
     * @param algorithm the algorithm, {@link #SSHA} or {@link #SMD5}, or
     *            {@code null} to not hash
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
        return prefix + Base64.encodeBytes(bytes);
    }

    /**
     * Verify a password against a hashed password.
     *
     * @param password the password to verify
     * @param hashedPassword the hashed password
     * @return {@code true} if the password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
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

        byte[] bytes = Base64.decode(digest);
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
        return MessageDigest.isEqual(hash, digestWithSalt(password, salt,
                digestalg));
    }

    public static byte[] digestWithSalt(String password, byte[] salt,
            String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
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
