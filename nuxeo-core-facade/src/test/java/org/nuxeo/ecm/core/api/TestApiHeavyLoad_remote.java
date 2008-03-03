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

package org.nuxeo.ecm.core.api;

import java.net.URL;
import java.rmi.RMISecurityManager;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.SecurityAssociationHandler;
import org.nuxeo.ecm.core.api.ejb.CoreFacadeBusinessDelegate;


/**
 *
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class TestApiHeavyLoad_remote extends TestApiHeavyLoad {

    protected Properties properties;

    protected Subject authenticatedSubject;

    protected LoginContext loginContext;

    @Override
    public void setUp() throws Exception {
        _loadTestingConfiguration();
        _defaultSecurityManager();
        _authenticate();
        _initializeServerInstance();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        _uninitializeServerInstance();
        _uninitializeRemoteHandle();
        _unauthenticate();

        properties = null;
        authenticatedSubject = null;
        loginContext = null;
    }

    @Override
    protected CoreSession getCoreSession() {
        return remote;
    }

    protected void _loadTestingConfiguration() {
        URL url = getClass().getResource("/org/nuxeo/ecm/core/api/nuxeo_jaas.config");
        System.setProperty("java.security.auth.login.config", url.toString());
        url = getClass().getResource("/org/nuxeo/ecm/core/api/nuxeo.policy");
        System.setProperty("java.security.policy", url.toString());
    }

    protected void _defaultSecurityManager() {
        if (null == System.getSecurityManager()) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    protected void _authenticate() throws LoginException {
        // XXX JBoss specific way to authenthicate using JBossSX
        SecurityAssociationHandler handler = new SecurityAssociationHandler();
        SimplePrincipal user = new SimplePrincipal("Administrator");
        handler.setSecurityInfo(user, "Administrator".toCharArray());
        loginContext = new LoginContext("nuxeo", handler);

        loginContext.login();
        authenticatedSubject = loginContext.getSubject();
    }

    protected void _unauthenticate() throws LoginException {
        loginContext.logout();
    }

    protected void _initializeServerInstance() throws ClientException {
        CoreInstance.getInstance().initialize(
                new CoreFacadeBusinessDelegate(""));
    }

    protected void _uninitializeServerInstance() throws ClientException {
        CoreInstance.getInstance().close(remote);
    }

    protected void _uninitializeRemoteHandle() {
        if (null != remote) {
            remote.destroy();
            remote = null;
        }
    }

}
