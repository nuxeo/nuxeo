/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.core.tests;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.core.service.UserWorkspaceServiceImplComponent;
import org.nuxeo.runtime.api.Framework;

public class TestUserWorkspace extends SQLRepositoryTestCase {

    protected CoreSession userSession;

    public TestUserWorkspace() {
        super("");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.api");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.types");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.core");
        fireFrameworkStarted();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        if (userSession != null) {
            closeSession(userSession);
        }
        super.tearDown();
    }

    public void testRestrictedAccess() throws Exception {
        userSession = openSessionAs("toto");
        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
        assertNotNull(uw);

        // check creator
        String creator = (String) uw.getProperty("dublincore", "creator");
        assertEquals(creator, "toto");

        // check write access
        uw.setProperty("dublibore", "description", "Toto's workspace");
        userSession.saveDocument(uw);
        userSession.save();
    }

    public void testMultiDomains() throws Exception {
        ACE ace = new ACE("Everyone", "Read", true);
        ACL acl = new ACLImpl();
        acl.add(ace);
        ACP acp = new ACPImpl();
        acp.addACL(acl);

        DocumentModel ws1 = session.createDocumentModel(
                "/default-domain/workspaces", "ws1", "Workspace");
        ws1 = session.createDocument(ws1);
        ws1.setACP(acp, true);
        ws1 = session.saveDocument(ws1);

        DocumentModel alternate = session.createDocumentModel("/",
                "alternate-domain", "Domain");
        alternate = session.createDocument(alternate);
        DocumentModel ws2 = session.createDocumentModel(
                "/alternate-domain/workspaces", "ws2", "Workspace");
        ws2 = session.createDocument(ws2);
        ws2.setACP(acp, true);
        ws2 = session.saveDocument(ws2);

        session.save();

        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        userSession = openSessionAs("toto");

        // access from root
        DocumentModel context = userSession.getRootDocument();
        DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession,
                null);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/default-domain"));

        // access form default domain
        context = userSession.getDocument(ws1.getRef());
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/default-domain"));

        // access form alternate domain
        context = userSession.getDocument(ws2.getRef());
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString(),
                uw.getPathAsString().startsWith("/default-domain"));

        // now delete the default-domain
        session.removeDocument(new PathRef("/default-domain"));
        session.save();
        userSession.save();
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/alternate-domain"));
    }

    public void testMultiDomainsCompat() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core",
                "OSGI-INF/compatUserWorkspaceImpl.xml");

        ACE ace = new ACE("Everyone", "Read", true);
        ACL acl = new ACLImpl();
        acl.add(ace);
        ACP acp = new ACPImpl();
        acp.addACL(acl);

        DocumentModel ws1 = session.createDocumentModel(
                "/default-domain/workspaces", "ws1", "Workspace");
        ws1 = session.createDocument(ws1);
        ws1.setACP(acp, true);
        ws1 = session.saveDocument(ws1);

        DocumentModel alternate = session.createDocumentModel("/",
                "alternate-domain", "Domain");
        alternate = session.createDocument(alternate);
        DocumentModel ws2 = session.createDocumentModel(
                "/alternate-domain/workspaces", "ws2", "Workspace");
        ws2 = session.createDocument(ws2);
        ws2.setACP(acp, true);
        ws2 = session.saveDocument(ws2);

        session.save();

        // force reset
        UserWorkspaceServiceImplComponent.reset();
        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        userSession = openSessionAs("toto");

        // access from root
        DocumentModel context = userSession.getRootDocument();
        DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession,
                null);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/default-domain"));

        // access form default domain
        context = userSession.getDocument(ws1.getRef());
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/default-domain"));

        // access form alternate domain
        context = userSession.getDocument(ws2.getRef());
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/alternate-domain"));

        // now delete the default-domain
        session.removeDocument(new PathRef("/default-domain"));
        session.save();
        userSession.save();
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/alternate-domain"));
    }

    public void testAnotherUserWorkspaceFinder() throws ClientException {
        UserWorkspaceService service = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(service);

        DocumentModel context = session.getRootDocument();
        DocumentModel uw = service.getCurrentUserPersonalWorkspace("user1",
                context);
        session.save();

        assertNotNull(uw);
        String user1WorkspacePath = uw.getPathAsString();
        assertTrue(user1WorkspacePath.contains("user1"));

        session.save();
        userSession = openSessionAs("user2");
        context = userSession.getRootDocument();
        try {
            // Assert that it throw a ClientException
            service.getCurrentUserPersonalWorkspace("user1", context);
            assertTrue("user2 is not allow to read user1 workspace", false);
        } catch (ClientException e) {
            // Nothing to do
        }

        uw = service.getUserPersonalWorkspace("user1", context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().contains("user1"));
        assertEquals(user1WorkspacePath, uw.getPathAsString());
        assertNull("Document is correctly detached", uw.getSessionId());
    }

    public void testUnrestrictedFinderCorrectlyCreateWorkspace()
            throws ClientException {
        UserWorkspaceService service = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(service);

        DocumentModel context = session.getRootDocument();
        DocumentModel uw = service.getUserPersonalWorkspace("user1", context);
        assertNotNull(uw);
        String user1WorkspacePath = uw.getPathAsString();

        session.save();
        userSession = openSessionAs("user1");
        context = userSession.getRootDocument();

        uw = service.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertEquals(user1WorkspacePath, uw.getPathAsString());
    }
}
