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
 *     Damien Metzler
 */
package org.nuxeo.ecm.directory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.nuxeo.ecm.directory.digest.PasswordDigester;
import org.nuxeo.ecm.directory.digest.PasswordDigesterService;
import org.nuxeo.ecm.directory.digest.UnknownAlgorithmException;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to check passwords and generated hashed salted ones.
 */
public class PasswordHelper {

    @Deprecated
    public static final String SSHA = "SSHA";

    @Deprecated
    public static final String SMD5 = "SMD5";

    /**
     * Checks if a password is already hashed.
     *
     * @param password
     * @return {@code true} if the password is hashed
     */
    public static boolean isHashed(String password) {
        String name = getDigesterService().getDigesterNameFromHash(password);
        if(name == null) {
            return false;
        }
        try {
            getDigesterService().getPasswordDigester(name);
            return true;
        } catch (UnknownAlgorithmException e) {
            return false;
        }
    }

    private static PasswordDigesterService getDigesterService() {
        return Framework.getService(PasswordDigesterService.class);
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

        PasswordDigester digester = getDigesterService().getPasswordDigester(
                algorithm);
        return digester.hashPassword(password);

    }


    /**
     * Verify a password against a hashed password.
     *
     * @param password the password to verify
     * @param hashedPassword the hashed password
     * @return {@code true} if the password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        // Extract method from hashed password
        PasswordDigesterService ds = getDigesterService();
        String digesterName = ds.getDigesterNameFromHash(hashedPassword);
        if (digesterName == null) {
            return password.equals(hashedPassword);
        }

        return ds.getPasswordDigester(digesterName).verifyPassword(password, hashedPassword);
    }


    @Deprecated
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
