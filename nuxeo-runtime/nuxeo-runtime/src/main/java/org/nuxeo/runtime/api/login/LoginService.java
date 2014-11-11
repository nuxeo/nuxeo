/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import java.security.Principal;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface LoginService {

    /**
     * System login, using a private principal that has all privileges. This
     * principal is not stored in any database and cannot be accessed by user.
     * <p>
     * The method requires the caller to have the {@link SystemLoginPermission}
     * permission.
     *
     * @return the login context
     */
    LoginContext login() throws LoginException;

    /**
     * System login, using a private principal that has all privileges. This
     * principal is not stored in any database and cannot be accessed by user.
     * <p>
     * The method requires the caller to have the {@link SystemLoginPermission}
     * permission.
     *
     * @param username the username that originated the system login
     * @return the login context
     */
    LoginContext loginAs(String username) throws LoginException;

    /**
     * Client login using the given username and password.
     */
    LoginContext login(String username, Object credentials) throws LoginException;

    /**
     * Client login using a custom callback handler to retrieve login info.
     *
     * @param cbHandler the callback handler to use to retrieve the login info
     * @return the login context
     */
    LoginContext login(CallbackHandler cbHandler) throws LoginException;

    SecurityDomain getSecurityDomain(String name);

    void addSecurityDomain(SecurityDomain domain);

    boolean isSystemId(Principal principal);

    void removeSecurityDomain(String name);

    SecurityDomain[] getSecurityDomains();

    void removeSecurityDomains();

}
