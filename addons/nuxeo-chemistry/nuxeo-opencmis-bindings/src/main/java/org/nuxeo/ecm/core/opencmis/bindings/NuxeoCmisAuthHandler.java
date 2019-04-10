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
package org.nuxeo.ecm.core.opencmis.bindings;

import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.impl.webservices.AbstractService;
import org.apache.chemistry.opencmis.server.impl.webservices.AuthHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.Authenticator;

import com.sun.xml.ws.api.handler.MessageHandlerContext;

/**
 * SOAP handler that extracts authentication information from the SOAP headers
 * and propagates it to Nuxeo for login.
 */
public class NuxeoCmisAuthHandler extends AuthHandler implements LoginProvider {

    private static final Log log = LogFactory.getLog(NuxeoCmisAuthHandler.class);

    public static final String NUXEO_LOGIN_CONTEXT = "nuxeo.opencmis.LoginContext";

    /** Framework property redefining the login provider class. */
    public static final String LOGIN_PROVIDER_PROP = LoginProvider.class.getName();

    protected LoginProvider loginProvider;

    @Override
    public boolean handleMessage(MessageHandlerContext context) {
        boolean res = super.handleMessage(context);
        @SuppressWarnings("unchecked")
        Map<String, String> callContextMap = (Map<String, String>) context.get(AbstractService.CALL_CONTEXT_MAP);
        if (callContextMap != null) {
            // login to Nuxeo
            String username = callContextMap.get(CallContext.USERNAME);
            String password = callContextMap.get(CallContext.PASSWORD);
            try {
                LoginContext loginContext = getLoginProvider().login(username,
                        password);
                // store in message context, for later logout
                context.put(NUXEO_LOGIN_CONTEXT, loginContext);
                context.setScope(NUXEO_LOGIN_CONTEXT, Scope.APPLICATION);
            } catch (LoginException e) {
                throw new RuntimeException("Login failed for user '" + username
                        + "'", e);
            }
        }
        return res;
    }

    @Override
    public void close(MessageContext context) {
        LoginContext loginContext = (LoginContext) context.get(NUXEO_LOGIN_CONTEXT);
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException e) {
                log.error("Cannot logout", e);
            }
        }
        super.close(context);
    }

    protected LoginProvider getLoginProvider() {
        if (loginProvider == null) {
            loginProvider = this;
            String className = Framework.getProperty(LOGIN_PROVIDER_PROP);
            if (className != null) {
                try {
                    Object instance = Class.forName(className).newInstance();
                    if (instance instanceof LoginProvider) {
                        loginProvider = (LoginProvider) instance;
                    } else {
                        log.error(className + " is not an instance of "
                                + LoginProvider.class.getName());
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
        return loginProvider;
    }

    // LoginProvider
    @Override
    public LoginContext login(String username, String password) {
        try {
            // check identity against UserManager
            if (!getAuthenticator().checkUsernamePassword(username, password)) {
                throw new RuntimeException("Authentication failed for user '"
                        + username + "'");
            }
            // login to Nuxeo framework
            return Framework.login(username, password);
        } catch (Exception e) {
            throw new RuntimeException("Login failed for user '" + username
                    + "'", e);
        }
    }

    protected Authenticator getAuthenticator() {
        Authenticator userManager;
        try {
            userManager = Framework.getService(Authenticator.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get Authenticator service", e);
        }
        if (userManager == null) {
            throw new RuntimeException("Cannot get Authenticator service");
        }
        return userManager;
    }

}
