/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.login.test;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.login.NuxeoAbstractServerLoginModule;

/**
 * Dummy login module for TEST PURPOSE ONLY that will not check user/password
 * against usermanager so that any username/password will authenticate a dummy
 * user. The aim is to not use the user manager. Useful for directories tests
 * where it's not possible to use the usermanager in the test due to the cyclic
 * dependency between projects
 *
 * @since 5.9.6
 */
public class TrustLoginModule extends NuxeoAbstractServerLoginModule {

    protected Principal identity;

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        Set<Principal> principals = subject.getPrincipals();
        Principal identity = getIdentity();
        principals.add(identity);

        return true;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {

        super.initialize(subject, callbackHandler, sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nc = new NameCallback("Username: ",
                SecurityConstants.ANONYMOUS);

        try {
            callbackHandler.handle(new Callback[] {nc, });

            identity = createIdentity(nc.getName());
        } catch (Exception e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        }

        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        Principal identity = getIdentity();
        Set<Principal> principals = subject.getPrincipals();
        principals.remove(identity);

        return true;
    }

    @Override
    protected Principal getIdentity() {
        return identity;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        return null;
    }

    @Override
    protected Principal createIdentity(String username) throws Exception {
        return new UserPrincipal(username);
    }


}
