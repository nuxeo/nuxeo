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
     * Check the given username/password pair.
     *
     * @param name
     * @param password
     * @return true is valid, false otherwise
     */
    public boolean authenticate(String name, String password);

}
