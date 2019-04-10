/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (aka matic)
 */

package org.nuxeo.ecm.core.opencmis.impl.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.chemistry.opencmis.client.bindings.spi.AbstractAuthenticationProvider;
import org.apache.chemistry.opencmis.commons.impl.Base64;

public class NuxeoPortalSSOAuthenticationProvider extends AbstractAuthenticationProvider {

    private static final long serialVersionUID = 1L;

    protected static final Random RANDOM = new SecureRandom();

    public final static String SECRET_KEY = "NUXEO_PORTAL_SSO_SECRET";

    protected String getSecretKey() {
        return (String) getSession().get(SECRET_KEY);
    }

    @Override
    public Map<String, List<String>> getHTTPHeaders(String url) {

        long ts = new Date().getTime();
        long random = RANDOM.nextInt();

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

        Map<String, List<String>> headers = new HashMap<String, List<String>>();

        headers.put("NX_USER", Collections.singletonList(username));
        headers.put("NX_TOKEN", Collections.singletonList(base64HashedToken));
        headers.put("NX_RD", Collections.singletonList(String.valueOf(random)));
        headers.put("NX_TS", Collections.singletonList(String.valueOf(ts)));

        return Collections.unmodifiableMap(headers);

    }

}
