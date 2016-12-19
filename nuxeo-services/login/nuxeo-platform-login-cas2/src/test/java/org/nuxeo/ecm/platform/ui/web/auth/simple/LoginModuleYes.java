/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
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

public class LoginModuleYes extends NuxeoAbstractServerLoginModule {

    protected Principal identity;

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean commit() throws LoginException {
        Set<Principal> principals = subject.getPrincipals();
        Principal identity = getIdentity();
        principals.add(identity);

        return true;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {

        super.initialize(subject, callbackHandler, sharedState, options);
    }

    public boolean login() throws LoginException {
        NameCallback nc = new NameCallback("Username: ", SecurityConstants.ANONYMOUS);

        try {
            callbackHandler.handle(new Callback[] { nc, });

            identity = createIdentity(nc.getName());
        } catch (Exception e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        }

        return true;
    }

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
    protected Principal createIdentity(String username) {
        return new UserPrincipal(username, Collections.emptyList(), false, false);
    }

}
