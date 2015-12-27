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

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.nuxeo.runtime.api.Framework;

/**
 * A login module that will use the current registered authenticator to validate a given username / password pair.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AuthenticationLoginModule implements LoginModule {

    protected Subject subject;

    protected CallbackHandler callbackHandler;

    @SuppressWarnings("rawtypes")
    protected Map sharedState;

    protected Principal principal;

    public Principal authenticate(String[] login) {
        return Framework.getService(Authenticator.class).authenticate(login[0], login[1]);
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
    }

    protected String[] retrieveLogin() throws LoginException {
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        NameCallback nc = new NameCallback("Username: ", "guest");
        Callback[] callbacks = { nc, pc };
        try {
            String[] login = new String[2];
            callbackHandler.handle(callbacks);
            login[0] = nc.getName();
            char[] tmpPassword = pc.getPassword();
            if (tmpPassword != null) {
                login[1] = new String(tmpPassword);
            }
            pc.clearPassword();
            return login;
        } catch (IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback().toString()
                    + " not available to gather authentication information " + "from the user");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean login() throws LoginException {
        String[] login = retrieveLogin();
        try {
            principal = authenticate(login);
        } catch (RuntimeException e) {
            LoginException ee = new LoginException("Authentication failed for " + login[0]);
            ee.initCause(e);
            throw ee;
        }
        if (principal == null) {
            throw new LoginException("Authentication failed for " + login[0]);
        }
        sharedState.put("javax.security.auth.login.name", principal);
        // sharedState.put("javax.security.auth.login.password", login[1]);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (principal != null) {
            subject.getPrincipals().add(principal);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean logout() throws LoginException {
        if (principal != null) {
            subject.getPrincipals().remove(principal);
        }
        return true;
    }

}
