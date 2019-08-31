/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.api.login;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface LoginService {

    /**
     * System login, using a private principal that has all privileges. This principal is not stored in any database and
     * cannot be accessed by user.
     *
     * @return the login context
     */
    LoginContext login();

    /**
     * System login, using a private principal that has all privileges. This principal is not stored in any database and
     * cannot be accessed by user.
     *
     * @param originatingUser the username that originated the system login
     * @return the login context
     */
    LoginContext loginAs(String originatingUser);

    /**
     * Client login using the given username and password.
     *
     * @deprecated since 11.1, use {@link Framework#loginUser} instead
     */
    @Deprecated
    LoginContext login(String username, Object credentials) throws LoginException;

    boolean isSystemId(Principal principal);

}
