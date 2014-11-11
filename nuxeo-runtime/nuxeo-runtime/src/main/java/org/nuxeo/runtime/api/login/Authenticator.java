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
package org.nuxeo.runtime.api.login;

import java.security.Principal;


/**
 * Authenticate the given username against the given password.
 * <p>
 * This service should be exposed by a user manager framework implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Authenticator {

    /**
     * Get a principal object for the given username if the username / password pair is valid, otherwise returns null.
     * <p>
     * This method is doing the authentication of the given username / password pair
     * and returns the corresponding principal object if authentication succeeded
     * otherwise returns null.
     *
     * @param name
     * @param password
     * @return the authenticated principal if authentication succeded otherwise null
     * @throws Exception - if an exception occurs
     */
    public Principal authenticate(String name, String password) throws Exception;

    /**
     * Check the password for the given username.
     * Returns true if the username / password pair match, false otherwise.
     *
     * @param name the username
     * @param password the password to check
     * @return true is valid, false otherwise
     * @throws Exception - if an exception occurs
     */
    public boolean checkUsernamePassword(String name, String password) throws Exception;

}
