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
package org.nuxeo.ecm.automation.client.jaxrs.spi.auth;

import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.spi.RequestInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

/**
 * Inject the basic authentication header in the request.
 *
 * @author matic
 */
public class BasicAuthInterceptor implements RequestInterceptor {

    protected String token;

    public BasicAuthInterceptor(String username, String password) {
        setAuth(username, password);
    }

    public void setAuth(String username, String password) {
        String info = username + ":" + password;
        token = "Basic " + Base64.encode(info);
    }

    @Override
    public void processRequest(Request request, Connector connector) {
        request.put("Authorization", token);
    }

}
