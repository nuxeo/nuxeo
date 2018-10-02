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
 *     slacoin
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.spi.RequestInterceptor;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author matic
 */
public class PortalSSOAuthInterceptor extends RequestInterceptor {

    protected static final Random RANDOM = new SecureRandom();

    protected final String secret;

    protected final String username;

    public PortalSSOAuthInterceptor(String secretKey, String userName) {
        this.secret = secretKey;
        this.username = userName;
    }

    @Override
    public void processRequest(Request request, Connector connector) {
        request.putAll(computeHeaders());
    }

    protected Map<String, String> computeHeaders() {
        // compute token
        long ts = new Date().getTime();
        long random = RANDOM.nextInt();

        String clearToken = String.format("%d:%d:%s:%s", ts, random, secret, username);

        byte[] hashedToken;

        try {
            hashedToken = MessageDigest.getInstance("MD5").digest(clearToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot compute token", e);
        }

        String base64HashedToken = Base64.encodeBase64String(hashedToken);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("NX_TS", String.valueOf(ts));
        headers.put("NX_RD", String.valueOf(random));
        headers.put("NX_TOKEN", base64HashedToken);
        headers.put("NX_USER", username);
        return headers;
    }

    @Override
    public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
        Map<String, String> computedHeaders = computeHeaders();
        MultivaluedMap<String, Object> headers = cr.getHeaders();
        for (Map.Entry<String, String> entry : computedHeaders.entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return getNext().handle(cr);
    }
}
