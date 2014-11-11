/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
