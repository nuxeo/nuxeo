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

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;

/**
 * This service is performing a login under another identity without checking the password.
 * <p>
 * You must never use this service explicitly in your code since it may be removed in future.
 * Instead you should use {@link Framework#loginAsUser(String)}
 * <p>
 * Implementors must implement this interface and expose the implementation as a Nuxeo service.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @since 5.4.0.2
 *
 */
public interface LoginAs {

    /**
     * Perform the Login As.
     * @param username
     * @return
     * @throws LoginException
     */
    public LoginContext loginAs(String username) throws LoginException;

}
