/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.List;

import junit.framework.Assert;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepositorySecurity extends SQLRepositoryTestCase {

    public TestSQLRepositorySecurity(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
                "OSGI-INF/test-repo-core-types-contrib.xml");
        openSession();
    }

    @Override
    protected void tearDown() throws Exception {
        // session.cancel();
        closeSession();
        super.tearDown();
    }

    //
    //
    // ---------------------------------------------------------
    // ----- copied from TestSecurity in nuxeo-core-facade -----
    // ---------------------------------------------------------
    //
    //

    // assumes that the global "session" belongs to an Administrator
    protected void setPermissionToAnonymous(String perm) throws ClientException {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl("anonymous");
        userEntry.addPrivilege(perm, true, false);
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        session.save();
    }

    protected void setPermissionToEveryone(String... perms)
            throws ClientException {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl(SecurityConstants.EVERYONE);
        for (String perm : perms) {
            userEntry.addPrivilege(perm, true, false);
        }
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        session.save();
    }

    protected void removePermissionToAnonymous() throws ClientException {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        acp.removeACL("test");
        doc.setACP(acp, true);
        session.save();
    }

    protected void removePermissionToEveryone() throws ClientException {
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        acp.removeACL("test");
        doc.setACP(acp, true);
        session.save();
    }

    public void testSecurity() throws ClientException {
        // temporary set an Everything privileges on the root for anonymous
        // so that we can create a folder
        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        CoreSession anonSession = openSessionAs("anonymous");
        try {
            DocumentModel root = anonSession.getRootDocument();

            DocumentModel folder = new DocumentModelImpl(
                    root.getPathAsString(), "folder#1", "Folder");
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

            assertEquals(Access.GRANT, acp.getAccess("a", "Read"));
            assertEquals(Access.UNKNOWN, acp.getAccess("a", "Write"));
            assertEquals(Access.GRANT, acp.getAccess("b", "Write"));
            assertEquals(Access.UNKNOWN, acp.getAccess("b", "Read"));
            assertEquals(Access.UNKNOWN, acp.getAccess("c", "Read"));
            assertEquals(Access.UNKNOWN, acp.getAccess("c", "Write"));

            // insert a deny ACE before the GRANT

            acp.getACL(ACL.LOCAL_ACL).add(0, new ACE("b", "Write", false));
            // store changes
            folder.setACP(acp, true);
            // refetch ac
            acp = folder.getACP();
            // check perms now
            assertEquals(Access.GRANT, acp.getAccess("a", "Read"));
            assertEquals(Access.UNKNOWN, acp.getAccess("a", "Write"));
            assertEquals(Access.DENY, acp.getAccess("b", "Write"));
            assertEquals(Access.UNKNOWN, acp.getAccess("b", "Read"));
            assertEquals(Access.UNKNOWN, acp.getAccess("c", "Read"));
            assertEquals(Access.UNKNOWN, acp.getAccess("c", "Write"));

            // create a child document and grant on it the write for b

            // remove anonymous Everything privileges on the root
            // so that it not influence test results
            removePermissionToAnonymous();
            anonSession.save(); // process invalidations

            try {
                DocumentModel folder2 = new DocumentModelImpl(
                        folder.getPathAsString(), "folder#2", "Folder");
                folder2 = anonSession.createDocument(folder2);
                fail("privilege is granted but should not be");
            } catch (DocumentSecurityException e) {
                // ok
            }

            setPermissionToAnonymous(SecurityConstants.EVERYTHING);
            anonSession.save(); // process invalidations

            root = anonSession.getRootDocument();

            // and try again - this time it should work
            DocumentModel folder2 = new DocumentModelImpl(
                    folder.getPathAsString(), "folder#2", "Folder");
            folder2 = anonSession.createDocument(folder2);

            ACP acp2 = new ACPImpl();
            acl = new ACLImpl();
            acl.add(new ACE("b", "Write", true));
            acp2.addACL(acl);

            folder2.setACP(acp2, true);
            acp2 = folder2.getACP();

            assertEquals(Access.GRANT, acp2.getAccess("a", "Read"));
            assertEquals(Access.UNKNOWN, acp2.getAccess("a", "Write"));
            assertEquals(Access.GRANT, acp2.getAccess("b", "Write"));
            assertEquals(Access.UNKNOWN, acp2.getAccess("b", "Read"));
            assertEquals(Access.UNKNOWN, acp2.getAccess("c", "Read"));
            assertEquals(Access.UNKNOWN, acp2.getAccess("c", "Write"));

            // remove anonymous Everything privileges on the root
            // so that it not influence test results
            removePermissionToAnonymous();
            anonSession.save(); // process invalidations

            setPermissionToEveryone(SecurityConstants.WRITE,
                    SecurityConstants.REMOVE, SecurityConstants.ADD_CHILDREN,
                    SecurityConstants.REMOVE_CHILDREN, SecurityConstants.READ);
            root = anonSession.getRootDocument();

            DocumentModel folder3 = new DocumentModelImpl(
                    folder.getPathAsString(), "folder#3", "Folder");
            folder3 = anonSession.createDocument(folder3);

            anonSession.removeDocument(folder3.getRef());

            removePermissionToEveryone();
            setPermissionToEveryone(SecurityConstants.REMOVE);
            anonSession.save(); // process invalidations

            try {
                folder3 = new DocumentModelImpl(folder.getPathAsString(),
                        "folder#3", "Folder");
                folder3 = anonSession.createDocument(folder3);
                Assert.fail();
            } catch (Exception e) {

            }
        } finally {
            closeSession(anonSession);
        }
    }

    public void testACLEscaping() throws ClientException {
        // temporary set an Everything privileges on the root for anonymous
        // so that we can create a folder
        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");
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

    public void testGetParentDocuments() throws ClientException {

        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        DocumentModel root = session.getRootDocument();

        String name = "Workspaces#1";
        DocumentModel workspaces = new DocumentModelImpl(
                root.getPathAsString(), name, "Workspace");
        session.createDocument(workspaces);
        String name2 = "repositoryWorkspace2#";
        DocumentModel repositoryWorkspace = new DocumentModelImpl(
                workspaces.getPathAsString(), name2, "Workspace");
        session.createDocument(repositoryWorkspace);

        String name3 = "ws#3";
        DocumentModel ws1 = new DocumentModelImpl(
                repositoryWorkspace.getPathAsString(), name3, "Workspace");
        session.createDocument(ws1);
        String name4 = "ws#4";
        DocumentModel ws2 = new DocumentModelImpl(ws1.getPathAsString(), name4,
                "Workspace");
        session.createDocument(ws2);

        ACP acp = new ACPImpl();
        ACE denyRead = new ACE("test", SecurityConstants.READ, false);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { denyRead });
        acp.addACL(acl);
        // TODO this produces a stack trace
        repositoryWorkspace.setACP(acp, true);
        ws1.setACP(acp, true);

        session.save();

        List<DocumentModel> ws2ParentsUnderAdministrator = session.getParentDocuments(ws2.getRef());
        assertTrue("list parents for" + ws2.getName() + "under " +
                session.getPrincipal().getName() + " is not empty:",
                !ws2ParentsUnderAdministrator.isEmpty());

        CoreSession testSession = openSessionAs("test");
        List<DocumentModel> ws2ParentsUnderTest = testSession.getParentDocuments(ws2.getRef());
        assertTrue("list parents for" + ws2.getName() + "under " +
                testSession.getPrincipal().getName() + " is empty:",
                ws2ParentsUnderTest.isEmpty());
        closeSession(testSession);
    }

    // copied from TestAPI in nuxeo-core-facade

    public void testPermissionChecks() throws Throwable {

        CoreSession joeReaderSession = null;
        CoreSession joeContributorSession = null;
        CoreSession joeLocalManagerSession = null;

        DocumentRef ref = createDocumentModelWithSamplePermissions("docWithPerms");

        try {
            // reader only has the right to consult the document
            joeReaderSession = openSessionAs("joe_reader");
            DocumentModel joeReaderDoc = joeReaderSession.getDocument(ref);
            try {
                joeReaderSession.saveDocument(joeReaderDoc);
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }

            try {
                joeReaderSession.createDocument(new DocumentModelImpl(
                        joeReaderDoc.getPathAsString(), "child", "File"));
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }

            try {
                joeReaderSession.removeDocument(ref);
                fail("should have raised a security exception");
            } catch (DocumentSecurityException e) {
            }
            joeReaderSession.save();

            // contributor only has the right to write the properties of
            // document
            joeContributorSession = openSessionAs("joe_contributor");
            DocumentModel joeContributorDoc = joeContributorSession.getDocument(ref);

            joeContributorSession.saveDocument(joeContributorDoc);

            DocumentRef childRef = joeContributorSession.createDocument(
                    new DocumentModelImpl(joeContributorDoc.getPathAsString(),
                            "child", "File")).getRef();
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

            // local manager can read, write, create and remove
            joeLocalManagerSession = openSessionAs("joe_localmanager");
            DocumentModel joeLocalManagerDoc = joeLocalManagerSession.getDocument(ref);

            joeLocalManagerSession.saveDocument(joeLocalManagerDoc);

            childRef = joeLocalManagerSession.createDocument(
                    new DocumentModelImpl(joeLocalManagerDoc.getPathAsString(),
                            "child2", "File")).getRef();
            joeLocalManagerSession.save();

            // joe local manager can copy the newly created doc
            joeLocalManagerSession.copy(childRef, ref, "child2_copy");

            // joe local manager cannot move the doc
            joeLocalManagerSession.move(childRef, ref, "child2_move");

            joeLocalManagerSession.removeDocument(ref);
            joeLocalManagerSession.save();

        } finally {
            Throwable rethrow = null;
            if (joeReaderSession != null) {
                try {
                    closeSession(joeReaderSession);
                } catch (Throwable t) {
                    rethrow = t;
                }
            }
            if (joeContributorSession != null) {
                try {
                    closeSession(joeContributorSession);
                } catch (Throwable t) {
                    if (rethrow == null) {
                        rethrow = t;
                    }
                }
            }
            if (joeLocalManagerSession != null) {
                try {
                    closeSession(joeLocalManagerSession);
                } catch (Throwable t) {
                    if (rethrow == null) {
                        rethrow = t;
                    }
                }
            }
            if (rethrow != null) {
                throw rethrow;
            }
        }
    }

    protected DocumentRef createDocumentModelWithSamplePermissions(String name)
            throws ClientException {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), name,
                "Folder");
        doc = session.createDocument(doc);

        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL();

        localACL.add(new ACE("joe_reader", SecurityConstants.READ, true));

        localACL.add(new ACE("joe_contributor", SecurityConstants.READ, true));
        localACL.add(new ACE("joe_contributor",
                SecurityConstants.WRITE_PROPERTIES, true));
        localACL.add(new ACE("joe_contributor", SecurityConstants.ADD_CHILDREN,
                true));

        localACL.add(new ACE("joe_localmanager", SecurityConstants.READ, true));
        localACL.add(new ACE("joe_localmanager", SecurityConstants.WRITE, true));
        localACL.add(new ACE("joe_localmanager",
                SecurityConstants.WRITE_SECURITY, true));

        acp.addACL(localACL);
        doc.setACP(acp, true);

        // add the permission to remove children on the root
        ACP rootACP = root.getACP();
        ACL rootACL = rootACP.getOrCreateACL();
        rootACL.add(new ACE("joe_localmanager",
                SecurityConstants.REMOVE_CHILDREN, true));
        rootACP.addACL(rootACL);
        root.setACP(rootACP, true);

        // make it visible for others
        session.save();
        return doc.getRef();
    }

    public void xxxtestGetAvailableSecurityPermissions() throws ClientException {
        List<String> permissions = session.getAvailableSecurityPermissions();

        // TODO
        assertTrue(permissions.contains("Everything"));
    }

}
