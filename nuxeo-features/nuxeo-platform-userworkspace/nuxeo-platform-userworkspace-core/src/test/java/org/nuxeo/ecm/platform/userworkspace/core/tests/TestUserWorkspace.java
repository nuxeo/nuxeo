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
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/userworkspace-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/userWorkspaceImpl.xml");
        openSession();
    }

    public void testRestrictedAccess() throws Exception {

        CoreSession session = openSessionAs("toto");
        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(session, null);
        assertNotNull(uw);

        // check creator
        String creator = (String) uw.getProperty("dublincore", "creator");
        assertEquals(creator, "toto");

        // check write access
        uw.setProperty("dublibore", "description", "Toto's workspace");
        session.saveDocument(uw);
        session.save();
    }

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

        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        CoreSession userSession = openSessionAs("toto");

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
        assertTrue(uw.getPathAsString().startsWith("/default-domain"));

        // now delete the default-domain
        session.removeDocument(new PathRef("/default-domain"));
        session.save();
        userSession.save();
        uw = uwm.getCurrentUserPersonalWorkspace(userSession, context);
        assertNotNull(uw);
        assertTrue(uw.getPathAsString().startsWith("/alternate-domain"));
    }

    public void testMultiDomainsCompat() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/compatUserWorkspaceImpl.xml");

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
        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        CoreSession userSession = openSessionAs("toto");

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
