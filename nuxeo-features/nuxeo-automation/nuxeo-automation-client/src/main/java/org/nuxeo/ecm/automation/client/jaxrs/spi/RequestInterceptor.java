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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.BasicAuthInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.PortalSSOAuthInterceptor;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Provide a way of intercepting requests before they are sent server side.
 * Authentication headers are injected this way.
 *
 * @see BasicAuthInterceptor
 * @see PortalSSOAuthInterceptor
 *
 */
public abstract class RequestInterceptor extends ClientFilter {

    public abstract void processRequest(Request request, Connector connector);

    @Override
    public ClientResponse handle(ClientRequest cr)
            throws ClientHandlerException {
        return getNext().handle(cr);
    }
}
