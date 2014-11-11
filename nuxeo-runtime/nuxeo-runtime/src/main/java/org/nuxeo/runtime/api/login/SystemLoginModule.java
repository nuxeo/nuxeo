/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
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
    protected Map sharedState;
    protected boolean trace;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options) {
        this.subject = subject;
        this.sharedState = sharedState;
        this.callbackHandler = callbackHandler;
        trace = log.isTraceEnabled();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean login() throws LoginException {
        if (trace) {
            log.trace("begin system login");
        }
        LoginService loginService = Framework.getLocalService(
                LoginService.class);
        if (loginService == null) {
            throw new LoginException("Nuxeo Login Service is not running - cannot do system login");
        }
        DefaultCallback cb = new DefaultCallback();
        try {
            callbackHandler.handle(new Callback[] {cb});
        } catch (Exception e) {
            throw new LoginException("System login failed - callback failed");
        }
        Object credential = cb.getCredential();
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
