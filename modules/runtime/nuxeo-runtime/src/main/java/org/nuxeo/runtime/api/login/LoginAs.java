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
 *     bstefanescu
 */
package org.nuxeo.runtime.api.login;

import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;

/**
 * This service is performing a login under another identity without checking the password.
 * <p>
 * You must never use this service explicitly in your code since it may be removed in future. Instead you should use
 * {@link Framework#loginAsUser(String)}
 * <p>
 * Implementors must implement this interface and expose the implementation as a Nuxeo service.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @since 5.4.0.2
 */
public interface LoginAs {

    /**
     * Perform the Login As.
     *
     * @param username
     * @return
     * @throws LoginException
     */
    NuxeoLoginContext loginAs(String username) throws LoginException;

}
