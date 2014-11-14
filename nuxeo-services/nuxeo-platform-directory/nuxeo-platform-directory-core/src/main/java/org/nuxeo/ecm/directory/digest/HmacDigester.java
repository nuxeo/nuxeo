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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 *
 * @since 7.1
 */
public class HmacDigester extends SaltedDigester {

    @Override
    protected byte[] generateDigest(String password, byte[] salt) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(salt, algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            return mac.doFinal(password.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new UnknownAlgorithmException(e);
        } catch (InvalidKeyException | IllegalStateException
                | UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to compute hash for password", e);
        }
    }
}
