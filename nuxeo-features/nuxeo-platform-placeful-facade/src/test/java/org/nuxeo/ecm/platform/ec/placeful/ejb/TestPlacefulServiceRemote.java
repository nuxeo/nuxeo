/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestPlacefulServiceRemote.java 21301 2007-06-25 11:35:52Z bstefanescu $
 */
package org.nuxeo.ecm.platform.ec.placeful.ejb;

import java.io.Serializable;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.SecurityAssociationHandler;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ec.placeful.DBAnnotatableDocument;
import org.nuxeo.ecm.platform.util.CoreFacadeBusinessDelegate;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
public class TestPlacefulServiceRemote extends TestCase {

    protected CoreSession remote;

    protected Properties properties;

    protected Subject authenticatedSubject;

    protected LoginContext loginContext;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        _initializeTest();
        _openConnection();

        assertNotNull(remote);
    }

    @Override
    protected void tearDown() throws Exception {
        _cleanUp(_getRootDocument().getRef());
        _unInitializeTest();
        super.tearDown();
    }

    protected void _loadTestingConfiguration() {
        //URL url = getClass().getResource("/org/nuxeo/ecm/core/api/nuxeo_jaas.config");
        URL url = getClass().getResource("/org/nuxeo/ecm/platform/ec/placeful/ejb/nuxeo_jaas.config");
        System.setProperty("java.security.auth.login.config", url.toString());
        //url = getClass().getResource("/org/nuxeo/ecm/core/api/nuxeo.policy");
        url = getClass().getResource("/org/nuxeo/ecm/platform/ec/placeful/ejb/nuxeo.policy");
        System.setProperty("java.security.policy", url.toString());
    }

    protected static void _defaultSecurityManager() {
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

    protected static void _initializeServerInstance() throws ClientException {
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

    protected void _unInitializeTest() throws Exception {
        _uninitializeServerInstance();
        _uninitializeRemoteHandle();
        _unauthenticate();

        properties = null;
        authenticatedSubject = null;
        loginContext = null;
    }

    protected void _initializeTest() throws Exception {
        _loadTestingConfiguration();
        _defaultSecurityManager();
        _authenticate();
        _initializeServerInstance();
    }

    protected DocumentModel _getRootDocument() throws ClientException {
        DocumentModel root = remote.getRootDocument();

        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getRef());
        assertNotNull(root.getPathAsString());

        return root;
    }

    protected void _cleanUp(DocumentRef ref) throws ClientException {
        remote.removeChildren(ref);

        remote.save();
    }

    protected DocumentModel _createChildDocument(DocumentModel childFolder)
            throws ClientException {

        DocumentModel ret = remote.createDocument(childFolder);

        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());

        return ret;
    }

    private void _openConnection() throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        remote = CoreInstance.getInstance().open("demo", ctx);

        assertNotNull(remote);
    }


    /*
    public void testDBDocumentAdapter() throws Exception {
        DocumentModel root = _getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "file", "File");

        file = _createChildDocument(file);

        DBAnnotatableDocument adoc = file.getAdapter(DBAnnotatableDocument.class);
        adoc.setAnnotation("key1", "val1");
        adoc.setAnnotation("key2", "val2");
        assertEquals("val1", adoc.getAnnotation("key1"));
        assertEquals("val2", adoc.getAnnotation("key2"));

        adoc = file.getAdapter(DBAnnotatableDocument.class);
        assertEquals("val1", adoc.getAnnotation("key1"));
        assertEquals("val2", adoc.getAnnotation("key2"));
    }
    */


    public void testDBAnnotation() throws Exception {
        DocumentModel root = _getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "file", "File");

        file = _createChildDocument(file);

        DBAnnotatableDocument adoc = file.getAdapter(DBAnnotatableDocument.class);
        SubscriptionConfig config = new SubscriptionConfig();
        config.setId("id1");
        config.setEvent("publish");
        // annotation name is ignored here actually, because we store entity
        // which is predefined at deploy time and corresponds to db table
        adoc.setAnnotation("SubscriptionConfig", config);
    }

}
