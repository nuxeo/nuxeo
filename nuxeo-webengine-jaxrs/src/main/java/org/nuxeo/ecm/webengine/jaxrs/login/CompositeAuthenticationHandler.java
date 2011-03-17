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
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.Utils;

/**
 * An authentication handlers that delegate the authentication to the first registered handler
 * that knows how to authenticate.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeAuthenticationHandler implements AuthenticationHandler {

    protected AuthenticationHandler[] handlers;

    public CompositeAuthenticationHandler(String classRefs) throws Exception {
        handlers = Utils.newInstances(AuthenticationHandler.class, classRefs);
    }

    public CompositeAuthenticationHandler(AuthenticationHandler[] handlers) {
        this.handlers = handlers;
    }

    @Override
    public void init(Map<String, String> properties) {
        // do nothing
    }

    @Override
    public LoginContext handleAuthentication(HttpServletRequest request, HttpServletResponse response) throws LoginException {
        for (AuthenticationHandler handler : handlers) {
            LoginContext lc = handler.handleAuthentication(request, response);
            if (lc != null) {
                return lc;
            }
        }
        return null;
    }

}
