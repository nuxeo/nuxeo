/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.security.Access.DENY;
import static org.nuxeo.ecm.core.api.security.Access.GRANT;
import static org.nuxeo.ecm.core.api.security.Access.UNKNOWN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_SECURITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogFeature.class, LogCaptureFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-permissions-contrib.xml")
public class TestSQLRepositorySecurity {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected LogFeature logFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Before
    public void setUp() {
        if (allowNegativeAcl()) {
            Framework.getProperties().setProperty(SQLSession.ALLOW_NEGATIVE_ACL_PROPERTY, "true");
        }
    }

    @After
    public void tearDown() {
        Framework.getProperties().remove(SQLSession.ALLOW_NEGATIVE_ACL_PROPERTY);
    }

    protected CloseableCoreSession openSessionAs(String username) {
        return CoreInstance.openCoreSession(session.getRepositoryName(), username);
    }

    protected CloseableCoreSession openSessionAs(NuxeoPrincipal principal) {
        return CoreInstance.openCoreSession(session.getRepositoryName(), principal);
    }

    // overridden to test with allowed negative properties
    protected boolean allowNegativeAcl() {
        return false; // default in Nuxeo
    }

    // assumes that the global "session" belongs to an Administrator
    protected void setPermissionToAnonymous(String perm) {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl("anonymous");
        userEntry.addPrivilege(perm);
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        session.save();
    }

    protected void setPermissionToEveryone(String... perms) {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl(EVERYONE);
        for (String perm : perms) {
            userEntry.addPrivilege(perm);
        }
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        session.save();
    }

    protected void removePermissionToAnonymous() {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        acp.removeACL("test");
        doc.setACP(acp, true);
        session.save();
    }

    protected void removePermissionToEveryone() {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        acp.removeACL("test");
        doc.setACP(acp, true);
        session.save();
    }

    @Test
    public void testSecurity() {
        // temporary set an Everything privileges on the root for anonymous
        // so that we can create a folder
        setPermissionToAnonymous(EVERYTHING);

        try (CloseableCoreSession anonSession = openSessionAs("anonymous")) {
            DocumentModel root = anonSession.getRootDocument();

            DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "folder#1", "Folder");
            folder = anonSession.createDocument(folder);

            ACP acp = folder.getACP();
            assertNotNull(acp); // the acp inherited from root is returned

            acp = new ACPImpl();

            ACL acl = new ACLImpl();
            acl.add(new ACE("a", "Read", true));
            acl.add(new ACE("b", "Write", true));
            acp.addACL(acl);

            folder.setACP(acp, true);

            acp = folder.getACP();

            assertNotNull(acp);

            assertEquals("a", acp.getACL(ACL.LOCAL_ACL).get(0).getUsername());
            assertEquals("b", acp.getACL(ACL.LOCAL_ACL).get(1).getUsername());

            assertSame(GRANT, acp.getAccess("a", "Read"));
            assertSame(UNKNOWN, acp.getAccess("a", "Write"));
            assertSame(GRANT, acp.getAccess("b", "Write"));
            assertSame(UNKNOWN, acp.getAccess("b", "Read"));
            assertSame(UNKNOWN, acp.getAccess("c", "Read"));
            assertSame(UNKNOWN, acp.getAccess("c", "Write"));

            // insert a deny Write ACE before the GRANT

            acp.getACL(ACL.LOCAL_ACL).add(0, new ACE("b", "Write", false));
            // store changes
            folder.setACP(acp, true);
            // refetch ac
            acp = folder.getACP();
            // check perms now
            assertSame(GRANT, acp.getAccess("a", "Read"));
            assertSame(UNKNOWN, acp.getAccess("a", "Write"));
            assertSame(DENY, acp.getAccess("b", "Write"));
            assertSame(UNKNOWN, acp.getAccess("b", "Read"));
            assertSame(UNKNOWN, acp.getAccess("c", "Read"));
            assertSame(UNKNOWN, acp.getAccess("c", "Write"));

            // create a child document and grant on it the write for b

            // remove anonymous Everything privileges on the root
            // so that it not influence test results
            removePermissionToAnonymous();
            anonSession.save(); // process invalidations

            try {
                DocumentModel folder2 = session.createDocumentModel(folder.getPathAsString(), "folder#2", "Folder");
                folder2 = anonSession.createDocument(folder2);
                fail("privilege is granted but should not be");
            } catch (DocumentSecurityException e) {
                // ok
            }

            setPermissionToAnonymous(EVERYTHING);
            anonSession.save(); // process invalidations

            root = anonSession.getRootDocument();

            // and try again - this time it should work
            DocumentModel folder2 = session.createDocumentModel(folder.getPathAsString(), "folder#2", "Folder");
            folder2 = anonSession.createDocument(folder2);

            ACP acp2 = new ACPImpl();
            acl = new ACLImpl();
            acl.add(new ACE("b", "Write", true));
            acp2.addACL(acl);

            folder2.setACP(acp2, true);
            acp2 = folder2.getACP();

            assertSame(GRANT, acp2.getAccess("a", "Read"));
            assertSame(UNKNOWN, acp2.getAccess("a", "Write"));
            assertSame(GRANT, acp2.getAccess("b", "Write"));
            assertSame(UNKNOWN, acp2.getAccess("b", "Read"));
            assertSame(UNKNOWN, acp2.getAccess("c", "Read"));
            assertSame(UNKNOWN, acp2.getAccess("c", "Write"));

            // remove anonymous Everything privileges on the root
            // so that it not influence test results
            removePermissionToAnonymous();
            anonSession.save(); // process invalidations

            setPermissionToEveryone(WRITE, REMOVE, ADD_CHILDREN, REMOVE_CHILDREN, READ);
            root = anonSession.getRootDocument();

            DocumentModel folder3 = session.createDocumentModel(folder.getPathAsString(), "folder#3", "Folder");
            folder3 = anonSession.createDocument(folder3);

            anonSession.removeDocument(folder3.getRef());

            removePermissionToEveryone();
            setPermissionToEveryone(REMOVE);
            anonSession.save(); // process invalidations

            try {
                folder3 = session.createDocumentModel(folder.getPathAsString(), "folder#3", "Folder");
                folder3 = anonSession.createDocument(folder3);
                fail();
            } catch (Exception e) {

            }
        }
    }

    @Test
    public void testACLEscaping() {
        // temporary set an Everything privileges on the root for anonymous
        // so that we can create a folder
        setPermissionToAnonymous(EVERYTHING);

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "folder1", "Folder");
        folder = session.createDocument(folder);

        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("xyz", "Read", true));
        acl.add(new ACE("abc@def<&>/ ", "Read", true));
        acl.add(new ACE("caf\u00e9", "Read", true));
        acl.add(new ACE("o'hara", "Read", true)); // name to quote
        acl.add(new ACE("A_x1234_", "Read", true)); // name to quote
        acp.addACL(acl);
        folder.setACP(acp, true);

        // check what we read
        acp = folder.getACP();
        assertNotNull(acp);
        acl = acp.getACL(ACL.LOCAL_ACL);
        assertEquals("xyz", acl.get(0).getUsername());
        assertEquals("abc@def<&>/ ", acl.get(1).getUsername());
        assertEquals("caf\u00e9", acl.get(2).getUsername());
        assertEquals("o'hara", acl.get(3).getUsername());
        assertEquals("A_x1234_", acl.get(4).getUsername());
    }

    @Test
    public void testGetParentDocuments() {

        setPermissionToAnonymous(EVERYTHING);

        DocumentModel root = session.getRootDocument();

        String name = "Workspaces#1";
        DocumentModel workspaces = session.createDocumentModel(root.getPathAsString(), name, "Workspace");
        session.createDocument(workspaces);
        String name2 = "repositoryWorkspace2#";
        DocumentModel repositoryWorkspace = session.createDocumentModel(workspaces.getPathAsString(), name2, "Workspace");
        repositoryWorkspace = session.createDocument(repositoryWorkspace);

        String name3 = "ws#3";
        DocumentModel ws1 = session.createDocumentModel(repositoryWorkspace.getPathAsString(), name3, "Workspace");
        ws1 = session.createDocument(ws1);
        String name4 = "ws#4";
        DocumentModel ws2 = session.createDocumentModel(ws1.getPathAsString(), name4, "Workspace");
        session.createDocument(ws2);

        if (session.isNegativeAclAllowed()) {
            ACP acp = new ACPImpl();
            ACE denyRead = new ACE("test", READ, false);
            ACL acl = new ACLImpl();
            acl.setACEs(new ACE[] { denyRead });
            acp.addACL(acl);
            // TODO this produces a stack trace
            repositoryWorkspace.setACP(acp, true);
            ws1.setACP(acp, true);
        }

        session.save();

        List<DocumentModel> ws2ParentsUnderAdministrator = session.getParentDocuments(ws2.getRef());
        assertTrue("list parents for" + ws2.getName() + "under " + session.getPrincipal().getName() + " is not empty:",
                !ws2ParentsUnderAdministrator.isEmpty());

        try (CloseableCoreSession testSession = openSessionAs("test")) {
            List<DocumentModel> ws2ParentsUnderTest = testSession.getParentDocuments(ws2.getRef());
            assertTrue("list parents for" + ws2.getName() + "under " + testSession.getPrincipal().getName()
                    + " is empty:", ws2ParentsUnderTest.isEmpty());
        }
    }

    @Test
    public void testACPInheritance() throws Exception {
        DocumentModel root = session.createDocumentModel("/", "testACPInheritance", "Folder");
        root = session.createDocument(root);
        DocumentModel doc = session.createDocumentModel("/testACPInheritance", "folder", "Folder");
        doc = session.createDocument(doc);

        ACP rootAcp = root.getACP();
        ACL localACL = rootAcp.getOrCreateACL();
        localACL.add(new ACE("joe_reader", READ, true));
        root.setACP(rootAcp, true);

        ACP acp = doc.getACP();
        localACL = acp.getOrCreateACL();
        localACL.add(new ACE("joe_contributor", WRITE, true));
        doc.setACP(acp, true);

        session.save();

        doc = session.getDocument(new PathRef("/testACPInheritance/folder"));
        acp = doc.getACP();
        ACL acl = acp.getACL(ACL.INHERITED_ACL);

        assertEquals("joe_reader", acl.getACEs()[0].getUsername());

        // block inheritance
        acp.getOrCreateACL().add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        doc.setACP(acp, true);
        session.save();

        // now the inherited acl should be null
        doc = session.getDocument(new PathRef("/testACPInheritance/folder"));
        acp = doc.getACP();
        acl = acp.getACL(ACL.INHERITED_ACL);
        assertNull(acl);
    }

    @Test
    public void testPermissionChecks() throws Throwable {
        DocumentRef ref = createDocumentModelWithSamplePermissions("docWithPerms");

        try (CloseableCoreSession joeReaderSession = openSessionAs("joe_reader")) {
            // reader only has the right to consult the document
            DocumentModel joeReaderDoc = joeReaderSession.getDocument(ref);
            try {
                joeReaderSession.saveDocument(joeReaderDoc);
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }

            try {
                joeReaderSession.createDocument(session.createDocumentModel(joeReaderDoc.getPathAsString(), "child", "File"));
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }

            try {
                joeReaderSession.removeDocument(ref);
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }
            joeReaderSession.save();
        }

        // contributor only has the right to write the properties of
        // document
        try (CloseableCoreSession joeContributorSession = openSessionAs("joe_contributor")) {
            DocumentModel joeContributorDoc = joeContributorSession.getDocument(ref);

            joeContributorSession.saveDocument(joeContributorDoc);

            DocumentRef childRef = joeContributorSession.createDocument(
                    session.createDocumentModel(joeContributorDoc.getPathAsString(), "child", "File")).getRef();
            joeContributorSession.save();

            // joe contributor can copy the newly created doc
            joeContributorSession.copy(childRef, ref, "child_copy");

            // joe contributor cannot move the doc
            try {
                joeContributorSession.move(childRef, ref, "child_move");
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }

            // joe contributor cannot remove the folder either
            try {
                joeContributorSession.removeDocument(ref);
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }
            joeContributorSession.save();
        }

        // local manager can read, write, create and remove
        try (CloseableCoreSession joeLocalManagerSession = openSessionAs("joe_localmanager")) {
            DocumentModel joeLocalManagerDoc = joeLocalManagerSession.getDocument(ref);

            joeLocalManagerSession.saveDocument(joeLocalManagerDoc);

            DocumentRef childRef = joeLocalManagerSession.createDocument(
                    session.createDocumentModel(joeLocalManagerDoc.getPathAsString(), "child2", "File")).getRef();
            joeLocalManagerSession.save();

            // joe local manager can copy the newly created doc
            joeLocalManagerSession.copy(childRef, ref, "child2_copy");

            // joe local manager cannot move the doc
            joeLocalManagerSession.move(childRef, ref, "child2_move");

            joeLocalManagerSession.removeDocument(ref);
            joeLocalManagerSession.save();
        }

    }

    protected DocumentRef createDocumentModelWithSamplePermissions(String name) {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = session.createDocumentModel(root.getPathAsString(), name, "Folder");
        doc = session.createDocument(doc);

        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL();

        localACL.add(new ACE("joe_reader", READ, true));

        localACL.add(new ACE("joe_contributor", READ, true));
        localACL.add(new ACE("joe_contributor", WRITE_PROPERTIES, true));
        localACL.add(new ACE("joe_contributor", ADD_CHILDREN, true));

        localACL.add(new ACE("joe_localmanager", READ, true));
        localACL.add(new ACE("joe_localmanager", WRITE, true));
        localACL.add(new ACE("joe_localmanager", WRITE_SECURITY, true));

        acp.addACL(localACL);
        doc.setACP(acp, true);

        // add the permission to remove children on the root
        ACP rootACP = root.getACP();
        ACL rootACL = rootACP.getOrCreateACL();
        rootACL.add(new ACE("joe_localmanager", REMOVE_CHILDREN, true));
        rootACP.addACL(rootACL);
        root.setACP(rootACP, true);

        // make it visible for others
        session.save();
        return doc.getRef();
    }

    @Test
    @Ignore
    public void testGetAvailableSecurityPermissions() {
        List<String> permissions = session.getAvailableSecurityPermissions();

        // TODO
        assertTrue(permissions.contains("Everything"));
    }

    @Test
    public void testReadAclSecurity() {
        // Check that all permissions that contain Browse enable to list a
        // document using aclOptimization
        SecurityService securityService = NXCore.getSecurityService();
        String[] browsePermissions = securityService.getPermissionsToCheck(BROWSE);
        // Check for test permission contribution
        assertTrue(Arrays.asList(browsePermissions).contains("ViewTest"));
        List<String> docNames = new ArrayList<String>(browsePermissions.length);
        DocumentModel root = session.getRootDocument();
        for (String permission : browsePermissions) {
            // Create a folder with only the browse permission
            String name = "joe-has-" + permission + "-permission";
            docNames.add(name);
            DocumentModel folder = session.createDocumentModel(root.getPathAsString(), name, "Folder");
            folder = session.createDocument(folder);
            ACP acp = folder.getACP();
            assertNotNull(acp); // the acp inherited from root is returned
            acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acl.add(new ACE("joe", permission, true));
            acp.addACL(acl);
            folder.setACP(acp, true);
        }
        session.save();

        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list;
            list = joeSession.query("SELECT * FROM Folder");
            List<String> names = new ArrayList<String>();
            for (DocumentModel doc : list) {
                names.add(doc.getName());
            }
            assertEquals("Expecting " + docNames + " got " + names, browsePermissions.length, list.size());

            list = joeSession.query("SELECT * FROM Folder WHERE ecm:isProxy = 0");
            names.clear();
            for (DocumentModel doc : list) {
                names.add(doc.getName());
            }
            assertEquals("Expecting " + docNames + " got " + names, browsePermissions.length, list.size());

            // Add a new folder to update the read acls
            DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "new-folder", "Folder");
            folder = session.createDocument(folder);
            ACP acp = folder.getACP();
            assertNotNull(acp); // the acp inherited from root is returned
            acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acl.add(new ACE("joe", browsePermissions[0], true));
            acl.add(new ACE("bob", browsePermissions[0], true));
            acp.addACL(acl);
            folder.setACP(acp, true);
            session.save();

            list = joeSession.query("SELECT * FROM Folder");
            assertEquals(browsePermissions.length + 1, list.size());
        }
    }

    @Test
    public void testReadAclSecurityUpdate() {
        // check that aclOptimization update the user aclr cache
        // NXP-13109
        DocumentModel root = session.getRootDocument();
        // Create a doc and set a new ACLR on it
        DocumentModel doc = session.createDocumentModel(root.getPathAsString(), "foo", "Folder");
        doc = session.createDocument(doc);
        ACP acp = doc.getACP();
        assertNotNull(acp);
        acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Everyone", "Read", true));
        acp.addACL(acl);
        doc.setACP(acp, true);
        session.save();

        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list;
            list = joeSession.query("SELECT * FROM Folder");
            assertEquals(1, list.size());
            // Remove the document, so the ACLR created is not anymore assigned
            session.removeDocument(doc.getRef());
            session.save();
            list = joeSession.query("SELECT * FROM Folder");
            assertEquals(0, list.size());
        }

        try (CloseableCoreSession bobSession = openSessionAs("bob")) {
            DocumentModelList list;
            // Perform a query to init the ACLR cache
            list = bobSession.query("SELECT * FROM Folder");
            assertEquals(0, list.size());
            // Create a new doc with the same ACLR
            doc = session.createDocumentModel(root.getPathAsString(), "bar", "Folder");
            doc = session.createDocument(doc);
            doc.setACP(acp, true);
            session.save();
            // Check that the ACLR has been added to the user cache
            list = bobSession.query("SELECT * FROM Folder");
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testReadAclAfterSetACP() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/folder", "doc", "File");
        doc = session.createDocument(doc);
        session.save();

        // nothing can be seen by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM Folder");
            assertEquals(0, list.size());
            list = joeSession.query("SELECT * FROM File");
            assertEquals(0, list.size());
        }

        // set ACL on folder
        ACL acl = new ACLImpl();
        acl.add(new ACE("Everyone", "Read", true));
        ACP acp = new ACPImpl();
        acp.addACL(acl);
        folder.setACP(acp, true);
        session.save();

        // now joe sees things
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM Folder");
            assertEquals(1, list.size());
            assertEquals(folder.getId(), list.get(0).getId());
            list = joeSession.query("SELECT * FROM File");
            assertEquals(1, list.size());
            assertEquals(doc.getId(), list.get(0).getId());
        }
    }

    @Test
    public void testReadAclAfterCreate() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        session.save();

        // folder can be seen by a member
        UserPrincipal joeMember = new UserPrincipal("joe", Arrays.asList("Everyone", "members"), false, false);
        try (CloseableCoreSession joeSession = openSessionAs(joeMember)) {
            DocumentModelList list = joeSession.query("SELECT * FROM Folder");
            assertEquals(1, list.size());
            assertEquals(folder.getId(), list.get(0).getId());
        }

        // but not as a non-member
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM Folder");
            assertEquals(0, list.size());
        }

        // set ACL on folder
        ACL acl = new ACLImpl();
        acl.add(new ACE("Everyone", "Read", true));
        ACP acp = new ACPImpl();
        acp.addACL(acl);
        folder.setACP(acp, true);
        session.save();

        // the folder can be seen by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM Folder");
            assertEquals(1, list.size());
            assertEquals(folder.getId(), list.get(0).getId());
        }

        // create a doc under the folder
        DocumentModel doc = session.createDocumentModel("/folder", "doc", "File");
        doc = session.createDocument(doc);
        session.save();

        // the doc can be seen by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM File");
            assertEquals(1, list.size());
            assertEquals(doc.getId(), list.get(0).getId());
        }
    }

    @Test
    public void testReadAclAfterMove() {
        DocumentModel folder1 = session.createDocumentModel("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel folder2 = session.createDocumentModel("/", "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel doc = session.createDocumentModel("/folder1", "doc", "File");
        doc = session.createDocument(doc);

        // set ACL on folder2
        ACL acl = new ACLImpl();
        acl.add(new ACE("Everyone", "Read", true));
        ACP acp = new ACPImpl();
        acp.addACL(acl);
        folder2.setACP(acp, true);
        session.save();

        // doc under folder1 cannot be read by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM File");
            assertEquals(0, list.size());
        }

        // move doc under folder2
        session.move(doc.getRef(), folder2.getRef(), null);
        session.save();

        // check doc now readable by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM File");
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testReadAclAfterCopy() {
        DocumentModel folder1 = session.createDocumentModel("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel folder2 = session.createDocumentModel("/", "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel doc = session.createDocumentModel("/folder1", "doc", "File");
        doc = session.createDocument(doc);

        // set ACL on folder2
        ACL acl = new ACLImpl();
        acl.add(new ACE("Everyone", "Read", true));
        ACP acp = new ACPImpl();
        acp.addACL(acl);
        folder2.setACP(acp, true);
        session.save();

        // doc under folder1 cannot be read by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM File");
            assertEquals(0, list.size());
        }

        // copy doc under folder2
        session.copy(doc.getRef(), folder2.getRef(), "doccopy");
        session.save();

        // check doc copy now readable by joe
        try (CloseableCoreSession joeSession = openSessionAs("joe")) {
            DocumentModelList list = joeSession.query("SELECT * FROM File");
            assertEquals(1, list.size());
            assertEquals("doccopy", list.get(0).getName());
        }
    }

    @Test
    public void testEmptyLocalACL() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        // don't add anything
        doc.setACP(acp, true);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        session = coreFeature.reopenCoreSession();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        acl = acp.getACL(ACL.LOCAL_ACL);
        assertNull(acl);
    }

    public static class LogDuplicateFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LogEvent event) {
            Level level = event.getLevel();
            if (!Level.WARN.equals(level)) {
                return false;
            }
            String msg = event.getMessage().getFormattedMessage();
            return msg.contains("duplicate entry");
        }
    }

    @Inject
    LogCaptureFeature.Result logCaptureResults;

    @Test
    @LogCaptureFeature.FilterWith(value = LogDuplicateFilter.class)
    public void shouldRemoveDuplicateACE() throws Exception {
        // Using helper prevent adding duplicates
        ACL acl = new ACLImpl();
        ACE ace = new ACE("leela", "Read");
        ACE ace2 = new ACE("bob", "Read");
        ACE acedup = new ACE("bob", "Read");
        assertTrue(acl.add(ace));
        assertTrue(acl.add(ace2));
        assertFalse(acl.add(ace));
        assertFalse(acl.add(acedup));
        assertEquals(2, acl.size());

        // Using setACEs at your own risk
        ACE[] aces = {ace, ace2, ace, acedup};
        logFeature.hideWarningFromConsoleLog();
        try {
            acl.setACEs(aces);
        } finally {
            logFeature.restoreConsoleLog();
        }
        assertEquals(4, acl.size());
        // at least we have a warning about duplicate
        logCaptureResults.assertHasEvent();
    }

    @Test
    public void testReadAclOnLargeTree() {
        String enabledProp = "nuxeo.core.readacl.async.enabled";
        String thresholdProp = "nuxeo.core.readacl.async.threshold";
        Framework.getProperties().put(enabledProp, "true");
        Framework.getProperties().put(thresholdProp, "10");
        try {
            doTestReadAclOnLargeTree();
        } finally {
            Framework.getProperties().remove(enabledProp);
            Framework.getProperties().remove(thresholdProp);
        }
    }

    protected void doTestReadAclOnLargeTree() {
        DocumentModel rootFolder = session.createDocumentModel("/", "folder", "Folder");
        rootFolder = session.createDocument(rootFolder);
        String firstUser = "mickey";
        String secondUser = "minnie";

        // set ACL for first user on root folder
        ACP acp = new ACPImpl();
        acp.addACE(ACL.LOCAL_ACL, new ACE(firstUser, READ, true));
        rootFolder.setACP(acp, true);

        int nbLevels = 10;
        int nbPerLevel = 10;
        DocumentModel parent = rootFolder;
        for (int level = 0; level < nbLevels; level++) {
            DocumentModel folder = session.createDocumentModel(parent.getPathAsString(), "folder-" + level, "Folder");
            folder = session.createDocument(folder);
            for (int i = 0; i < nbPerLevel; i++) {
                DocumentModel doc = session.createDocumentModel(folder.getPathAsString(), "doc-" + level + "-" + i,
                        "File");
                doc = session.createDocument(doc);
            }
            parent = folder;
        }
        session.save();
        int nbDocs = 1 + nbLevels * (nbPerLevel + 1);

        // check that only first user has access to everything, but not second user
        assertEquals(nbDocs, numberOfReadableDocuments(firstUser));
        assertEquals(0, numberOfReadableDocuments(secondUser));

        // set ACL for user on root folder
        acp.addACE(ACL.LOCAL_ACL, new ACE(secondUser, READ, true));
        rootFolder.setACP(acp, true);
        session.save();

        // wait for asynchronous stuff to finish
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        eventService.waitForAsyncCompletion();

        // check that both users now have access to everything
        assertEquals(nbDocs, numberOfReadableDocuments(firstUser));
        assertEquals(nbDocs, numberOfReadableDocuments(secondUser));
    }

    protected int numberOfReadableDocuments(String username) {
        try (CloseableCoreSession userSession = openSessionAs(username)) {
            String nxql = "SELECT ecm:uuid FROM Document";
            PartialList<Map<String, Serializable>> pl = userSession.queryProjection(nxql, 0, 0);
            return pl.size();
        }
    }

}
