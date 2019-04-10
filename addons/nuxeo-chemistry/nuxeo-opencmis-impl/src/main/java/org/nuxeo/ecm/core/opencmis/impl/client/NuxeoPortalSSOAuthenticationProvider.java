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
 *     Stephane Lacoin (aka matic)
 */

package org.nuxeo.ecm.core.opencmis.impl.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.chemistry.opencmis.client.bindings.spi.AbstractAuthenticationProvider;
import org.nuxeo.common.utils.Base64;

public class NuxeoPortalSSOAuthenticationProvider extends AbstractAuthenticationProvider   {

    private static final long serialVersionUID = 1L;

    public final static String SECRET_KEY = "NUXEO_PORTAL_SSO_SECRET";

    protected String getSecretKey() {
        return (String)getSession().get(SECRET_KEY);
    }

    @Override
    public Map<String, List<String>> getHTTPHeaders(String url) {

        long ts = new Date().getTime();
        long random = new Random(ts).nextInt();

        String secret = getSecretKey();

        String username = getUser();

        String clearToken = String.format("%d:%d:%s:%s", ts, random, secret, username);

        byte[] hashedToken;

        try {
            hashedToken = MessageDigest.getInstance("MD5").digest(clearToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Cannot compute token", e);
        }

        String base64HashedToken = Base64.encodeBytes(hashedToken);

        // set request headers

        Map<String,List<String>> headers = new HashMap<String,List<String>>();

        headers.put("NX_USER", Collections.singletonList(username));
        headers.put("NX_TOKEN", Collections.singletonList(base64HashedToken));
        headers.put("NX_RD", Collections.singletonList(String.valueOf(random)));
        headers.put("NX_TS", Collections.singletonList(String.valueOf(ts)));

        return Collections.unmodifiableMap(headers);

    }

}
