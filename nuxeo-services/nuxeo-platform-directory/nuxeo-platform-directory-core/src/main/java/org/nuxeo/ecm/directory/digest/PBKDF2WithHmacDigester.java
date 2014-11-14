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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 *
 * @since 7.1
 */
public class PBKDF2WithHmacDigester extends SaltedDigester {

    private int numberOfIterations;

    private int keyLength;

    @Override
    protected byte[] generateDigest(String password, byte[] salt) {
        char[] passwordChars = password.toCharArray();

        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt,
                numberOfIterations, keyLength);
        try {
            SecretKeyFactory key = SecretKeyFactory.getInstance(algorithm);
            return key.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Unable to compute hash for password", e);
        }
    }

    @Override
    public void setParams(Map<String, String> params) {
        super.setParams(params);

        for (Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
            case "numberOfIterations":
                numberOfIterations = Integer.parseInt(entry.getValue());
                break;
            case "keyLength":
                keyLength = Integer.parseInt(entry.getValue());
                break;
            }
        }
    }
}
