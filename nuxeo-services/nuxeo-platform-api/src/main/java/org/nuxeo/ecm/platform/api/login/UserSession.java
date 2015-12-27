/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
