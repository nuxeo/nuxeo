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
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.impl.webservices.AbstractService;
import org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.Authenticator;

/**
 * SOAP handler that extracts authentication information from the SOAP headers
 * and propagates it to Nuxeo for login.
 */
public class NuxeoCmisAuthHandler extends CXFAuthHandler implements
        LoginProvider {

    public static final String NUXEO_LOGIN_CONTEXT = "nuxeo.opencmis.LoginContext";

    private static final Log log = LogFactory.getLog(NuxeoCmisAuthHandler.class);

    protected LoginProvider loginProvider;

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean res = super.handleMessage(context);

        HttpServletRequest request = (HttpServletRequest) context.get(MessageContext.SERVLET_REQUEST);
        request.setAttribute(CmisWebServicesServlet.CMIS_VERSION, CmisVersion.CMIS_1_1);

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
            String className = Framework.getProperty(LoginProvider.class.getName());
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

    protected static Authenticator getAuthenticator() {
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
