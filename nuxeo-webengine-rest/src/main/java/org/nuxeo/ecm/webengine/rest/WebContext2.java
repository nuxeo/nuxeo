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

package org.nuxeo.ecm.webengine.rest;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.rest.domains.WebDomain;
import org.nuxeo.ecm.webengine.session.UserSession;

import com.sun.jersey.api.core.HttpContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebContext2 {

    protected WebDomain<?> domain;
    protected HttpServletRequest req;
    protected HttpServletResponse resp;
    protected UserSession us;
    protected HttpContext ctx;

    public WebContext2(WebDomain<?> domain, HttpContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        this.domain = domain;
        this.ctx = ctx;
        this.req = req;
        this.resp = resp;
        this.us = UserSession.getCurrentSession(req.getSession(true));
    }

    public WebEngine2 getEngine() {
        return domain.engine;
    }

    /**
     * @return the domain.
     */
    public WebDomain<?> getDomain() {
        return domain;
    }

    public UserSession getUserSession() {
        return us;
    }

    public CoreSession getCoreSession() {
        return us.getCoreSession();
    }

    public Principal getPrincipal() {
        return us.getPrincipal();
    }

    /**
     * @return the req.
     */
    public HttpServletRequest getRequest() {
        return req;
    }

    /**
     * @return the resp.
     */
    public HttpServletResponse getResponse() {
        return resp;
    }

    public HttpContext getHttpContext() {
        return ctx;
    }

}
