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

import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.TestRuntime;

/**
 *
 * @author Razvan Caraghin
 *
 */
public class TestLocalAPI extends TestAPI {

    protected RuntimeService runtime;

    @Override
    protected void setUp() throws Exception {
        // Duplicated from NXRuntimeTestCase
        runtime = Framework.getRuntime();
        if (runtime != null) {
            Framework.shutdown();
            runtime = null; // be sure no runtime is intialized (this may happen when some test crashes)
        }
        runtime = new TestRuntime();
        Framework.initialize(runtime);

        deploy("EventService.xml");

        deploy("CoreService.xml");
        deploy("TypeService.xml");
        deploy("SecurityService.xml");
        deploy("RepositoryService.xml");
        deploy("test-CoreExtensions.xml");
        deploy("CoreTestExtensions.xml");
        deploy("DemoRepository.xml");
        deploy("LifeCycleService.xml");
        deploy("LifeCycleServiceExtensions.xml");
        deploy("CoreEventListenerService.xml");
        deploy("DocumentAdapterService.xml");

        openSession();
    }

    // Duplicated from NXRuntimeTestCase
    @Override
    public void deploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            runtime.getContext().deploy(url);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to deploy bundle " + bundle);
        }
    }

    // Duplicated from NXRuntimeTestCase
    @Override
    public void undeploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            runtime.getContext().undeploy(url);
        } catch (Exception e) {
            fail("Failed to undeploy bundle " + bundle);
        }
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
//        assertTrue(p.isPhantom());
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

            doc = new DocumentModelImpl(root.getPathAsString(),
                    "mydoc2", "MyDocType");

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
            List<DocumentRef> childrenRefs = remote.getChildrenRefs(root.getRef(), null);
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
        byte[] bytes = createBytes(1024*1024, (byte)24);

        Blob blob = new ByteArrayBlob(bytes);
        doc.getPart("file").get("content").setValue(blob);
        doc = remote.saveDocument(doc);

        blob = (Blob)doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

        // test that reset works
        blob.getStream().reset();

        blob = (Blob)doc.getPart("file").get("content").getValue();
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
        // assertEquals("the title modified", doc.getProperty("common", "title"));

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

        // publishDocument API
        proxy = remote.publishDocument(doc, root);
        assertEquals(2, remote.getChildrenRefs(root.getRef(), null).size());
        assertTrue(proxy.isProxy());

        // republish a proxy
        remote.save(); // needed for publish-by-copy to work
        DocumentModel proxy2 = remote.publishDocument(proxy, root);
        assertEquals(3, remote.getChildrenRefs(root.getRef(), null).size());
        assertTrue(proxy2.isProxy());

    }
}
