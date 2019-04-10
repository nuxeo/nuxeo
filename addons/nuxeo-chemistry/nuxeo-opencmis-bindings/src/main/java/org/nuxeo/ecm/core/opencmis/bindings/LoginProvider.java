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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Interface defining a login method, used during SOAP authentication.
 * <p>
 * The provider can be redefined by a Nuxeo Framework property named with the full name of this interface.
 */
public interface LoginProvider {

    /**
     * Log in the user given the username and password, and returns a login context.
     *
     * @param username the username
     * @param password the password
     * @return the login context
     * @throws LoginException if the user cannot be logged in
     */
    LoginContext login(String username, String password) throws LoginException;

}
