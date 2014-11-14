/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.directory.digest;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 7.1
 */
public abstract class AbstractSaltedDigester implements PasswordDigester {

    protected static final Random random = new SecureRandom();

    private int saltLength;

    private String name;

    @Override
    public String hashPassword(String password) {
        byte[] salt = generateSalt(getSaltLength());
        return hasPasswordWithSalt(password, salt);

    }

    public String hasPasswordWithSalt(String password, byte[] salt) {
        byte[] hash = generateDigest(password, salt);
        return encodeSaltAndHash(salt, hash);
    }

    @Override
    public void setParams(Map<String, String> params) {
        for (Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
            case "saltLength":
                saltLength = Integer.parseInt(entry.getValue());
                break;
            }
        }
    }

    /**
     * Override this method to implement the digest algorithm.
     *
     * @param password
     * @param salt
     * @return
     *
     * @since 7.1
     */
    abstract protected byte[] generateDigest(String password, byte[] salt);

    /**
     *
     * @since 7.1
     */
    protected Integer getSaltLength() {
        return saltLength;
    }

    /**
     * Securely generate a random salt of a given length.
     *
     * @param method
     * @return
     *
     * @since 7.1
     */
    private static byte[] generateSalt(Integer saltLength) {
        byte[] salt = new byte[saltLength];
        synchronized (random) {
            random.nextBytes(salt);
        }
        return salt;
    }

    /**
     * Return a base64 encoded representation of the hashed password. Something
     * like {SSHA}sdfqlagmv23fefazef==
     *
     * @param method
     * @param salt
     * @param hash
     * @return
     *
     * @since 7.1
     */
    private String encodeSaltAndHash(byte[] salt, byte[] hash) {
        byte[] bytes = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, bytes, 0, hash.length);
        System.arraycopy(salt, 0, bytes, hash.length, salt.length);
        return String.format("{%s}%s", getName(), Base64.encodeBytes(bytes));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;

    }

    /**
     * Verify a password against a hashed password.
     *
     * @param password the password to verify
     * @param hashedPassword the hashed password
     * @return {@code true} if the password matches
     */
    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        // Extract method from hashed password
        PasswordDigesterService service = Framework.getService(PasswordDigesterService.class);
        String method = service.getDigesterNameFromHash(hashedPassword);
        if (!getName().equals(method)) {
            return false;
        }

        // Extract digest part
        String digest = hashedPassword.substring(String.format("{%s}", method).length());

        byte[] bytes = Base64.decode(digest);
        if (bytes == null) {
            // invalid base64
            return false;
        }
        int lengthOfHashInBytes = bytes.length - saltLength;

        //needs hash + at least two bytes of salt
        if (lengthOfHashInBytes <= 2) {
            return false;
        }

        // Get Salt from digest
        byte[] salt = new byte[bytes.length - lengthOfHashInBytes];
        System.arraycopy(bytes, lengthOfHashInBytes, salt, 0, salt.length);

        // Get hash from digest
        byte[] hash = new byte[lengthOfHashInBytes];
        System.arraycopy(bytes, 0, hash, 0, hash.length);

        // Compare hash with a generated one from password and salt
        return MessageDigest.isEqual(hash, generateDigest(password, salt));
    }

}
