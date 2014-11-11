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
