/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.opencmis.bindings.LoginProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Login provider that does not check the password and just logs in the provided
 * user.
 */
public class TrustingLoginProvider implements LoginProvider {

    @Override
    public LoginContext login(String username, String password)
            throws LoginException {
        LoginContext loginContext = Framework.loginAsUser(username);
        Object[] principals = loginContext.getSubject().getPrincipals().toArray();
        if (principals.length > 0) {
            Principal principal = (Principal) principals[0];
            TrustingNuxeoAuthFilter.maybeMakeAdministrator(principal);
        }
        return loginContext;
    }

}
