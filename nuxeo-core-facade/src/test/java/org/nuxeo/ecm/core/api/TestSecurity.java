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
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestSecurity extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestSecurity.class);

    private final static String REPO_NAME = "default";

    CoreSession remote;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle(CoreFacadeTestConstants.SCHEMA_BUNDLE);
        deployContrib(CoreFacadeTestConstants.CORE_BUNDLE,
                "OSGI-INF/CoreService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_BUNDLE,
                "OSGI-INF/RepositoryService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "test-CoreExtensions.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "DemoRepository.xml");

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "anonymous");
        remote = CoreInstance.getInstance().open(REPO_NAME, ctx);
    }

    @Override
    protected void tearDown() throws Exception {
        CoreInstance.getInstance().close(remote);
        super.tearDown();
    }

    private static void setPermissionToAnonymous(String perm) throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "Administrator");
        CoreSession rootSession = CoreInstance.getInstance().open(REPO_NAME, ctx);
        DocumentModel doc = rootSession.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl("anonymous");
        userEntry.addPrivilege(perm, true, false);
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        rootSession.save();
        CoreInstance.getInstance().close(rootSession);
    }

    private static void removePermissionToAnonymous() throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "Administrator");
        CoreSession rootSession = CoreInstance.getInstance().open(REPO_NAME, ctx);
        DocumentModel doc = rootSession.getRootDocument();
        ACP acp = doc.getACP();
        acp.removeACL("test");
        doc.setACP(acp, true);
        rootSession.save();
        CoreInstance.getInstance().close(rootSession);
    }

    private static void setPermissionToEveryone(String... perms)
            throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "Administrator");
        CoreSession rootSession = CoreInstance.getInstance().open(REPO_NAME, ctx);
        DocumentModel doc = rootSession.getRootDocument();
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
        rootSession.save();
        CoreInstance.getInstance().close(rootSession);
    }

    private static void removePermissionToEveryone() throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "Administrator");
        CoreSession rootSession = CoreInstance.getInstance().open(REPO_NAME, ctx);
        DocumentModel doc = rootSession.getRootDocument();
        ACP acp = doc.getACP();
        acp.removeACL("test");
        doc.setACP(acp, true);
        rootSession.save();
        CoreInstance.getInstance().close(rootSession);
    }

    public void testSecurity() throws ClientException {
        // temporary set an Everything privileges on the root for anonymous
        // so that we can create a folder
        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        DocumentModel root = remote.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = remote.createDocument(folder);

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

        try {
            DocumentModel folder2 = new DocumentModelImpl(
                    folder.getPathAsString(), "folder#2", "Folder");
            folder2 = remote.createDocument(folder2);
            fail("privilege is granted but should not be");
        } catch (DocumentSecurityException e) {
            // ok
        }

        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        root = remote.getRootDocument();

        // and try again - this time it should work
        DocumentModel folder2 = new DocumentModelImpl(folder.getPathAsString(),
                "folder#2", "Folder");
        folder2 = remote.createDocument(folder2);

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

        setPermissionToEveryone(SecurityConstants.WRITE,
                SecurityConstants.REMOVE, SecurityConstants.ADD_CHILDREN,
                SecurityConstants.REMOVE_CHILDREN, SecurityConstants.READ);
        root = remote.getRootDocument();

        DocumentModel folder3 = new DocumentModelImpl(folder.getPathAsString(),
                "folder#3", "Folder");
        folder3 = remote.createDocument(folder3);

        remote.removeDocument(folder3.getRef());

        removePermissionToEveryone();
        setPermissionToEveryone(SecurityConstants.REMOVE);

        try {
            folder3 = new DocumentModelImpl(folder.getPathAsString(),
                    "folder#3", "Folder");
            folder3 = remote.createDocument(folder3);
            Assert.fail();
        } catch (Exception e) {

        }
    }

    public void testACLEscaping() throws ClientException {
        // temporary set an Everything privileges on the root for anonymous
        // so that we can create a folder
        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        DocumentModel root = remote.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");
        folder = remote.createDocument(folder);

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

    /**
     * Test for method 'org.nuxeo.ecm.core.api.AbstractSession.getParentDocuments(DocumentRef)'
     * @throws ClientException
     */
    public void testGetParentDocuments() throws ClientException {

        setPermissionToAnonymous(SecurityConstants.EVERYTHING);

        DocumentModel root = remote.getRootDocument();

        String name = "Workspaces#1";
        DocumentModel workspaces = new DocumentModelImpl(
                root.getPathAsString(), name, "Workspace");
        remote.createDocument(workspaces);
        String name2 = "repositoryWorkspace2#";
        DocumentModel repositoryWorkspace = new DocumentModelImpl(
                workspaces.getPathAsString(), name2, "Workspace");
        remote.createDocument(repositoryWorkspace);

        String name3 = "ws#3";
        DocumentModel ws1 = new DocumentModelImpl(
                repositoryWorkspace.getPathAsString(), name3, "Workspace");
        remote.createDocument(ws1);
        String name4 = "ws#4";
        DocumentModel ws2 = new DocumentModelImpl(ws1.getPathAsString(), name4,
                "Workspace");
        remote.createDocument(ws2);

        ACP acp = new ACPImpl();
        ACE denyRead = new ACE("test", SecurityConstants.READ, false);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { denyRead });
        acp.addACL(acl);
        repositoryWorkspace.setACP(acp, true);
        ws1.setACP(acp, true);

        remote.save();

        List<DocumentModel> ws2ParentsUnderAdministrator = remote.getParentDocuments(ws2.getRef());
        assertTrue("list parents for" + ws2.getName() + "under "
                + remote.getPrincipal().getName() + " is not empty:",
                !ws2ParentsUnderAdministrator.isEmpty());

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "test");
        CoreSession testSession = CoreInstance.getInstance().open(REPO_NAME, ctx);
        List<DocumentModel> ws2ParentsUnderTest = testSession.getParentDocuments(ws2.getRef());
        assertTrue("list parents for" + ws2.getName() + "under "
                + testSession.getPrincipal().getName() + " is empty:",
                ws2ParentsUnderTest.isEmpty());
    }

}
