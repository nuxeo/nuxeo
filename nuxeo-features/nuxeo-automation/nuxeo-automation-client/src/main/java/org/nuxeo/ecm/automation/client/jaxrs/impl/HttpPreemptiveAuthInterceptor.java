/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

public class HttpPreemptiveAuthInterceptor implements HttpRequestInterceptor {

    protected static final Log log = LogFactory.getLog(HttpPreemptiveAuthInterceptor.class);

    protected final AuthScheme authScheme;

    HttpPreemptiveAuthInterceptor(AuthScheme authScheme) {
        this.authScheme = authScheme;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
        // If not auth scheme has been initialized yet
        if (authState.getAuthScheme() != null) {
            return;
        }

        // fetch credentials
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
        HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());

        // Obtain credentials matching the target host
        Credentials creds = credsProvider.getCredentials(authScope);

        if (creds == null) {
            log.warn("no credentials provided for " + authScope);
            return;
        }

        authState.setAuthScheme(authScheme);
        authState.setCredentials(creds);
    }

}
