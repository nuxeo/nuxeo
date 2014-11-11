/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.api.login;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @deprecated use instead {@link Framework#login(String, Object)}
 */
@Deprecated
public class UserSession {

    private LoginContext loginContext;

    private final String username;
    private final String password;

    public UserSession(String username) {
        this(username, null);
    }

    public UserSession(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void login() throws LoginException {
        loginContext = Framework.login(username, password);
    }

    public void logout() throws LoginException {
        if (loginContext != null) {
            loginContext.logout();
        }
    }

}
