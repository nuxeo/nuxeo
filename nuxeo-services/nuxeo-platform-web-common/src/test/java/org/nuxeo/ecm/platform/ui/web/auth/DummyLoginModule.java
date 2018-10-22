/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallback;

/**
 * Dummy login module that just uses the UserIdentificationInfoCallback.
 *
 * @since 10.2
 */
public class DummyLoginModule implements LoginModule {

    protected Subject subject;

    protected CallbackHandler callbackHandler;

    protected Map<String, ?> sharedState;

    protected Map<String, ?> options;

    // filled if login succeeded
    protected UserIdentificationInfo userIdent;

    // filled if commit succeeded
    protected Principal principal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
    }

    @Override
    public boolean login() throws LoginException {
        // UserIdentificationInfoCallback is recognized by the Nuxeo UserIdentificationInfoCallbackHandler
        UserIdentificationInfoCallback uiic = new UserIdentificationInfoCallback();
        try {
            callbackHandler.handle(new Callback[] { uiic });
        } catch (UnsupportedCallbackException | IOException e) {
            throw new LoginException(e.toString());
        }
        userIdent = uiic.getUserInfo();
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (userIdent == null) {
            return false;
        }
        principal = new UserPrincipal(userIdent.getUserName(), null, false, false);
        subject.getPrincipals().add(principal);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        logout();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        if (principal != null) {
            subject.getPrincipals().remove(principal);
        }
        userIdent = null;
        principal = null;
        return true;
    }

}
