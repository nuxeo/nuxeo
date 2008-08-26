/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.jersey.patch;

import java.io.InputStream;
import java.net.URI;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.InBoundHeaders;
import com.sun.jersey.spi.container.WebApplication;


/**
 * Copied from {@link com.sun.jersey.impl.container.servlet.ServletContainerRequest}
 * to expose HttpServletRequest and/or the HttpSession and webengine ations
 *
 * Adapts a HttpServletRequest to provide the methods of HttpRequest
 */
public class ServletContainerRequest extends ContainerRequest {

    private final HttpServletRequest request;
    protected String action;

    public ServletContainerRequest(
            HttpServletRequest request,
            WebApplication wa,
            String method,
            URI baseUri,
            URI requestUri,
            InBoundHeaders headers,
            InputStream entity, String action) {
        super(wa, method, baseUri, requestUri, headers, entity);
        this.request = request;
        this.action = action;
    }

    /**
     * @return the action.
     */
    public String getAction() {
        return action;
    }

    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    public HttpSession getSession() {
        return request.getSession();
    }

    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }

    // SecurityContext

    @Override
    public Principal getUserPrincipal() {
        return request.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return request.getAuthType();
    }
}

