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

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.Attachment;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import static org.nuxeo.ecm.core.api.Constants.CORE_BUNDLE;
import static org.nuxeo.ecm.core.api.Constants.CORE_FACADE_TESTS_BUNDLE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.*;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_SECURITY;
import static org.nuxeo.ecm.core.lifecycle.LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME;

/**
 * @author Razvan Caraghin
 */
public class TestLocalAPI extends BaseTestCase {

    @BeforeClass
    public static void startRuntime() throws Exception {
        runtime = new NXRuntimeTestCase() {};
        runtime.setUp();

        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/SecurityService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");

        runtime.deployBundle("org.nuxeo.ecm.core.event");

        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "TypeService.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "permissions-contrib.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "test-CoreExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "CoreTestExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "DemoRepository.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "LifeCycleService.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "LifeCycleServiceExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "DocumentAdapterService.xml");
    }

    // Tests

    @Test
    public void testPropertyModel() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "theDoc", "MyDocType");

        doc = session.createDocument(doc);

        DocumentPart dp = doc.getPart("MySchema");
        Property p = dp.get("long");

        assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(12);
        assertEquals(12L, p.getValue());
        session.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        assertFalse(p.isPhantom());
        assertEquals(12L, p.getValue());
        p.setValue(null);
        assertFalse(p.isPhantom());
        assertNull(p.getValue());

        session.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        // assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(13L);
        p.remove();
        assertTrue(p.isRemoved());
        assertNull(p.getValue());

        session.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        assertTrue(p.isPhantom());
        assertNull(p.getValue());
    }

    @Test
    public void testOrdering() throws Exception {
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(),
                "theParent", "OrderedFolder");

        parent = session.createDocument(parent);

        DocumentModel doc1 = new DocumentModelImpl(parent.getPathAsString(),
                "the1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl(parent.getPathAsString(),
                "the2", "File");
        doc2 = session.createDocument(doc2);

        String name1 = doc1.getName();
        String name2 = doc2.getName();

        DocumentModelList children = session.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name1, children.get(0).getName());
        assertEquals(name2, children.get(1).getName());

        session.orderBefore(parent.getRef(), name2, name1);

        children = session.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name2, children.get(0).getName());
        assertEquals(name1, children.get(1).getName());

        session.orderBefore(parent.getRef(), name2, null);

        children = session.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name1, children.get(0).getName());
        assertEquals(name2, children.get(1).getName());
    }

    @Test
    public void testPropertyXPath() throws Exception {
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(),
                "theParent", "OrderedFolder");

        parent = session.createDocument(parent);

        DocumentModel doc = new DocumentModelImpl(parent.getPathAsString(),
                "theDoc", "File");

        doc.setProperty("dublincore", "title", "my title");
        assertEquals("my title", doc.getPropertyValue("dc:title"));
        doc.setProperty("file", "filename", "the file name");
        assertEquals("the file name", doc.getPropertyValue("filename"));
        assertEquals("the file name", doc.getPropertyValue("file:filename"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testComplexList() throws Exception {
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "MyDocType");

        doc = session.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        diff.add(new Attachment("at1", "value1").asMap());
        diff.add(new Attachment("at2", "value2").asMap());
        doc.setProperty("testList", "attachments", diff);
        doc = session.saveDocument(doc);

        list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertEquals(2, list.size());

        Blob blob = (Blob) ((Map) list.get(0)).get("content");
        assertEquals("value1", blob.getString());
        blob = (Blob) ((Map) list.get(1)).get("content");
        assertEquals("value2", blob.getString());

        diff = new ListDiff();
        diff.remove(0);
        diff.insert(0, new Attachment("at1.bis", "value1.bis").asMap());
        doc.setProperty("testList", "attachments", diff);
        doc = session.saveDocument(doc);

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
        doc = session.saveDocument(doc);

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
        doc = session.saveDocument(doc);

        list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void testDataModel() throws Exception {
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "Book");

        doc = session.createDocument(doc);

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

        doc = session.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        diff.add(new Attachment("at1", "value1").asMap());
        diff.add(new Attachment("at2", "value2").asMap());
        doc.setProperty("testList", "attachments", diff);
        doc = session.saveDocument(doc);

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

    @Test
    public void testGetChildrenRefs() throws Exception {
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "Book");
        doc = session.createDocument(doc);
        DocumentModel doc2 = new DocumentModelImpl(root.getPathAsString(),
                "mydoc2", "MyDocType");
        doc2 = session.createDocument(doc2);
        List<DocumentRef> childrenRefs = session.getChildrenRefs(root.getRef(),
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

    @Test
    public void testLazyBlob() throws Exception {
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "File");

        doc = session.createDocument(doc);
        byte[] bytes = createBytes(1024 * 1024, (byte) 24);

        Blob blob = new ByteArrayBlob(bytes);
        doc.getPart("file").get("content").setValue(blob);
        doc = session.saveDocument(doc);

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

        // test that reset works
        blob.getStream().reset();

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));
    }

    @Test
    public void testProxy() throws Exception {
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "the title");
        doc = session.saveDocument(doc);
        // remote.save();

        VersionModel version = new VersionModelImpl();
        version.setCreated(Calendar.getInstance());
        version.setLabel("v1");
        session.checkIn(doc.getRef(), version);
        session.save();

        // checkout the doc to modify it
        session.checkOut(doc.getRef());
        doc.setProperty("dublincore", "title", "the title modified");
        doc = session.saveDocument(doc);
        session.saveDocument(doc);
        session.save();

        DocumentModel proxy = session.createProxy(root.getRef(), doc.getRef(),
                version, true);
        // remote.save();
        // assertEquals("the title", proxy.getProperty("common", "title"));
        // assertEquals("the title modified", doc.getProperty("common",
        // "title"));

        // make another new version
        VersionModel version2 = new VersionModelImpl();
        version2.setCreated(Calendar.getInstance());
        version2.setLabel("v2");
        session.checkIn(doc.getRef(), version2);
        // remote.save();
        session.checkOut(doc.getRef());
        DocumentModelList list = session.getChildren(root.getRef());
        assertEquals(2, list.size());

        for (DocumentModel model : list) {
            assertEquals("File", model.getType());
        }

        session.removeDocument(proxy.getRef());
        // remote.save();

        list = session.getChildren(root.getRef());
        assertEquals(1, list.size());

        // create folder to hold proxies
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());
        assertTrue(session.isCheckedOut(doc.getRef()));

        // publishDocument API
        proxy = session.publishDocument(doc, root);
        session.save(); // needed for publish-by-copy to work
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());
        assertTrue(proxy.isProxy());

        // republish a proxy
        DocumentModel proxy2 = session.publishDocument(proxy, folder);
        session.save();
        assertTrue(proxy2.isProxy());
        assertEquals(1, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());

        // a second time to check overwrite
        // XXX this test fails for mysterious reasons (hasNode doesn't detect
        // the child node that was added by the first copy -- XASession pb?)
        // remote.publishDocument(proxy, folder);
        // remote.save();
        // assertEquals(1, remote.getChildrenRefs(folder.getRef(),
        // null).size());
        // assertEquals(3, remote.getChildrenRefs(root.getRef(), null).size());

        // and without overwrite
        session.publishDocument(proxy, folder, false);
        session.save();
        assertEquals(2, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());
    }

    @Test
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
            joeLocalManagerSession = openSession("joe_localmanager");
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
            session.removeDocument(ref);
            session.save();
        }
    }

    protected static CoreSession openSession(String userName)
            throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", userName);
        ctx.put("principal", new UserPrincipal(userName));
        return CoreInstance.getInstance().open("default", ctx);
    }

    protected DocumentRef createDocumentModelWithSamplePermissions(String name)
            throws ClientException {
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), name,
                "Folder");
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
        //ACP rootACP = root.getACP();
        //ACL rootACL = rootACP.getOrCreateACL();
        //rootACL.add(new ACE("joe_localmanager", REMOVE_CHILDREN, true));
        //rootACP.addACL(rootACL);
        //root.setACP(rootACP, true);

        // make it visible for others
        session.save();
        return doc.getRef();
    }

    @Test
    public void testDocumentInitialLifecycleState() throws Exception {
        DocumentModel docProject = new DocumentModelImpl(
                root.getPathAsString(), "DocWork", "File");
        docProject = session.createDocument(docProject);
        assertEquals("project", docProject.getCurrentLifeCycleState());

        DocumentModel docApproved = new DocumentModelImpl(
                root.getPathAsString(), "DocApproved", "File");
        docApproved.putContextData(INITIAL_LIFECYCLE_STATE_OPTION_NAME, "approved");
        docApproved = session.createDocument(docApproved);
        assertEquals("approved", docApproved.getCurrentLifeCycleState());
    }

    // see identical test in TestSQLRepositoryVersioning

    @Test
    public void testVersionSecurity() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder123", "Folder");
        folder = session.createDocument(folder);
        ACP acp = new ACPImpl();
        ACE ace = new ACE("princ1", "perm1", true);
        ACL acl = new ACLImpl("acl1", false);
        acl.add(ace);
        acp.addACL(acl);
        session.setACP(folder.getRef(), acp, true);
        DocumentModel file = new DocumentModelImpl("/folder123", "file123", "File");
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

        if (!usingCustomVersioning) {
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
        // recheck security on version (works because we're administrator)
        session.getACP(version.getRef());
    }

}
