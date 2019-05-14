/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.local;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * A dummy {@link LoginModule} for tests which logs the given user and create a {@link UserPrincipal principal}.
 *
 * @since 11.1
 */
public class DummyClientLoginModule implements LoginModule {

    protected Subject subject;

    private CallbackHandler callbackHandler;

    protected Map<String, ?> sharedState;

    protected NuxeoPrincipal principal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;

    }

    @Override
    public boolean login() throws LoginException {
        // dummy login
        NameCallback nc = new NameCallback("Username: ");
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(new Callback[] { nc, pc });
        } catch (UnsupportedCallbackException | IOException e) {
            LoginException loginException = new LoginException("Authentications Failure - " + e.getMessage());
            loginException.initCause(e);
            throw loginException;
        }

        String username = nc.getName();
        char[] password = pc.getPassword();

        // do a dummy anonymous/administrator control based on username
        boolean isAnonymous = SecurityConstants.ANONYMOUS.equals(username);
        boolean isAdministrator = SecurityConstants.ADMINISTRATOR.equals(username);
        principal = new UserPrincipal(username, null, isAnonymous, isAdministrator);
        principal.setPassword(String.valueOf(password));
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (principal != null) {
            subject.getPrincipals().add(principal);
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        principal = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        if (principal != null) {
            subject.getPrincipals().remove(principal);
        }
        return true;
    }
}
