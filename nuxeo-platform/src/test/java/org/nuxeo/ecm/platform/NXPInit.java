/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform;

import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.SecurityAssociationHandler;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.platform.util.CoreFacadeBusinessDelegate;

/**
 * Remote tests through the ejb facade.
 *
 * @author Razvan Caraghin
 * @deprectaed obsolete tests - should be re moved
 * FIXME: remove these tests they are deprectaed
 */
public abstract class NXPInit extends TestCase {

    protected Properties properties;

    protected Subject authenticatedSubject;

    protected LoginContext loginContext;

    protected void setUp() throws Exception {
        super.setUp();

        _loadTestingConfiguration();
        _defaultSecurityManager();
        _authenticate();
        _initializeServerInstance();
    }

    protected void tearDown() throws Exception {
        _unauthenticate();
        _uninitializeServerInstance();

        properties = null;
        authenticatedSubject = null;
        loginContext = null;

        super.tearDown();
    }

    protected void _loadTestingConfiguration() {
        System.setProperty("java.security.auth.login.config",
                Thread.currentThread().getContextClassLoader().getResource("nuxeo_jaas.config").getPath());
        System.setProperty("java.security.policy",
                Thread.currentThread().getContextClassLoader().getResource("nuxeo.policy").getPath());
    }

    protected void _defaultSecurityManager() {
        if (null == System.getSecurityManager()) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    protected void _authenticate() throws LoginException {
        // XXX JBoss-specific way to authenthicate using JBossSX
        SecurityAssociationHandler handler = new SecurityAssociationHandler();
        SimplePrincipal user = new SimplePrincipal("q");
        handler.setSecurityInfo(user, "q".toCharArray());
        loginContext = new LoginContext("nuxeo.ecm", handler);

        loginContext.login();
        authenticatedSubject = loginContext.getSubject();
    }

    protected void _unauthenticate() throws LoginException {
        loginContext.logout();
    }

    protected void _initializeServerInstance() throws ClientException {
        CoreInstance.getInstance().initialize(
                new CoreFacadeBusinessDelegate(null));
    }

    protected void _uninitializeServerInstance() throws Exception {
        // TODO: removing the session produces a failure on the next new session
        // open - need to investigate why
        // CoreInstance.getInstance().close(handle);
    }
}
