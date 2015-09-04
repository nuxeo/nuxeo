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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.core.service.UserWorkspaceServiceImplComponent;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.userworkspace.api", //
        "org.nuxeo.ecm.platform.dublincore", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.directory.api", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
})
public class TestUserWorkspace {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserWorkspaceService uwm;

    @Test
    public void testRestrictedAccess() throws Exception {
        try (CoreSession userSession = coreFeature.openCoreSession("toto")) {
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
    }

    @Test
    public void testMultiDomains() throws Exception {
        ACE ace = new ACE("Everyone", "Read", true);
        ACL acl = new ACLImpl();
        acl.add(ace);
        ACP acp = new ACPImpl();
        acp.addACL(acl);

        DocumentModel ws1 = session.createDocumentModel("/default-domain/workspaces", "ws1", "Workspace");
        ws1 = session.createDocument(ws1);
        ws1.setACP(acp, true);
        ws1 = session.saveDocument(ws1);

        DocumentModel alternate = session.createDocumentModel("/", "alternate-domain", "Domain");
        alternate = session.createDocument(alternate);
        DocumentModel ws2 = session.createDocumentModel("/alternate-domain/workspaces", "ws2", "Workspace");
        ws2 = session.createDocument(ws2);
        ws2.setACP(acp, true);
        ws2 = session.saveDocument(ws2);

        session.save();

        try (CoreSession userSession = coreFeature.openCoreSession("toto")) {
            // access from root
            DocumentModel context = userSession.getRootDocument();
            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
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
            assertTrue(uw.getPathAsString(), uw.getPathAsString().startsWith("/default-domain"));

            // now delete the default-domain
            session.removeDocument(new PathRef("/default-domain"));
            session.save();
            userSession.save();
            uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
            assertNotNull(uw);
            assertTrue(uw.getPathAsString().startsWith("/alternate-domain"));
        }
    }

    @Test
    // @LocalDeploy("org.nuxeo.ecm.platform.userworkspace.core:OSGI-INF/compatUserWorkspaceImpl.xml")
    public void testMultiDomainsCompat() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/compatUserWorkspaceImpl.xml");
        uwm = Framework.getService(UserWorkspaceService.class); // re-compute
        try {
            doTestMultiDomainsCompat();
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/compatUserWorkspaceImpl.xml");
            uwm = Framework.getService(UserWorkspaceService.class); // re-compute
        }
    }

    protected void doTestMultiDomainsCompat() throws Exception {
        ACE ace = new ACE("Everyone", "Read", true);
        ACL acl = new ACLImpl();
        acl.add(ace);
        ACP acp = new ACPImpl();
        acp.addACL(acl);

        DocumentModel ws1 = session.createDocumentModel("/default-domain/workspaces", "ws1", "Workspace");
        ws1 = session.createDocument(ws1);
        ws1.setACP(acp, true);
        ws1 = session.saveDocument(ws1);

        DocumentModel alternate = session.createDocumentModel("/", "alternate-domain", "Domain");
        alternate = session.createDocument(alternate);
        DocumentModel ws2 = session.createDocumentModel("/alternate-domain/workspaces", "ws2", "Workspace");
        ws2 = session.createDocument(ws2);
        ws2.setACP(acp, true);
        ws2 = session.saveDocument(ws2);

        session.save();

        // force reset
        UserWorkspaceServiceImplComponent.reset();
        uwm = Framework.getService(UserWorkspaceService.class); // re-compute

        try (CoreSession userSession = coreFeature.openCoreSession("toto")) {
            // access from root
            DocumentModel context = userSession.getRootDocument();
            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
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
    }

    @Test
    public void testAnotherUserWorkspaceFinder() {
        DocumentModel context = session.getRootDocument();
        DocumentModel uw = uwm.getCurrentUserPersonalWorkspace("user1", context);
        session.save();

        assertNotNull(uw);
        String user1WorkspacePath = uw.getPathAsString();
        assertTrue(user1WorkspacePath.contains("user1"));

        session.save();

        try (CoreSession userSession = coreFeature.openCoreSession("user2")) {
            context = userSession.getRootDocument();
            try {
                // Assert that it throws
                uwm.getCurrentUserPersonalWorkspace("user1", context);
                assertTrue("user2 is not allow to read user1 workspace", false);
            } catch (DocumentSecurityException e) {
                // Nothing to do
            }
            uw = uwm.getUserPersonalWorkspace("user1", context);
            assertNotNull(uw);
            assertTrue(uw.getPathAsString().contains("user1"));
            assertEquals(user1WorkspacePath, uw.getPathAsString());
            assertNull("Document is correctly detached", uw.getSessionId());
        }
    }

    @Test
    public void testUnrestrictedFinderCorrectlyCreateWorkspace() {
        DocumentModel context = session.getRootDocument();
        DocumentModel uw = uwm.getUserPersonalWorkspace("user1", context);
        assertNotNull(uw);
        String user1WorkspacePath = uw.getPathAsString();

        session.save();

        try (CoreSession userSession = coreFeature.openCoreSession("user1")) {
            context = userSession.getRootDocument();

            uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
            assertNotNull(uw);
            assertEquals(user1WorkspacePath, uw.getPathAsString());
        }
    }
}
