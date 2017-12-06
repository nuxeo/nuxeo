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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SystemLoginModule implements LoginModule {

    private static final Log log = LogFactory.getLog(SystemLoginModule.class);

    protected Subject subject;

    protected CallbackHandler callbackHandler;

    protected Map<String, Object> sharedState = new HashMap<>();

    protected boolean trace;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.sharedState.putAll(sharedState);
        this.callbackHandler = callbackHandler;
        trace = log.isTraceEnabled();
    }

    @Override
    public boolean login() throws LoginException {
        if (trace) {
            log.trace("begin system login");
        }
        LoginService loginService =
                Framework.getService(LoginService.class);
        if (loginService == null) {
            throw new LoginException(
                    "Nuxeo Login Service is not running - cannot do system login");
        }
        CredentialsCallback cb = new CredentialsCallback();
        try {
            callbackHandler.handle(new Callback[] { cb });
        } catch (RuntimeException | IOException
                | UnsupportedCallbackException e) {
            LoginException ee =
                    new LoginException("System login failed - callback failed");
            ee.initCause(e);
            throw ee;
        }
        Object credential = cb.getCredentials();
        if (LoginComponent.isSystemLogin(credential)) {
            Principal principal = (Principal) credential;
            sharedState.put("javax.security.auth.login.name", principal);
            sharedState.put("javax.security.auth.login.password", null);
            if (trace) {
                log.trace("System Login Succeded");
            }
            return true;
        }
        if (trace) {
            log.trace("System Login Failed");
        }
        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        if (trace) {
            log.trace("commit, subject=" + subject);
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (trace) {
            log.trace("abort, subject=" + subject);
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        if (trace) {
            log.trace("logout, subject=" + subject);
        }
        return true;
    }

}
