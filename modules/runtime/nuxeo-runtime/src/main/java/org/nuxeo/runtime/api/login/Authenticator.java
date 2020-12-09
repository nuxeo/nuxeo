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

import java.security.Principal;

/**
 * Authenticate the given username against the given password.
 * <p>
 * This service should be exposed by a user manager framework implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Authenticator {

    /**
     * Get a principal object for the given username if the username / password pair is valid, otherwise returns null.
     * <p>
     * This method is doing the authentication of the given username / password pair and returns the corresponding
     * principal object if authentication succeeded otherwise returns null.
     *
     * @return the authenticated principal if authentication succeded otherwise null
     */
    Principal authenticate(String name, String password);

    /**
     * Check the password for the given username. Returns true if the username / password pair match, false otherwise.
     *
     * @param name the username
     * @param password the password to check
     * @return true is valid, false otherwise
     */
    boolean checkUsernamePassword(String name, String password);

}
