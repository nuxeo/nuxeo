/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.MultivaluedMap;

import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.spi.RequestInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;

/**
 *
 * @author matic
 *
 */
public class PortalSSOAuthInterceptor extends RequestInterceptor {

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
        long random = new Random(ts).nextInt();

        String clearToken = String.format("%d:%d:%s:%s", ts, random, secret,
                username);

        byte[] hashedToken;

        try {
            hashedToken = MessageDigest.getInstance("MD5").digest(
                    clearToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Cannot compute token", e);
        }

        String base64HashedToken = Base64.encode(hashedToken);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("NX_TS", String.valueOf(ts));
        headers.put("NX_RD", String.valueOf(random));
        headers.put("NX_TOKEN", base64HashedToken);
        headers.put("NX_USER", username);
        return headers;
    }

    @Override
    public ClientResponse handle(ClientRequest cr)
            throws ClientHandlerException {
        Map<String, String> computedHeaders = computeHeaders();
        MultivaluedMap<String, Object> headers = cr.getHeaders();
        for (Map.Entry<String, String> entry : computedHeaders.entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return getNext().handle(cr);
    }
}
