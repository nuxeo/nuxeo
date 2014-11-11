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

/**
 *
 * Initiate an authentication for the given HTTP request.
 *
 * Implementations are responsible to detect whether the request contains
 * any known authentication data and perform the authentication if needed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface AuthenticationHandler {

    /**
     * Initialize this handler given a property map.
     *
     * @param properties
     */
    void init(Map<String,String> properties);

    /**
     * Handle the authentication if the request contains any known authentication data.
     * If authentication was done returns the resulting LoginContext otherwise returns null.
     * If authentication failed throws {@link LoginException} and the implementation <b>must</b>
     * finish the request by correctly responding to the client or redirecting
     * to another page - through the given response object.
     *
     * @param request the http request
     * @param response the http response
     * @return the loginc context if successful, or null if login was not handled.
     * @throws LoginException if authentication failed.
     */
    LoginContext handleAuthentication(HttpServletRequest request, HttpServletResponse response) throws LoginException;

}
