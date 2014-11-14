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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 *
 * @since 7.1
 */
public class SaltedDigester extends AbstractSaltedDigester  {

    protected String algorithm;

    protected static final Log log = LogFactory.getLog(SaltedDigester.class);

    @Override
    protected byte[] generateDigest(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(password.getBytes("UTF-8"));
            md.update(salt);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnknownAlgorithmException();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void setParams(Map<String, String> params) {
        super.setParams(params);
        for (Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
            case "algorithm":
                algorithm = entry.getValue();
                break;
            }
        }
    }



}
