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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.runtime.RuntimeService;

/**
 *
 * @author Razvan Caraghin
 *
 */
public class TestLocalAPI extends TestAPI {

    protected RuntimeService runtime;

    private static final Log log = LogFactory.getLog(TestLocalAPI.class);

    protected void doDeployments() throws Exception {
        deployContrib(CoreFacadeTestConstants.CORE_BUNDLE,
                "OSGI-INF/CoreService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "TypeService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "permissions-contrib.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "RepositoryService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "test-CoreExtensions.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "CoreTestExtensions.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "DemoRepository.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "LifeCycleService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "LifeCycleServiceExtensions.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "CoreEventListenerService.xml");
        deployContrib(CoreFacadeTestConstants.CORE_FACADE_TESTS_BUNDLE,
                "DocumentAdapterService.xml");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doDeployments();
        openSession();
    }

    public void testPropertyModel() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "theDoc", "MyDocType");

        doc = remote.createDocument(doc);

        DocumentPart dp = doc.getPart("MySchema");
        Property p = dp.get("long");

        assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(12);
        assertEquals(new Long(12), p.getValue());
        remote.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        assertFalse(p.isPhantom());
        assertEquals(new Long(12), p.getValue());
        p.setValue(null);
        assertFalse(p.isPhantom());
        assertNull(p.getValue());

        remote.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        // assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(new Long(13));
        p.remove();
        assertTrue(p.isRemoved());
        assertNull(p.getValue());

        remote.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        assertTrue(p.isPhantom());
        assertNull(p.getValue());
    }

    public void testOrdering() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(),
                "theParent", "OrderedFolder");

        parent = remote.createDocument(parent);

        DocumentModel doc1 = new DocumentModelImpl(parent.getPathAsString(),
                "the1", "File");
        doc1 = remote.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl(parent.getPathAsString(),
                "the2", "File");
        doc2 = remote.createDocument(doc2);

        String name1 = doc1.getName();
        String name2 = doc2.getName();

        DocumentModelList children = remote.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name1, children.get(0).getName());
        assertEquals(name2, children.get(1).getName());

        remote.orderBefore(parent.getRef(), name2, name1);

        children = remote.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name2, children.get(0).getName());
        assertEquals(name1, children.get(1).getName());

        remote.orderBefore(parent.getRef(), name2, null);

        children = remote.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name1, children.get(0).getName());
        assertEquals(name2, children.get(1).getName());

    }

    public void testPropertyXPath() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(),
                "theParent", "OrderedFolder");

        parent = remote.createDocument(parent);

        DocumentModel doc = new DocumentModelImpl(parent.getPathAsString(),
                "theDoc", "File");

        doc.setProperty("dublincore", "title", "my title");
        assertEquals("my title", doc.getPropertyValue("dc:title"));
        doc.setProperty("file", "filename", "the file name");
        assertEquals("the file name", doc.getPropertyValue("filename"));
        assertEquals("the file name", doc.getPropertyValue("file:filename"));

    }

    @SuppressWarnings("unchecked")
    public void testComplexList() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "MyDocType");

        doc = remote.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        diff.add(new Attachment("at1", "value1").asMap());
        diff.add(new Attachment("at2", "value2").asMap());
        doc.setProperty("testList", "attachments", diff);
        doc = remote.saveDocument(doc);

        list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertEquals(2, list.size());

        Blob blob;
        blob = (Blob) ((Map) list.get(0)).get("content");
        assertEquals("value1", blob.getString());
        blob = (Blob) ((Map) list.get(1)).get("content");
        assertEquals("value2", blob.getString());

        diff = new ListDiff();
        diff.remove(0);
        diff.insert(0, new Attachment("at1.bis", "value1.bis").asMap());
        doc.setProperty("testList", "attachments", diff);
        doc = remote.saveDocument(doc);

        list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertEquals(2, list.size());

        blob = (Blob) ((Map) list.get(0)).get("content");
        assertEquals("value1.bis", blob.getString());
        blob = (Blob) ((Map) list.get(1)).get("content");
        assertEquals("value2", blob.getString());

        diff = new ListDiff();
        diff.move(0, 1);
        doc.setProperty("testList", "attachments", diff);
        doc = remote.saveDocument(doc);

        list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertEquals(2, list.size());
        blob = (Blob) ((Map) list.get(0)).get("content");
        assertEquals("value2", blob.getString());
        blob = (Blob) ((Map) list.get(1)).get("content");
        assertEquals("value1.bis", blob.getString());

        diff = new ListDiff();
        diff.removeAll();
        doc.setProperty("testList", "attachments", diff);
        doc = remote.saveDocument(doc);

        list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    public void testDataModel() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "Book");

        doc = remote.createDocument(doc);

        DataModel dm = doc.getDataModel("book");
        dm.setValue("title", "my title");
        assertEquals("my title", dm.getValue("title"));
        dm.setValue("title", "my title2");
        assertEquals("my title2", dm.getValue("title"));

        dm.setValue("price", 123);
        assertEquals(123L, dm.getValue("price"));
        dm.setValue("price", 124);
        assertEquals(124L, dm.getValue("price"));

        dm.setValue("author/pJob", "Programmer");
        assertEquals("Programmer", dm.getValue("author/pJob"));
        dm.setValue("author/pJob", "Programmer2");
        assertEquals("Programmer2", dm.getValue("author/pJob"));

        dm.setValue("author/pName/FirstName", "fname");
        assertEquals("fname", dm.getValue("author/pName/FirstName"));
        dm.setValue("author/pName/FirstName", "fname2");
        assertEquals("fname2", dm.getValue("author/pName/FirstName"));

        // list test

        doc = new DocumentModelImpl(root.getPathAsString(), "mydoc2",
                "MyDocType");

        doc = remote.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        diff.add(new Attachment("at1", "value1").asMap());
        diff.add(new Attachment("at2", "value2").asMap());
        doc.setProperty("testList", "attachments", diff);
        doc = remote.saveDocument(doc);

        dm = doc.getDataModel("testList");

        dm.setValue("attachments/item[0]/name", "at1-modif");
        assertEquals("at1-modif", dm.getValue("attachments/item[0]/name"));
        dm.setValue("attachments/item[0]/name", "at1-modif2");
        assertEquals("at1-modif2", dm.getValue("attachments/item[0]/name"));
        dm.setValue("attachments/item[1]/name", "at2-modif");
        assertEquals("at2-modif", dm.getValue("attachments/item[1]/name"));
        dm.setValue("attachments/item[1]/name", "at2-modif2");
        assertEquals("at2-modif2", dm.getValue("attachments/item[1]/name"));

    }

    public void testGetChildrenRefs() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "Book");
        doc = remote.createDocument(doc);
        DocumentModel doc2 = new DocumentModelImpl(root.getPathAsString(),
                "mydoc2", "MyDocType");
        doc2 = remote.createDocument(doc2);
        List<DocumentRef> childrenRefs = remote.getChildrenRefs(root.getRef(),
                null);
        assertEquals(2, childrenRefs.size());
        Set<String> expected = new HashSet<String>();
        expected.add(doc.getId());
        expected.add(doc2.getId());
        Set<String> actual = new HashSet<String>();
        actual.add(childrenRefs.get(0).toString());
        actual.add(childrenRefs.get(1).toString());
        assertEquals(expected, actual);
    }

    public static byte[] createBytes(int size, byte val) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, val);
        return bytes;
    }

    @SuppressWarnings("unchecked")
    public void testLazyBlob() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "File");

        doc = remote.createDocument(doc);
        byte[] bytes = createBytes(1024 * 1024, (byte) 24);

        Blob blob = new ByteArrayBlob(bytes);
        doc.getPart("file").get("content").setValue(blob);
        doc = remote.saveDocument(doc);

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

        // test that reset works
        blob.getStream().reset();

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

    }

    public void testProxy() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "proxy_test", "File");

        doc = remote.createDocument(doc);
        doc.setProperty("common", "title", "the title");
        doc = remote.saveDocument(doc);
        // remote.save();

        VersionModel version = new VersionModelImpl();
        version.setCreated(Calendar.getInstance());
        version.setLabel("v1");
        remote.checkIn(doc.getRef(), version);
        // remote.save();

        // checkout the doc to modify it
        remote.checkOut(doc.getRef());
        doc.setProperty("common", "title", "the title modified");
        doc = remote.saveDocument(doc);
        // remote.save();

        DocumentModel proxy = remote.createProxy(root.getRef(), doc.getRef(),
                version, true);
        // remote.save();
        // assertEquals("the title", proxy.getProperty("common", "title"));
        // assertEquals("the title modified", doc.getProperty("common",
        // "title"));

        // make another new version
        VersionModel version2 = new VersionModelImpl();
        version2.setCreated(Calendar.getInstance());
        version2.setLabel("v2");
        remote.checkIn(doc.getRef(), version2);
        // remote.save();

        DocumentModelList list = remote.getChildren(root.getRef());
        assertEquals(2, list.size());

        for (DocumentModel model : list) {
            assertEquals("File", model.getType());
        }

        remote.removeDocument(proxy.getRef());
        // remote.save();

        list = remote.getChildren(root.getRef());
        assertEquals(1, list.size());

        // create folder to hold proxies
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder", "Folder");
        folder = remote.createDocument(folder);
        remote.save();
        folder = remote.getDocument(folder.getRef());

        // publishDocument API
        proxy = remote.publishDocument(doc, root);
        remote.save(); // needed for publish-by-copy to work
        assertEquals(3, remote.getChildrenRefs(root.getRef(), null).size());
        assertTrue(proxy.isProxy());

        // republish a proxy
        DocumentModel proxy2 = remote.publishDocument(proxy, folder);
        remote.save();
        assertTrue(proxy2.isProxy());
        assertEquals(1, remote.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, remote.getChildrenRefs(root.getRef(), null).size());

        // a second time to check overwrite
        // XXX this test fails for mysterious reasons (hasNode doesn't detect
        // the child node that was added by the first copy -- XASession pb?)
        // remote.publishDocument(proxy, folder);
        // remote.save();
        // assertEquals(1, remote.getChildrenRefs(folder.getRef(),
        // null).size());
        // assertEquals(3, remote.getChildrenRefs(root.getRef(), null).size());

        // and without overwrite
        remote.publishDocument(proxy, folder, false);
        remote.save();
        assertEquals(2, remote.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, remote.getChildrenRefs(root.getRef(), null).size());
    }

    public void testPermissionChecks() throws Exception {

        CoreSession joeReaderSession = null;
        CoreSession joeContributorSession = null;
        CoreSession joeLocalManagerSession = null;

        DocumentRef ref = createDocumentModelWithSamplePermissions("docWithPerms");

        try {
            // reader only has the right to consult the document
            joeReaderSession = openSession("joe_reader");
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
            joeContributorSession = openSession("joe_contributor");
            DocumentModel joeContributorDoc = joeContributorSession.getDocument(ref);

            joeContributorSession.saveDocument(joeContributorDoc);

            DocumentRef childRef = joeContributorSession.createDocument(new DocumentModelImpl(
                    joeContributorDoc.getPathAsString(), "child", "File")).getRef();
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
            joeLocalManagerSession = openSession("joe_localmanager");
            DocumentModel joeLocalManagerDoc = joeLocalManagerSession.getDocument(ref);

            joeLocalManagerSession.saveDocument(joeLocalManagerDoc);

            childRef = joeLocalManagerSession.createDocument(new DocumentModelImpl(
                    joeLocalManagerDoc.getPathAsString(), "child2", "File")).getRef();
            joeLocalManagerSession.save();

            // joe local manager can copy the newly created doc
            joeLocalManagerSession.copy(childRef, ref, "child2_copy");

            // joe local manager cannot move the doc
            joeLocalManagerSession.move(childRef, ref, "child2_move");

            joeLocalManagerSession.removeDocument(ref);
            joeLocalManagerSession.save();

        } finally {
            if (joeReaderSession != null) {
                CoreInstance.getInstance().close(joeReaderSession);
            }
            if (joeContributorSession != null) {
                CoreInstance.getInstance().close(joeContributorSession);
            }
            if (joeLocalManagerSession != null) {
                CoreInstance.getInstance().close(joeLocalManagerSession);
            }
        }
    }

    protected CoreSession openSession(String userName) throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", userName);
        ctx.put("principal", new UserPrincipal(userName));
        return CoreInstance.getInstance().open("default", ctx);
    }

    protected DocumentRef createDocumentModelWithSamplePermissions(String name)
            throws ClientException {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), name,
                "Folder");
        doc = remote.createDocument(doc);

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
        rootACL.add(new ACE("joe_localmanager", SecurityConstants.REMOVE_CHILDREN, true));
        rootACP.addACL(rootACL);
        root.setACP(rootACP, true);

        // make it visible for others
        remote.save();
        return doc.getRef();
    }

    // see identical test in TestSQLRepositoryVersioning

    public void testVersionSecurity() throws Exception {
        CoreSession session = remote;
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder = session.createDocument(folder);
        ACP acp = new ACPImpl();
        ACE ace = new ACE("princ1", "perm1", true);
        ACL acl = new ACLImpl("acl1", false);
        acl.add(ace);
        acp.addACL(acl);
        session.setACP(folder.getRef(), acp, true);
        DocumentModel file = new DocumentModelImpl("/folder", "file", "File");
        file = session.createDocument(file);
        // set security
        acp = new ACPImpl();
        ace = new ACE("princ2", "perm2", true);
        acl = new ACLImpl("acl2", false);
        acl.add(ace);
        acp.addACL(acl);
        session.setACP(file.getRef(), acp, true);
        session.save();
        VersionModel vm = new VersionModelImpl();
        vm.setLabel("v1");
        session.checkIn(file.getRef(), vm);
        session.checkOut(file.getRef());

        // check security on version
        DocumentModel version = session.getDocumentWithVersion(file.getRef(), vm);
        acp = session.getACP(version.getRef());
        ACL[] acls = acp.getACLs();
        if (this.getClass().getName().equals(TestLocalAPI.class.getName())) {
            // JCR versioning (unused) does something incorrect here
            return;
        }
        // the following is only run with TestLocalAPIWithCustomVersioning
        assertEquals(2, acls.length);
        acl = acls[0];
        assertEquals(1, acl.size());
        assertEquals("princ2", acl.get(0).getUsername());
        acl = acls[1];
        assertEquals(1 + 4, acl.size()); // 1 + 4 root defaults
        assertEquals("princ1", acl.get(0).getUsername());

        // remove live document (create a proxy so the version stays)
        session.createProxy(folder.getRef(), file.getRef(), vm, true);
        session.save();
        session.removeDocument(file.getRef());
        // recheck security on version
        try {
            session.getACP(version.getRef());
            fail();
        } catch (DocumentSecurityException e) {
            // ok
        }
    }

}
