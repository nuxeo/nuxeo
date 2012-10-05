/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISObject;
import org.apache.chemistry.CMISRuntimeException;
import org.apache.chemistry.Connection;
import org.apache.chemistry.ConstraintViolationException;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.ObjectNotFoundException;
import org.apache.chemistry.Property;
import org.apache.chemistry.Repository;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Tree;
import org.apache.chemistry.Type;
import org.apache.chemistry.impl.simple.SimpleContentStream;
import org.apache.chemistry.impl.simple.SimpleObjectId;
import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public abstract class NuxeoChemistryTestCase extends SQLRepositoryTestCase {

    public static final String ROOT_TYPE_ID = "Root"; // from Nuxeo

    public static final String ROOT_FOLDER_NAME = ""; // from NuxeoProperty

    public static final String DELETE_TRANSITION = "delete";

    protected Repository repository;

    protected Connection conn;

    protected SPI spi;

    protected static String file5id;

    /**
     * Must be implemented by concrete testing classes.
     */
    public abstract Repository makeRepository() throws Exception;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // deployed for fulltext indexing
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        // event listener and query maker service
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        // CMIS query maker
        deployBundle("org.nuxeo.ecm.core.chemistry.impl");

        // MyDocType
        deployContrib("org.nuxeo.ecm.core.chemistry.tests.test",
                "OSGI-INF/types-contrib.xml");

        openSession();

        // cmis
        try {
            repository = makeRepository();
            openConn();
        } catch (Exception e) {
            super.tearDown();
        }
    }

    @Override
    public void tearDown() throws Exception {
        closeConn();
        closeSession();
        super.tearDown();
    }

    protected void openConn() {
        openConn(null);
    }

    protected void openConn(Map<String, Serializable> parameters) {
        conn = repository.getConnection(parameters);
        spi = conn.getSPI();
    }

    protected void closeConn() {
        if (conn != null) {
            conn.close();
            conn = null;
        }
        spi = null;
    }

    protected static Calendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        return cal;
    }

    public static Repository makeNuxeoRepository(CoreSession session)
            throws Exception {

        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = "Noodles with rice";
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        blob1.setFilename(filename);
        file1.setPropertyValue("content", blob1);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:coverage", "foo/bar");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "something");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "football");
        file2 = session.createDocument(file2);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1",
                "testfile3", "Note");
        file3.setPropertyValue("note", "this is a note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description",
                "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors",
                new String[] { "bob", "john" });
        file3 = session.createDocument(file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2",
                "Folder");
        folder2 = session.createDocument(folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "something");
        file4 = session.createDocument(file4);

        // create deleted file
        DocumentModel file5 = new DocumentModelImpl("/testfolder1",
                "testfile5", "File");
        file5.setPropertyValue("dc:title", "title5");
        file5 = session.createDocument(file5);
        file5.followTransition(DELETE_TRANSITION);
        session.saveDocument(file5);

        session.save();

        file5id = file5.getId();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();

        return new NuxeoRepository(session.getRepositoryName());
    }

    public void testBasic() throws Exception {
        Folder root = conn.getRootFolder();
        assertNotNull(root);

        Type rootType = root.getType();
        assertNotNull(rootType);
        assertEquals(ROOT_TYPE_ID, rootType.getId());
        assertEquals(ROOT_TYPE_ID, root.getTypeId());
        assertEquals(ROOT_FOLDER_NAME, root.getName());
        assertNull(root.getParent());

        Map<String, Property> props = root.getProperties();
        assertNotNull(props);
        assertTrue(props.size() > 0);

        List<CMISObject> entries = root.getChildren();
        assertEquals(2, entries.size());

        Folder folder = conn.getFolder("/testfolder1");
        Document file = null;
        for (CMISObject child : folder.getChildren()) {
            String name = child.getName();
            if (name.equals("testfile1")) {
                file = (Document) child;
            }
        }
        assertNotNull(file);

        // get stream
        ContentStream cs = file.getContentStream();
        assertNotNull(cs);
        assertEquals("testfile.txt", cs.getFileName());
        assertEquals("text/plain", cs.getMimeType());
        assertEquals(17, cs.getLength());
        assertEquals("Noodles with rice", FileUtils.read(cs.getStream()));

        // set stream
        cs = new SimpleContentStream("foo".getBytes(), "text/html", "foo.html");
        file.setContentStream(cs);
        file.save();
    }

    public void testDeletedInTrash() throws Exception {
        Folder folder = conn.getFolder("/testfolder1");

        ObjectEntry ent = spi.getObjectByPath("/testfolder1/testfile5", null);
        assertNull("file 5 should be in trash", ent);
        CMISObject ob = conn.getObject(spi.newObjectId(file5id));
        assertNull("file 5 should be in trash", ob);

        for (CMISObject child : folder.getChildren()) {
            if (child.getName().equals("testfile5")) {
                fail("file 5 should be in trash");
            }
        }
        for (ObjectEntry child : spi.getChildren(folder, null, null, null)) {
            if (child.getValue(Property.NAME).equals("testfile5")) {
                fail("file 5 should be in trash");
            }
        }

        String query = "SELECT cmis:objectId FROM cmis:document WHERE dc:title = 'title5'";
        ListPage<ObjectEntry> col = spi.query(query, false, null, null);
        assertTrue("file 5 should be in trash", col.isEmpty());

        // test trashed child doesn't block folder delete
        spi.deleteObject(spi.getObjectByPath("/testfolder1/testfile1", null),
                true);
        spi.deleteObject(spi.getObjectByPath("/testfolder1/testfile2", null),
                true);
        spi.deleteObject(spi.getObjectByPath("/testfolder1/testfile3", null),
                true);
        spi.deleteObject(folder, true);
    }

    public void testTrees() throws Exception {
        Tree<ObjectEntry> tree;
        Folder root = conn.getRootFolder();
        ObjectEntry fold2 = spi.getObjectByPath("/testfolder2", null);
        tree = spi.getDescendants(root, -1, null, null);
        assertEquals(7, tree.size());
        tree = spi.getDescendants(root, 1, null, null);
        assertEquals(2, tree.size());
        tree = spi.getDescendants(root, 2, null, null);
        assertEquals(6, tree.size());
        tree = spi.getDescendants(root, 3, null, null);
        assertEquals(7, tree.size());
        tree = spi.getDescendants(root, 4, null, null);
        assertEquals(7, tree.size());
        tree = spi.getDescendants(fold2, -1, null, null);
        assertEquals(2, tree.size());
        tree = spi.getDescendants(fold2, 1, null, null);
        assertEquals(1, tree.size());
        tree = spi.getDescendants(fold2, 2, null, null);
        assertEquals(2, tree.size());
        tree = spi.getDescendants(fold2, 3, null, null);
        assertEquals(2, tree.size());
    }

    public void testDefaultProperties() throws Exception {
        Folder root = conn.getRootFolder();
        CMISObject child = root.getChildren().get(0);
        assertNotNull(child.getProperty("dc:coverage"));
        assertNull(child.getString("dc:coverage"));
    }

    public void testCreate() throws Exception {
        Folder root = conn.getRootFolder();
        Document doc = root.newDocument("File");
        doc.setName("doc");
        ContentStream cs = new SimpleContentStream("foo".getBytes("UTF-8"),
                "plain/text", "foo.txt");
        doc.setContentStream(cs);
        doc.save();
    }

    public void testCreateSPI() throws Exception {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(Property.TYPE_ID, "cmis:folder");
        properties.put(Property.NAME, "myfolder");
        ObjectId folderId = spi.createFolder(properties,
                repository.getInfo().getRootFolderId());
        assertNotNull(folderId);

        // create a doc in it
        properties.put(Property.TYPE_ID, "Note");
        properties.put(Property.NAME, "mynote");
        ObjectId noteId = spi.createDocument(properties, folderId, null, null);
        assertNotNull(noteId);

        // list children to check
        ListPage<ObjectEntry> children = spi.getChildren(folderId, null, null,
                null);
        assertEquals(1, children.size());

        ObjectEntry entry = children.get(0);
        assertEquals("Note", entry.getTypeId());
    }

    public void testCopySPI() throws Exception {
        ObjectEntry ob = spi.getObjectByPath("/testfolder1/testfile1", null);
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("dc:title", "new title");
        try {
            ObjectId id = spi.createDocumentFromSource(ob,
                    repository.getInfo().getRootFolderId(), properties, null);
            assertNotNull(id);
            assertNotSame(id, ob.getId());
        } catch (CMISRuntimeException e) {
            assertTrue(e.getMessage().contains(
                    "AtomPub bindings do not support"));
            return;
        }
        // fetch
        ObjectEntry doc = spi.getObjectByPath("/testfile1", null);
        assertNotNull(doc);
        assertEquals("new title", doc.getValue("dc:title"));
    }

    public void testCopy() throws Exception {
        ObjectEntry foldid = spi.getObjectByPath("/", null);
        Folder fold = (Folder) conn.getObject(foldid);
        ObjectEntry ob = spi.getObjectByPath("/testfolder1/testfile1", null);
        Document doc = (Document) conn.getObject(ob);
        try {
            Document newdoc = doc.copy(fold);
            assertNotNull(newdoc);
            assertNotSame(newdoc.getId(), doc.getId());
            assertEquals("testfile1_Title", newdoc.getValue("dc:title"));
        } catch (CMISRuntimeException e) {
            assertTrue(e.getMessage().contains(
                    "AtomPub bindings do not support"));
            return;
        }
    }

    public void testUpdate() throws Exception {
        byte[] blobBytes = "A file...\n".getBytes("UTF-8");
        String filename = "doc.txt";
        ContentStream cs = new SimpleContentStream(blobBytes,
                "text/plain;charset=UTF-8", filename);

        // update a doc with a content stream
        ObjectEntry ob = spi.getObjectByPath("/testfolder1/testfile1", null);
        Document doc = (Document) conn.getObject(ob);
        doc.setContentStream(cs);
        doc.setValue("dc:title", "my doc 1");
        doc.save();

        // update a doc that doesn't have a content stream yet
        ob = spi.getObjectByPath("/testfolder1/testfile2", null);
        doc = (Document) conn.getObject(ob);
        doc.setContentStream(cs);
        doc.setValue("dc:title", "my doc 2");
        doc.save();
    }

    public void testUpdateSPI() throws Exception {
        ObjectEntry ob = spi.getObjectByPath("/testfolder1/testfile1", null);
        assertEquals("testfile1_Title", ob.getValue("dc:title"));

        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("dc:title", "foo");
        ObjectId id = spi.updateProperties(ob, null, properties);
        assertEquals(ob.getId(), id.getId());

        ob = spi.getProperties(id, null);
        assertEquals("foo", ob.getValue("dc:title"));
    }

    public void testContentStreamSPI() throws Exception {
        // set
        ObjectEntry ob = spi.getObjectByPath("/testfolder1/testfile2", null);
        SimpleObjectId id = new SimpleObjectId(ob.getId());
        assertFalse(spi.hasContentStream(id)); // unfetched
        assertFalse(spi.hasContentStream(ob)); // fetched

        byte[] blobBytes = "A file...\n".getBytes("UTF-8");
        String filename = "doc.txt";
        ContentStream cs = new SimpleContentStream(blobBytes,
                "text/plain;charset=UTF-8", filename);
        spi.setContentStream(ob, cs, true);

        // refetch
        assertTrue(spi.hasContentStream(id));
        cs = spi.getContentStream(id, null);
        assertNotNull(cs);
        assertEquals(filename, cs.getFileName());
        assertEquals("text/plain;charset=UTF-8",
                cs.getMimeType().replace(" ", ""));

        InputStream in = cs.getStream();
        assertNotNull(in);

        byte[] array = IOUtils.toByteArray(in);
        assertEquals(blobBytes.length, array.length);
        assertEquals(blobBytes.length, cs.getLength());

        // delete
        spi.deleteContentStream(id);
        assertFalse(spi.hasContentStream(id));
    }

    public void testDeleteSPI() throws Exception {
        ObjectEntry doc1 = spi.getObjectByPath("/testfolder1/testfile1", null);
        spi.deleteObject(doc1, false);
        doc1 = spi.getObjectByPath("/testfolder1/testfile1", null);
        assertNull(doc1);

        try {
            spi.deleteObject(spi.newObjectId("nosuchid"), false);
            fail();
        } catch (ObjectNotFoundException e) {
            // ok
        }
        ObjectEntry folder1 = spi.getObjectByPath("/testfolder2", null);
        try {
            spi.deleteObject(folder1, false);
            fail();
        } catch (ConstraintViolationException e) {
            // ok to fail, still has children
        }
    }

    public void testDeleteTreeSPI() throws Exception {
        ObjectEntry fold1 = spi.getObjectByPath("/testfolder1", null);
        spi.deleteTree(fold1, null, true);
        ObjectEntry oe = spi.getObjectByPath("/testfolder1", null);
        assertNull(oe);

        oe = spi.getObjectByPath("/testfolder1/testfile1", null);
        assertNull(oe);

        oe = spi.getObjectByPath("/testfolder2", null);
        assertNotNull(oe);
    }

    public void testMoveSPI() throws Exception {
        ObjectEntry fold = spi.getObjectByPath("/testfolder1", null);
        ObjectEntry doc = spi.getObjectByPath(
                "/testfolder2/testfolder3/testfile4", null);
        ObjectId res = spi.moveObject(doc, fold, null);
        assertEquals(doc.getId(), res.getId());
        doc = spi.getObjectByPath("/testfolder2/testfolder3/testfile4", null);
        assertNull(doc);
        doc = spi.getObjectByPath("/testfolder1/testfile4", null);
        assertNotNull(doc);
    }

    public void testMove() throws Exception {
        ObjectEntry foldid = spi.getObjectByPath("/testfolder1", null);
        Folder fold = (Folder) conn.getObject(foldid);

        ObjectEntry docid = spi.getObjectByPath(
                "/testfolder2/testfolder3/testfile4", null);
        Document doc = (Document) conn.getObject(docid);
        doc.move(fold, null);
        assertEquals(docid.getId(), doc.getId());
        ObjectEntry d = spi.getObjectByPath(
                "/testfolder2/testfolder3/testfile4", null);
        assertNull(d);
        d = spi.getObjectByPath("/testfolder1/testfile4", null);
        assertNotNull(d);
    }

    public void testQuery() throws Exception {
        String query;
        Collection<CMISObject> res;
        List<ObjectEntry> col;
        ObjectEntry ob;
        Iterator<ObjectEntry> it;
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        assertNotNull(folder1);

        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef(
                "/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef(
                "/testfolder1/testfile3"));

        DocumentModel mydoc = new DocumentModelImpl("/", "mydoc", "MyDocType");
        mydoc.setPropertyValue("dc:title", "My Doc");
        mydoc.setPropertyValue("my:boolean", Boolean.TRUE);
        mydoc.setPropertyValue("my:date", Calendar.getInstance());
        mydoc.setPropertyValue("my:double", Double.valueOf(123.456));
        mydoc = session.createDocument(mydoc);
        session.save();

        // simple query through SPI

        query = "SELECT * FROM cmis:document";
        col = spi.query(query, false, null, null);
        assertEquals(5, col.size());

        query = "SELECT * FROM cmis:folder";
        col = spi.query(query, false, null, null);
        assertEquals(3, col.size());

        query = "SELECT cmis:objectId, dc:DESCRIPTION" //
                + " FROM cmis:document" //
                + " WHERE dc:title = 'testfile1_Title'";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());

        query = "SELECT cmis:objectId" //
                + " FROM cmis:document" //
                + " WHERE dc:title IN ('testfile1_Title', 'xyz')";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());

        query = "SELECT cmis:objectId, dc:DESCRIPTION" //
                + " FROM cmis:document" //
                + " WHERE dc:title = 'testfile1_Title'"
                + " AND dc:description <> 'argh'"
                + " AND dc:coverage <> 'zzzzz'";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());

        it = col.iterator();
        ob = it.next();
        assertEquals("testfile1_description", ob.getValue("dc:description"));
        assertEquals(file1.getId(), ob.getValue("cmis:objectId"));

        res = conn.query("SELECT * FROM cmis:document", false);
        assertNotNull(res);
        assertEquals(5, res.size());

        res = conn.query("SELECT * FROM cmis:folder", false);
        assertEquals(3, res.size());

        res = conn.query(
                "SELECT * FROM cmis:document WHERE dc:title = 'testfile1_Title'",
                false);
        assertEquals(1, res.size());

        // spec says names are case-insensitive
        res = conn.query(
                "SELECT * FROM CMIS:DOCUMENT WHERE DC:TITLE = 'testfile1_Title'",
                false);
        assertEquals(1, res.size());

        // boolean
        res = conn.query("SELECT * FROM MyDocType WHERE my:boolean = true",
                false);
        assertEquals(1, res.size());
        res = conn.query("SELECT * FROM MyDocType WHERE my:boolean <> FALSE",
                false);
        assertEquals(1, res.size());

        // decimal
        res = conn.query("SELECT * FROM MyDocType WHERE my:double = 123.456",
                false);
        assertEquals(1, res.size());
        res = conn.query("SELECT * FROM MyDocType WHERE my:double <> 123",
                false);
        assertEquals(1, res.size());
        col = spi.query("SELECT * FROM MyDocType", false, null, null);
        assertEquals(1, col.size());
        assertEquals(BigDecimal.valueOf(123.456),
                col.get(0).getValue("my:double"));
        assertTrue(col.get(0).getValue("my:double") instanceof BigDecimal);

        // datetime
        res = conn.query(
                "SELECT * FROM MyDocType WHERE my:date <> TIMESTAMP '1999-09-09T01:01:01Z'",
                false);
        assertEquals(1, res.size());
        try {
            res = conn.query(
                    "SELECT * FROM MyDocType WHERE my:date <> TIMESTAMP 'foobar'",
                    false);
            fail();
        } catch (CMISRuntimeException e) {
            // ok
        }
    }

    // note is specified as largetext in the repo config
    public void testQueryLargeTextField() throws Exception {
        String query;
        Collection<ObjectEntry> col;

        query = "SELECT note FROM Note";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());
        ObjectEntry entry = col.iterator().next();
        assertEquals("this is a note", entry.getValue("note"));
    }

    // computed properties, not directly fetchable from SQL
    // also document and folder types are post-processed
    public void testQueryComputed() throws Exception {
        String query = "SELECT cmis:objectId," //
                + "     cmis:objectTypeId," //
                + "     cmis:baseTypeId," //
                + "     cmis:contentStreamLength" //
                + " FROM cmis:document WHERE dc:title = 'testfile1_Title'";
        Collection<ObjectEntry> col = spi.query(query, false, null, null);
        assertEquals(1, col.size());
        ObjectEntry entry = col.iterator().next();
        assertEquals(Integer.valueOf(17),
                entry.getValue(Property.CONTENT_STREAM_LENGTH));
        assertEquals("File", entry.getValue(Property.TYPE_ID));
        assertEquals("cmis:document", entry.getValue(Property.BASE_TYPE_ID));
    }

    public void testQueryStar() throws Exception {
        String query;
        Collection<ObjectEntry> col;

        query = "SELECT * FROM File WHERE dc:title = 'testfile1_Title'";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());
        ObjectEntry entry = col.iterator().next();

        assertEquals("testfile1", entry.getValue(Property.NAME));
        assertEquals("File", entry.getValue(Property.TYPE_ID));
        assertEquals(BaseType.DOCUMENT.getId(),
                entry.getValue(Property.BASE_TYPE_ID));
        assertNotNull(entry.getValue(Property.ID));
        assertNotNull(entry.getValue(Property.CREATION_DATE));

        assertEquals("testfile1_Title", entry.getValue("dc:title"));
        assertEquals("testfile1_description", entry.getValue("dc:description"));
        assertEquals("foo/bar", entry.getValue("dc:coverage"));

        // these are computed properties, not directly fetched from SQL
        assertEquals(Integer.valueOf(17),
                entry.getValue(Property.CONTENT_STREAM_LENGTH));
        assertEquals("testfile.txt",
                entry.getValue(Property.CONTENT_STREAM_FILE_NAME));
    }

    public void testQueryAny() throws Exception {
        Collection<CMISObject> res;
        res = conn.query(
                "SELECT * FROM cmis:document WHERE 'pete' = ANY dc:contributors",
                false);
        assertEquals(1, res.size());

        res = conn.query(
                "SELECT * FROM cmis:document WHERE 'bob' = ANY dc:contributors",
                false);
        assertEquals(2, res.size());
    }

    // FIXME problem with ObjectEntry model visible with AtomPub
    public void XXXtestQueryJoin() throws Exception {
        String query;
        Collection<CMISObject> res;
        Collection<ObjectEntry> col;
        ObjectEntry ob;
        Iterator<ObjectEntry> it;
        DocumentModel folder1 = session.getDocument(new PathRef("/testfolder1"));
        assertNotNull(folder1);

        DocumentModel file1 = session.getDocument(new PathRef(
                "/testfolder1/testfile1"));
        DocumentModel file2 = session.getDocument(new PathRef(
                "/testfolder1/testfile2"));
        DocumentModel file3 = session.getDocument(new PathRef(
                "/testfolder1/testfile3"));

        // JOIN query through SPI

        query = "SELECT A.dc:title, B.cmis:OBJECTID, B.dc:title" //
                + " FROM cmis:folder A" //
                + " JOIN cmis:document B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.dc:title = 'testfolder1_Title'" //
                + " ORDER BY B.dc:title";
        col = spi.query(query, false, null, null);
        assertEquals(3, col.size());

        it = col.iterator();
        ob = it.next();
        assertEquals("testfolder1_Title", ob.getValue("A.dc:title"));
        assertEquals("testfile1_Title", ob.getValue("B.dc:title"));
        assertEquals(file1.getId(), ob.getValue("B.cmis:objectId"));

        ob = it.next();
        assertEquals("testfolder1_Title", ob.getValue("A.dc:title"));
        assertEquals("testfile2_Title", ob.getValue("B.dc:title"));
        assertEquals(file2.getId(), ob.getValue("B.cmis:objectId"));

        ob = it.next();
        assertEquals("testfolder1_Title", ob.getValue("A.dc:title"));
        assertEquals("testfile3_Title", ob.getValue("B.dc:title"));
        assertEquals(file3.getId(), ob.getValue("B.cmis:objectId"));
    }

    public void testQueryFulltext() throws Exception {
        Collection<CMISObject> res;
        res = conn.query(
                "SELECT * FROM cmis:document WHERE CONTAINS('testfile2_Title')",
                false);
        assertEquals(1, res.size());
        res = conn.query(
                "SELECT * FROM cmis:document WHERE NOT CONTAINS('testfile2_Title')",
                false);
        assertEquals(3, res.size());
    }

    public void testQueryScore() throws Exception {
        ListPage<ObjectEntry> res;
        res = spi.query(
                "SELECT SCORE() FROM cmis:document WHERE CONTAINS('note')",
                false, null, null);
        assertEquals(1, res.size());
        // TODO test SEARCH_SCORE present in data set
        res = spi.query(
                "SELECT SCORE() AS relevance FROM cmis:document WHERE CONTAINS('note')"
                        + " ORDER BY relevance DESC", false, null, null);
        assertEquals(1, res.size());
        // TODO test "relevance" present in data set
    }

    public void testQueryInTree() throws Exception {
        String folder1id = spi.getObjectByPath("/testfolder1", null).getId();
        String folder2id = spi.getObjectByPath("/testfolder2", null).getId();

        Collection<CMISObject> res;
        res = conn.query(
                String.format(
                        "SELECT * FROM cmis:document WHERE IN_FOLDER('%s')",
                        folder1id), false);
        assertEquals(3, res.size());

        res = conn.query(String.format(
                "SELECT * FROM cmis:document WHERE IN_TREE('%s')", folder2id),
                false);
        assertEquals(1, res.size());
    }

    public void testQuerySpecial() throws Exception {
        String folder1id = spi.getObjectByPath("/testfolder1", null).getId();
        String file4id = spi.getObjectByPath(
                "/testfolder2/testfolder3/testfile4", null).getId();

        Collection<CMISObject> res;
        res = conn.query(String.format(
                "SELECT * FROM cmis:document WHERE cmis:objectId = '%s'",
                file4id), false);
        assertEquals(1, res.size());

        res = conn.query(String.format(
                "SELECT * FROM cmis:document WHERE cmis:parentId = '%s'",
                folder1id), false);
        assertEquals(3, res.size());

        res = conn.query(
                "SELECT * FROM cmis:document WHERE cmis:objectTypeId = 'File'",
                false);
        assertEquals(3, res.size());

        res = conn.query(
                "SELECT * FROM cmis:document WHERE cmis:name = 'testfile4'",
                false);
        assertEquals(1, res.size());
    }

    public void testQueryDistinct() throws Exception {
        ListPage<ObjectEntry> res;
        res = spi.query("SELECT dc:description FROM File", false, null, null);
        assertEquals(3, res.size());
        res = spi.query("SELECT DISTINCT dc:description FROM File", false,
                null, null);
        assertEquals(2, res.size()); // file2 and file4 have same descr
    }

    // TODO connect as different user with AtomPub
    public void TODOtestQuerySecurity() throws Exception {
        String query;
        Collection<ObjectEntry> col;

        query = "SELECT * FROM cmis:document";
        col = spi.query(query, false, null, null);
        assertEquals(4, col.size());

        query = "SELECT * FROM cmis:folder";
        col = spi.query(query, false, null, null);
        assertEquals(3, col.size());

        // block members access to testfolder2
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("members", "Read", false)); // deny read
        acp.addACL(acl);
        DocumentModel folder = session.getDocument(new PathRef("/testfolder2"));
        folder.setACP(acp, true);
        session.save();
        closeConn();

        // query as someone in group members
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put("principal",
                new UserPrincipal("a_member", Arrays.asList("members")));
        openConn(parameters);

        query = "SELECT * FROM cmis:document";
        col = spi.query(query, false, null, null);
        assertEquals(3, col.size());

        query = "SELECT * FROM cmis:folder";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());
    }

    public void testQuerySecurityPolicy() throws Exception {
        deployContrib("org.nuxeo.ecm.core.query.test",
                "OSGI-INF/security-policy-contrib.xml");

        String query;
        Collection<ObjectEntry> col;

        query = "SELECT * FROM cmis:document";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size()); // just testfile3 which is a Note

        query = "SELECT * FROM cmis:folder";
        col = spi.query(query, false, null, null);
        assertEquals(3, col.size()); // policy doesn't apply

        query = "SELECT cmis:objectTypeId FROM cmis:document";
        col = spi.query(query, false, null, null);
        assertEquals(1, col.size());

        // TODO column aliases through AtomPub
        // query = "SELECT D.cmis:ObJeCtTyPeId FROM cmis:document D";
        // col = spi.query(query, false, null, null);
        // assertEquals(1, col.size());
    }

}
