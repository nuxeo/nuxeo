/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.publisher.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.impl.finder.AbstractRootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.test.TestServiceRootFinder.Populate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Deploy("org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-content-template-contrib.xml")
@RepositoryConfig(init = Populate.class)
public class TestServiceRootFinder extends PublisherTestCase {

    @Inject
    HotDeployer deployer;

    @Inject
    PublisherService service;

    public static class Populate implements RepositoryInit {

        protected static Populate self;

        @Override
        public void populate(CoreSession session) {
            self = this;
            createInitialDocs(session, "/default-domain", 1);
            createInitialDocs(session, "/another-default-domain", 2);
        }

        protected void createInitialDocs(CoreSession session, String domainPath, int idx) {
            DocumentModel wsRoot = session.getDocument(new PathRef(domainPath + "/workspaces"));

            DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
            ws.setProperty("dublincore", "title", "test WS");
            ws = session.createDocument(ws);

            DocumentModel sectionsRoot = session.getDocument(new PathRef(domainPath + "/sections"));

            String prefix = "D" + idx + "_";

            DocumentModel section1 = session.createDocumentModel(sectionsRoot.getPathAsString(), prefix + "section1",
                    "Section");
            section1.setProperty("dublincore", "title", prefix + "section1");
            section1 = session.createDocument(section1);

            DocumentModel section2 = session.createDocumentModel(sectionsRoot.getPathAsString(), prefix + "section2",
                    "Section");
            section2.setProperty("dublincore", "title", prefix + "section2");
            section2 = session.createDocument(section2);

            DocumentModel section11 = session.createDocumentModel(section1.getPathAsString(), prefix + "section11",
                    "Section");
            section11.setProperty("dublincore", "title", prefix + "section11");
            section11 = session.createDocument(section11);

            // ACL
            if (idx == 1) {
                blockACLs(session, sectionsRoot.getRef());
                setReadACL(session, section2.getRef(), "myuser1", true);
            }

        }

        protected void blockACLs(CoreSession session, DocumentRef ref) {
            ACP acp = session.getACP(ref);
            ACL existingACL = acp.getOrCreateACL();
            existingACL.add(new ACE(SecurityConstants.ADMINISTRATOR, SecurityConstants.EVERYTHING, true));
            existingACL.add(ACE.BLOCK);
            acp.addACL(existingACL);
            session.setACP(ref, acp, true);
        }

        protected void setReadACL(CoreSession session, DocumentRef ref, String user, boolean grant) {
            ACP acp = session.getACP(ref);
            ACL existingACL = acp.getOrCreateACL();
            // existingACL.clear();
            existingACL.add(new ACE(user, SecurityConstants.READ, grant));
            acp.addACL(existingACL);
            session.setACP(ref, acp, true);
        }

    }

    private void dumpNode(PublicationNode node, StringBuffer sb) throws Exception {
        sb.append(node.getPath());
        sb.append("\n");
        for (PublicationNode child : node.getChildrenNodes()) {
            dumpNode(child, sb);
        }
    }

    private void changeUser(String userName) throws Exception {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Session userdir = directoryService.open("userDirectory");
        DocumentModel userModel = userdir.getEntry(userName);
        // set it on session
        NuxeoPrincipal originalUser = session.getPrincipal();
        originalUser.setModel(userModel);
        originalUser.setName(userName);
    }

    @Test
    public void testSectionRootFinder() throws Exception {
        Populate.self.setReadACL(session, session.getRootDocument().getRef(), "myuser1", true);

        RootSectionFinder finder = service.getRootSectionFinder(session);

        // first get all roots
        DocumentModelList roots = finder.getDefaultSectionRoots(true, true);
        assertEquals(2, roots.size());

        String sectionUUID = roots.get(0).getId();
        DocumentModel ws = session.getDocument(new PathRef("/default-domain/workspaces/ws1"));

        // check restrictions at workspace level
        roots = finder.getSectionRootsForWorkspace(ws, true);
        assertEquals(0, roots.size()); // no restriction

        // check accessibles sections
        // should be everything since there are no restrictions
        roots = finder.getAccessibleSectionRoots(ws);
        assertEquals(2, roots.size()); // no restriction

        // now create a restriction
        String[] sectionIdsArray = new String[] { sectionUUID };
        ws.setPropertyValue(AbstractRootSectionsFinder.SECTIONS_PROPERTY_NAME, sectionIdsArray);
        ws = session.saveDocument(ws);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // reset the finder since it does contains a cache
        finder.reset();

        // fetch the restrictions for workspace
        roots = finder.getSectionRootsForWorkspace(ws);
        assertEquals(1, roots.size()); // 1 restriction
        // System.out.println(dump(roots));

        roots = finder.getAccessibleSectionRoots(ws);
        assertEquals(1, roots.size()); // 1 restriction

        // change restriction
        String sectionUUID1 = session.getDocument(
                new PathRef("/another-default-domain/sections/D2_section1/D2_section11")).getId();
        String sectionUUID2 = session.getDocument(new PathRef("/another-default-domain/sections/D2_section2")).getId();

        sectionIdsArray = new String[] { sectionUUID1, sectionUUID2 };
        ws.setPropertyValue(AbstractRootSectionsFinder.SECTIONS_PROPERTY_NAME, sectionIdsArray);
        ws = session.saveDocument(ws);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // change user
        changeUser("myuser1");

        finder = service.getRootSectionFinder(session);

        PublisherService ps = Framework.getService(PublisherService.class);
        String treeName = ps.getAvailablePublicationTree().get(0);
        PublicationTree tree = ps.getPublicationTree(treeName, session, null);
        assertNotNull(tree);

        StringBuffer sb = new StringBuffer();
        dumpNode(tree, sb);
        // /default-domain/sections
        // /default-domain/sections/D1_section2
        assertEquals(4, sb.toString().split("\n").length);

        String treeName2 = ps.getAvailablePublicationTree().get(1);
        tree = ps.getPublicationTree(treeName2, session, null);
        assertNotNull(tree);

        sb = new StringBuffer();
        dumpNode(tree, sb);
        // /another-default-domain/sections
        // /another-default-domain/sections/D2_section1
        // /another-default-domain/sections/D2_section1/D2_section11
        // /another-default-domain/sections/D2_section2
        assertEquals(4, sb.toString().split("\n").length);

        // now include filtering
        tree = ps.getPublicationTree(treeName2, session, null, ws);
        assertNotNull(tree);

        sb = new StringBuffer();
        dumpNode(tree, sb);
        // /another-default-domain/sections
        // /another-default-domain/sections/D2_section1/D2_section11
        // /another-default-domain/sections/D2_section2
        assertEquals(3, sb.toString().split("\n").length);

    }

    @Test
    public void testSectionRootFinderContrib() throws Exception {

        RootSectionFinder finder = service.getRootSectionFinder(session);

        assertTrue(finder instanceof DefaultRootSectionsFinder);

        deployer.deploy("org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-finder-contrib-test.xml");
        finder = service.getRootSectionFinder(session);
        assertTrue(finder instanceof SampleRootSectionFinder);

    }

}
