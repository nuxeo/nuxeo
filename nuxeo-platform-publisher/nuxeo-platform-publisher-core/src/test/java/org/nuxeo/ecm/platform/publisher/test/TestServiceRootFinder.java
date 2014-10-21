/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import org.hsqldb.jdbc.jdbcDataSource;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.impl.finder.AbstractRootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class TestServiceRootFinder extends SQLRepositoryTestCase {

    protected DocumentModel doc2Publish;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        NuxeoContainer.installNaming();

        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:jena");
        ds.setUser("sa");
        ds.setPassword("");
        NuxeoContainer.addDeepBinding(
                "java:comp/env/jdbc/nxrelations-default-jena", ds);
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseType", "HSQL");
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");

        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");

        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/test-sql-directories-contrib.xml");

        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/relations-default-jena-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/publisher-content-template-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/publish-facets-contrib.xml");

        deployBundle("org.nuxeo.ecm.platform.publisher.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.publisher.core");

        fireFrameworkStarted();
        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            closeSession();
        } finally {
            if (NuxeoContainer.isInstalled()) {
                NuxeoContainer.uninstall();
            }
            super.tearDown();
        }
    }

    protected void createInitialDocs(String domainPath, int idx)
            throws Exception {
        DocumentModel wsRoot = session.getDocument(new PathRef(domainPath
                + "/workspaces"));

        DocumentModel ws = session.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        DocumentModel sectionsRoot = session.getDocument(new PathRef(domainPath
                + "/sections"));

        String prefix = "D" + idx + "_";

        DocumentModel section1 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), prefix + "section1", "Section");
        section1.setProperty("dublincore", "title", prefix + "section1");
        section1 = session.createDocument(section1);

        DocumentModel section2 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), prefix + "section2", "Section");
        section2.setProperty("dublincore", "title", prefix + "section2");
        section2 = session.createDocument(section2);

        DocumentModel section11 = session.createDocumentModel(
                section1.getPathAsString(), prefix + "section11", "Section");
        section11.setProperty("dublincore", "title", prefix + "section11");
        section11 = session.createDocument(section11);

        // ACL
        if (idx == 1) {
            blockACLs(sectionsRoot.getRef());
            setReadACL(section2.getRef(), "myuser1", true);
        }

        session.save();
    }

    protected void setReadACL(DocumentRef ref, String user, boolean grant)
            throws Exception {
        ACP acp = session.getACP(ref);
        ACL existingACL = acp.getOrCreateACL();
        // existingACL.clear();
        existingACL.add(new ACE(user, SecurityConstants.READ, grant));
        acp.addACL(existingACL);
        session.setACP(ref, acp, true);
    }

    protected void blockACLs(DocumentRef ref) throws Exception {
        ACP acp = session.getACP(ref);
        ACL existingACL = acp.getOrCreateACL();
        existingACL.add(new ACE(SecurityConstants.ADMINISTRATOR,
                SecurityConstants.EVERYTHING, true));
        existingACL.add(ACE.BLOCK);
        acp.addACL(existingACL);
        session.setACP(ref, acp, true);
    }

    private String dump(DocumentModelList roots) throws Exception {
        StringBuffer sb = new StringBuffer();

        sb.append("Dumping root list\n");
        for (DocumentModel doc : roots) {
            sb.append(doc.getPathAsString());
            sb.append(" -- ");
            sb.append(doc.getTitle());
            sb.append(" (");
            sb.append(doc.getId());
            sb.append(" )\n");
        }
        return sb.toString();
    }

    private void dumpNode(PublicationNode node, StringBuffer sb)
            throws Exception {
        sb.append(node.getPath());
        sb.append("\n");
        for (PublicationNode child : node.getChildrenNodes()) {
            dumpNode(child, sb);
        }
    }

    private void changeUser(String userName) throws Exception {
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        Session userdir = directoryService.open("userDirectory");
        DocumentModel userModel = userdir.getEntry(userName);
        // set it on session
        NuxeoPrincipal originalUser = (NuxeoPrincipal) session.getPrincipal();
        originalUser.setModel(userModel);
        originalUser.setName(userName);
    }

    @Test
    public void testSectionRootFinder() throws Exception {
        setReadACL(session.getRootDocument().getRef(), "myuser1", true);

        createInitialDocs("default-domain", 1);
        createInitialDocs("another-default-domain", 2);

        PublisherService service = Framework.getLocalService(PublisherService.class);

        RootSectionFinder finder = service.getRootSectionFinder(session);

        // first get all roots
        DocumentModelList roots = finder.getDefaultSectionRoots(true, true);
        assertEquals(2, roots.size());
        // System.out.println(dump(roots));

        String sectionUUID = roots.get(0).getId();
        DocumentModel ws = session.getDocument(new PathRef(
                "/default-domain/workspaces/ws1"));

        // check restrictions at workspace level
        roots = finder.getSectionRootsForWorkspace(ws, true);
        assertEquals(0, roots.size()); // no restriction
        // System.out.println(dump(roots));

        // check accessibles sections
        // should be everything since there are no restrictions
        roots = finder.getAccessibleSectionRoots(ws);
        assertEquals(2, roots.size()); // no restriction
        // System.out.println(dump(roots));

        // now create a restriction
        String[] sectionIdsArray = new String[] { sectionUUID };
        ws.setPropertyValue(AbstractRootSectionsFinder.SECTIONS_PROPERTY_NAME,
                sectionIdsArray);
        ws = session.saveDocument(ws);
        session.save();

        // reset the finder since it does contains a cache
        finder.reset();

        // fetch the restrictions for workspace
        roots = finder.getSectionRootsForWorkspace(ws);
        assertEquals(1, roots.size()); // 1 restriction
        // System.out.println(dump(roots));

        roots = finder.getAccessibleSectionRoots(ws);
        assertEquals(1, roots.size()); // 1 restriction
        // System.out.println(dump(roots));

        // change restriction
        String sectionUUID1 = session.getDocument(
                new PathRef(
                        "/another-default-domain/sections/D2_section1/D2_section11")).getId();
        String sectionUUID2 = session.getDocument(
                new PathRef("/another-default-domain/sections/D2_section2")).getId();

        sectionIdsArray = new String[] { sectionUUID1, sectionUUID2 };
        ws.setPropertyValue(AbstractRootSectionsFinder.SECTIONS_PROPERTY_NAME,
                sectionIdsArray);
        ws = session.saveDocument(ws);
        session.save();

        // change user
        changeUser("myuser1");

        finder = service.getRootSectionFinder(session);

        PublisherService ps = Framework.getLocalService(PublisherService.class);
        String treeName = ps.getAvailablePublicationTree().get(0);
        PublicationTree tree = ps.getPublicationTree(treeName, session, null);
        assertNotNull(tree);

        StringBuffer sb = new StringBuffer();
        dumpNode(tree, sb);
        // /default-domain/sections
        // /default-domain/sections/D1_section2
        assertEquals(2, sb.toString().split("\n").length);
        // System.out.println(sb.toString());

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
        // System.out.println(sb.toString());

        // now include filtering
        tree = ps.getPublicationTree(treeName2, session, null, ws);
        assertNotNull(tree);

        sb = new StringBuffer();
        dumpNode(tree, sb);
        // /another-default-domain/sections
        // /another-default-domain/sections/D2_section1/D2_section11
        // /another-default-domain/sections/D2_section2
        assertEquals(3, sb.toString().split("\n").length);
        // System.out.println(sb.toString());

    }

    @Test
    public void testSectionRootFinderContrib() throws Exception {

        PublisherService service = Framework.getLocalService(PublisherService.class);

        RootSectionFinder finder = service.getRootSectionFinder(session);

        assertTrue(finder instanceof DefaultRootSectionsFinder);

        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/publisher-finder-contrib-test.xml");

        finder = service.getRootSectionFinder(session);

        assertTrue(finder instanceof SampleRootSectionFinder);

    }

}
