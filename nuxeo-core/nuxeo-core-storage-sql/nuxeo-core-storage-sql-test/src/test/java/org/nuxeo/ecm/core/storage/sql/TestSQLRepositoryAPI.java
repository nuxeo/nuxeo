/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeComparator;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.EventConstants;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyBeforeModificationListener;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyTestListener;
import org.nuxeo.runtime.api.Framework;

/**
 * NOTE: to run these tests in Eclipse, make sure your test runner allocates at
 * least -Xmx200M to the JVM.
 *
 * @author Florent Guillaume
 */
public class TestSQLRepositoryAPI extends SQLRepositoryTestCase {

    public TestSQLRepositoryAPI(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib.xml");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    public void testBasics() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel child = new DocumentModelImpl("/", "domain", "MyDocType");
        child = session.createDocument(child);
        session.save();

        child.setProperty("dublincore", "title", "The title");
        // use local tz
        Calendar cal = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34,
                56);
        child.setProperty("dublincore", "modified", cal);
        session.saveDocument(child);
        session.save();
        closeSession();

        // ----- new session -----
        openSession();
        // root = session.getRootDocument();
        child = session.getChild(root.getRef(), "domain");

        String title = (String) child.getProperty("dublincore", "title");
        assertEquals("The title", title);
        String description = (String) child.getProperty("dublincore",
                "description");
        assertNull(description);
        Calendar modified = (Calendar) child.getProperty("dublincore",
                "modified");
        assertEquals(cal, modified);
    }

    public void testLists() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel child = new DocumentModelImpl("/", "domain", "MyDocType");
        child = session.createDocument(child);
        session.save();

        // simple list as array
        child.setProperty("dublincore", "subjects", new String[] { "a", "b" });
        // simple list as List
        child.setProperty("dublincore", "contributors", new ArrayList<String>(
                Arrays.asList("c", "d")));
        // simple list as non-serializable array
        child.setProperty("testList", "strings", new Object[] { "e", "f" });
        // complex list as List
        child.setProperty("testList", "participants", new ArrayList<String>(
                Arrays.asList("c", "d")));
        session.saveDocument(child);
        session.save();
        closeSession();

        // ----- new session -----
        openSession();
        root = session.getRootDocument();
        child = session.getChild(root.getRef(), "domain");

        Object subjects = child.getProperty("dublincore", "subjects");
        assertTrue(subjects instanceof String[]);
        assertEquals(Arrays.asList("a", "b"),
                Arrays.asList((String[]) subjects));
        Object contributors = child.getProperty("dublincore", "contributors");
        assertTrue(contributors instanceof String[]);
        assertEquals(Arrays.asList("c", "d"),
                Arrays.asList((String[]) contributors));
        Object strings = child.getProperty("testList", "strings");
        assertTrue(strings instanceof String[]);
        assertEquals(Arrays.asList("e", "f"), Arrays.asList((String[]) strings));
        Object participants = child.getProperty("testList", "participants");
        assertTrue(participants instanceof List);
        assertEquals(Arrays.asList("c", "d"), participants);
    }

    public void testPathWithExtraSlash() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "MyDocType");
        doc = session.createDocument(doc);
        session.save();
        DocumentModelList children = session.getChildren(new PathRef("/"));
        assertEquals(1, children.size());
        children = session.getChildren(new PathRef("/doc"));
        assertEquals(0, children.size());
        children = session.getChildren(new PathRef("/doc/"));
        assertEquals(0, children.size());
    }

    public void testComplexType() throws Exception {
        // boiler plate to handle the asynchronous full-text indexing of blob
        // content in a deterministic way
        EventServiceImpl eventService = (EventServiceImpl) Framework.getLocalService(EventService.class);
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");

        DocumentModel doc = new DocumentModelImpl("/", "complex-doc",
                "ComplexDoc");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();
        session.save();

        // test setting and reading a map with an empty list
        closeSession();
        openSession();
        doc = session.getDocument(docRef);
        Map<String, Object> attachedFile = new HashMap<String, Object>();
        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        attachedFile.put("name", "some name");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        closeSession();
        eventService.waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        doc = session.getDocument(docRef);
        assertEquals(attachedFile,
                doc.getProperty("cmpf:attachedFile").getValue());
        assertEquals(attachedFile.get("vignettes"),
                doc.getProperty("cmpf:attachedFile/vignettes").getValue());

        // test fulltext indexing of complex property at level one
        DocumentModelList results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'some name'", 1);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("complex-doc", results.get(0).getTitle());

        // test setting and reading a list of maps without a complex type in the
        // maps
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        vignette.put(
                "content",
                StreamingBlob.createFromString("textblob content", "text/plain"));
        vignette.put("label", "vignettelabel");
        vignettes.add(vignette);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        closeSession();
        eventService.waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        doc = session.getDocument(docRef);
        assertEquals(
                "text/plain",
                doc.getProperty(
                        "cmpf:attachedFile/vignettes/vignette[0]/content/mime-type").getValue());
        assertEquals(
                Long.valueOf(0),
                doc.getProperty(
                        "cmpf:attachedFile/vignettes/vignette[0]/height").getValue());
        assertEquals(
                "vignettelabel",
                doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/label").getValue());

        // test fulltext indexing of complex property at level 3
        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'vignettelabel'", 2);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("complex-doc", results.get(0).getTitle());

        // test fulltext indexing of complex property at level 3 in blob
        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'textblob content'", 2);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("complex-doc", results.get(0).getTitle());

        // test setting and reading a list of maps with a blob inside the map
        byte[] binaryContent = "01AB".getBytes();
        Blob blob = StreamingBlob.createFromByteArray(binaryContent,
                "application/octet-stream");
        blob.setFilename("file.bin");
        vignette.put("content", blob);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        closeSession();
        eventService.waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        assertEquals(
                Long.valueOf(0),
                doc.getProperty(
                        "cmpf:attachedFile/vignettes/vignette[0]/height").getValue());
        assertEquals(
                "vignettelabel",
                doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/label").getValue());

        // this doesn't work due to core restrictions (BlobProperty):
        // assertEquals(blob.getFilename(), doc.getProperty(
        // "cmpf:attachedFile/vignettes/vignette[0]/content/name").getValue());
        Blob b = (Blob) doc.getProperty(
                "cmpf:attachedFile/vignettes/vignette[0]/content").getValue();
        assertEquals("file.bin", b.getFilename());

        // test deleting the list of vignette and ensure that the fulltext index
        // has been properly updated (regression test for NXP-6315)
        doc.setPropertyValue("cmpf:attachedFile/vignettes",
                new ArrayList<Map<String, Object>>());
        session.saveDocument(doc);
        session.save();

        closeSession();
        eventService.waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'vignettelabel'", 2);
        assertNotNull(results);
        assertEquals(0, results.size());

        results = session.query("SELECT * FROM Document"
                + " WHERE ecm:fulltext = 'textblob content'", 2);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    public void testComplexTypeOrdering() throws Exception {
        if (database instanceof DatabaseOracle) {
            // Oracle has problems opening and closing many connections in a
            // short time span (Listener refused the connection with the
            // following error: ORA-12519, TNS:no appropriate service handler
            // found)
            // It seems to have something to do with how closed sessions are not
            // immediately accounted for by Oracle's PMON (process monitor)
            // So don't run this test with Oracle.
            return;
        }

        // test case to reproduce an ordering content related Heisenbug on
        // postgresql: NXP-2810: Preserve creation order of children of a
        // complex type property in SQL storage with PostgreSQL

        // create documents with a list of ordered vignettes
        createComplexDocs(0, 5);

        // check that the created docs hold their complex content in the
        // creation order
        checkComplexDocs(0, 5);

        // add some more docs
        createComplexDocs(5, 10);

        // check that both the old and new document still hold their complex
        // content in the same creation order
        checkComplexDocs(0, 10);
    }

    protected void createComplexDocs(int iMin, int iMax) throws ClientException {
        for (int i = iMin; i < iMax; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i,
                    "ComplexDoc");

            Map<String, Object> attachedFile = new HashMap<String, Object>();
            List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
            attachedFile.put("name", "some name");
            attachedFile.put("vignettes", vignettes);

            for (int j = 0; j < 3; j++) {
                Map<String, Object> vignette = new HashMap<String, Object>();
                vignette.put("width", Long.valueOf(j));
                vignette.put("height", Long.valueOf(j));
                vignette.put("content",
                        StreamingBlob.createFromString(String.format(
                                "document %d, vignette %d", i, j)));
                vignettes.add(vignette);
            }
            doc.setPropertyValue("cmpf:attachedFile",
                    (Serializable) attachedFile);
            doc = session.createDocument(doc);

            session.save();
            closeSession();
            openSession();
        }
    }

    protected void checkComplexDocs(int iMin, int iMax) throws ClientException,
            IOException {
        for (int i = iMin; i < iMax; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/doc" + i));

            for (int j = 0; j < 3; j++) {
                String propertyPath = String.format(
                        "cmpf:attachedFile/vignettes/%d/", j);
                assertEquals(Long.valueOf(j),
                        doc.getProperty(propertyPath + "height").getValue());
                assertEquals(Long.valueOf(j),
                        doc.getProperty(propertyPath + "width").getValue());
                assertEquals(
                        String.format("document %d, vignette %d", i, j),
                        doc.getProperty(propertyPath + "content").getValue(
                                Blob.class).getString());
            }

            closeSession();
            openSession();
        }
    }

    public void testMarkDirty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "MyDocType");
        doc = session.createDocument(doc);
        session.save();

        doc.setProperty("dublincore", "title", "title1");
        doc.setProperty("testList", "participants", new ArrayList<String>(
                Arrays.asList("a", "b")));
        session.saveDocument(doc);
        session.save();

        doc.setProperty("dublincore", "title", "title2");
        doc.setProperty("testList", "participants", new ArrayList<String>(
                Arrays.asList("c", "d")));
        session.saveDocument(doc);
        session.save();

        // ----- new session -----
        closeSession();
        openSession();
        // root = session.getRootDocument();
        doc = session.getDocument(new PathRef("/doc"));
        String title = (String) doc.getProperty("dublincore", "title");
        assertEquals("title2", title);
        Object participants = doc.getProperty("testList", "participants");
        assertEquals(Arrays.asList("c", "d"), participants);
    }

    public void testMarkDirtyForList() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");
        Map<String, Object> attachedFile = new HashMap<String, Object>();
        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        attachedFile.put("vignettes", vignettes);
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", 111L);
        vignettes.add(vignette);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        doc = session.createDocument(doc);
        session.save();

        doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/width").setValue(
                222L);
        session.saveDocument(doc);
        session.save();

        doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/width").setValue(
                333L);
        session.saveDocument(doc);
        session.save();

        // ----- new session -----
        closeSession();
        openSession();
        doc = session.getDocument(new PathRef("/doc"));
        assertEquals(
                333L,
                doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/width").getValue());
    }

    //
    //
    // ----------------------------------------------------
    // ----- copied from TestAPI in nuxeo-core-facade -----
    // ----------------------------------------------------
    //
    //

    protected final Random random = new Random(new Date().getTime());

    protected String generateUnique() {
        return String.valueOf(random.nextLong());
    }

    protected DocumentModel createChildDocument(DocumentModel childFolder)
            throws ClientException {
        DocumentModel ret = session.createDocument(childFolder);
        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());
        return ret;
    }

    protected List<DocumentModel> createChildDocuments(
            List<DocumentModel> childFolders) throws ClientException {
        List<DocumentModel> rets = new ArrayList<DocumentModel>();
        Collections.addAll(
                rets,
                session.createDocument(childFolders.toArray(new DocumentModel[0])));

        assertNotNull(rets);
        assertEquals(childFolders.size(), rets.size());

        for (DocumentModel createdChild : rets) {
            assertNotNull(createdChild);
            assertNotNull(createdChild.getName());
            assertNotNull(createdChild.getRef());
            assertNotNull(createdChild.getPathAsString());
            assertNotNull(createdChild.getId());
        }

        return rets;
    }

    public void testGetRootDocument() throws ClientException {
        DocumentModel root = session.getRootDocument();
        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getRef());
        assertNotNull(root.getPathAsString());
    }

    @SuppressWarnings({ "SimplifiableJUnitAssertion" })
    public void testDocumentReferenceEqualitySameInstance()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        assertTrue(root.getRef().equals(root.getRef()));
    }

    public void testCancel() throws ClientException {
        DocumentModel root = session.getRootDocument();

        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), "folder#" + generateUnique(), "Folder");
        childFolder = createChildDocument(childFolder);

        session.cancel();
        // TODO, cancel unimplemented
        // assertFalse(session.exists(childFolder.getRef()));
    }

    public void testCreateDomainDocumentRefDocumentModel()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "domain#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Domain");
        childFolder = createChildDocument(childFolder);

        assertEquals("Domain", childFolder.getType());
        assertEquals(name, childFolder.getName());
    }

    public void testCreateFolderDocumentRefDocumentModel()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        assertEquals("Folder", childFolder.getType());
        assertEquals(name, childFolder.getName());
    }

    public void testCreateFileDocumentRefDocumentModel() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name, "File");

        childFile = createChildDocument(childFile);

        assertEquals("File", childFile.getType());
        assertEquals(name, childFile.getName());
    }

    public void testCreateFolderDocumentRefDocumentModelArray()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "folder#" + generateUnique();
        DocumentModel childFolder2 = new DocumentModelImpl(
                root.getPathAsString(), name2, "Folder");

        List<DocumentModel> childFolders = new ArrayList<DocumentModel>();
        childFolders.add(childFolder);
        childFolders.add(childFolder2);

        List<DocumentModel> returnedChildFolders = createChildDocuments(childFolders);

        assertEquals(name, returnedChildFolders.get(0).getName());
        assertEquals(name2, returnedChildFolders.get(1).getName());
    }

    public void testCreateFileDocumentRefDocumentModelArray()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name, "File");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile2 = new DocumentModelImpl(
                root.getPathAsString(), name2, "File");

        List<DocumentModel> childFiles = new ArrayList<DocumentModel>();
        childFiles.add(childFile);
        childFiles.add(childFile2);

        List<DocumentModel> returnedChildFiles = createChildDocuments(childFiles);

        assertEquals(name, returnedChildFiles.get(0).getName());
        assertEquals(name2, returnedChildFiles.get(1).getName());
    }

    public void testExists() throws ClientException {
        DocumentModel root = session.getRootDocument();

        assertTrue(session.exists(root.getRef()));
    }

    public void testGetChild() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentModel retrievedChild = session.getChild(root.getRef(), name);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());

        assertEquals(name, retrievedChild.getName());

        retrievedChild = session.getChild(root.getRef(), name2);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());

        assertEquals(name2, retrievedChild.getName());
    }

    public void testGetChildrenDocumentRef() throws ClientException {
        DocumentModel root = session.getRootDocument();

        List<DocumentModel> docs = session.getChildren(root.getRef());

        assertEquals(0, docs.size());
    }

    public void testGetChildrenDocumentRef2() throws ClientException {
        DocumentModel root = session.getRootDocument();

        DocumentModelIterator docs = session.getChildrenIterator(root.getRef());

        assertFalse(docs.hasNext());
    }

    public void testGetFileChildrenDocumentRefString() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file childs
        List<DocumentModel> retrievedChilds = session.getChildren(
                root.getRef(), "File");

        assertNotNull(retrievedChilds);
        assertEquals(1, retrievedChilds.size());

        assertNotNull(retrievedChilds.get(0));
        assertNotNull(retrievedChilds.get(0).getId());
        assertNotNull(retrievedChilds.get(0).getName());
        assertNotNull(retrievedChilds.get(0).getPathAsString());
        assertNotNull(retrievedChilds.get(0).getRef());

        assertEquals(name2, retrievedChilds.get(0).getName());
        assertEquals("File", retrievedChilds.get(0).getType());
    }

    public void testGetFileChildrenDocumentRefString2() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file childs
        DocumentModelIterator retrievedChilds = session.getChildrenIterator(
                root.getRef(), "File");

        assertNotNull(retrievedChilds);

        assertTrue(retrievedChilds.hasNext());
        DocumentModel doc = retrievedChilds.next();
        assertFalse(retrievedChilds.hasNext());

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertNotNull(doc.getName());
        assertNotNull(doc.getPathAsString());
        assertNotNull(doc.getRef());

        assertEquals(name2, doc.getName());
        assertEquals("File", doc.getType());
    }

    public void testGetFolderChildrenDocumentRefString() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder childs
        List<DocumentModel> retrievedChilds = session.getChildren(
                root.getRef(), "Folder");

        assertNotNull(retrievedChilds);
        assertEquals(1, retrievedChilds.size());

        assertNotNull(retrievedChilds.get(0));
        assertNotNull(retrievedChilds.get(0).getId());
        assertNotNull(retrievedChilds.get(0).getName());
        assertNotNull(retrievedChilds.get(0).getPathAsString());
        assertNotNull(retrievedChilds.get(0).getRef());

        assertEquals(name, retrievedChilds.get(0).getName());
        assertEquals("Folder", retrievedChilds.get(0).getType());
    }

    public void testGetFolderChildrenDocumentRefString2()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder childs
        DocumentModelIterator retrievedChilds = session.getChildrenIterator(
                root.getRef(), "Folder");

        assertNotNull(retrievedChilds);

        assertTrue(retrievedChilds.hasNext());
        DocumentModel doc = retrievedChilds.next();
        assertFalse(retrievedChilds.hasNext());

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertNotNull(doc.getName());
        assertNotNull(doc.getPathAsString());
        assertNotNull(doc.getRef());

        assertEquals(name, doc.getName());
        assertEquals("Folder", doc.getType());
    }

    public void testGetChildrenDocumentRefStringFilter() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "folder#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "Folder");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        /*
         * Filter filter = new NameFilter(name2); // get folder children
         * List<DocumentModel> retrievedChilds =
         * session.getChildren(root.getRef(), null, null, filter, null);
         *
         * assertNotNull(retrievedChilds); assertEquals(1,
         * retrievedChilds.size());
         *
         * assertNotNull(retrievedChilds.get(0));
         * assertNotNull(retrievedChilds.get(0).getId());
         * assertNotNull(retrievedChilds.get(0).getName());
         * assertNotNull(retrievedChilds.get(0).getPathAsString());
         * assertNotNull(retrievedChilds.get(0).getRef());
         *
         * assertEquals(name2, retrievedChilds.get(0).getName());
         */
    }

    // FIXME: same as testGetChildrenDocumentRefStringFilter. Remove?
    public void testGetChildrenDocumentRefStringFilter2()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "folder#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "Folder");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        /*
         * Filter filter = new NameFilter(name2); // get folder childs
         * DocumentModelIterator retrievedChilds = session.getChildrenIterator(
         * root.getRef(), null, null, filter);
         *
         * assertNotNull(retrievedChilds);
         *
         * assertTrue(retrievedChilds.hasNext()); DocumentModel doc =
         * retrievedChilds.next(); assertFalse(retrievedChilds.hasNext());
         *
         * assertNotNull(doc); assertNotNull(doc.getId());
         * assertNotNull(doc.getName()); assertNotNull(doc.getPathAsString());
         * assertNotNull(doc.getRef());
         *
         * assertEquals(name2, doc.getName());
         */
    }

    /**
     * Test for NXP-741: Search based getChildren.
     *
     * @throws ClientException
     */
    public void testGetChildrenInFolderWithSearch() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                name, "FolderWithSearch");

        folder = createChildDocument(folder);

        // create more children
        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        for (int i = 0; i < 5; i++) {
            name = "File_" + i;
            DocumentModel childFile = new DocumentModelImpl(
                    folder.getPathAsString(), name, "File");
            childDocs.add(childFile);
        }

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        int i = 0;
        for (DocumentModel retChild : returnedChildDocs) {
            name = "File_" + i;
            assertEquals(name, retChild.getName());
            i++;
        }
    }

    public void testGetDocumentDocumentRef() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentModel doc = session.getDocument(returnedChildDocs.get(0).getRef());

        assertNotNull(doc);
        assertNotNull(doc.getRef());
        assertNotNull(doc.getName());
        assertNotNull(doc.getId());
        assertNotNull(doc.getPathAsString());

        assertEquals(name, doc.getName());
        assertEquals("Folder", doc.getType());
    }

    // TODO: fix this test.
    public void XXXtestGetDocumentDocumentRefStringArray()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        session.saveDocument(childFile);
        session.save();

        DocumentModel returnedDocument = session.getDocument(childFile.getRef());

        assertNotNull(returnedDocument);
        assertNotNull(returnedDocument.getRef());
        assertNotNull(returnedDocument.getId());
        assertNotNull(returnedDocument.getName());
        assertNotNull(returnedDocument.getPathAsString());
        assertNotNull(returnedDocument.getType());
        assertNotNull(returnedDocument.getSchemas());

        // TODO: should it contain 3 or 1 schemas? not sure about that.
        List<String> schemas = Arrays.asList(returnedDocument.getSchemas());
        assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        assertEquals("f1", returnedDocument.getProperty("dublincore", "title"));
        assertEquals("desc 1",
                returnedDocument.getProperty("dublincore", "description"));
        assertNull(returnedDocument.getProperty("file", "filename"));

        returnedDocument = session.getDocument(childFile.getRef());

        assertNotNull(returnedDocument);
        assertNotNull(returnedDocument.getRef());
        assertNotNull(returnedDocument.getId());
        assertNotNull(returnedDocument.getName());
        assertNotNull(returnedDocument.getPathAsString());
        assertNotNull(returnedDocument.getType());
        assertNotNull(returnedDocument.getSchemas());

        schemas = Arrays.asList(returnedDocument.getSchemas());
        assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        assertEquals("f1", returnedDocument.getProperty("dublincore", "title"));
        assertEquals("desc 1",
                returnedDocument.getProperty("dublincore", "description"));
        assertEquals("second name",
                returnedDocument.getProperty("file", "filename"));
    }

    public void testGetFilesDocumentRef() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file childs
        List<DocumentModel> retrievedChilds = session.getFiles(root.getRef());

        assertNotNull(retrievedChilds);
        assertEquals(1, retrievedChilds.size());

        assertNotNull(retrievedChilds.get(0));
        assertNotNull(retrievedChilds.get(0).getId());
        assertNotNull(retrievedChilds.get(0).getPathAsString());
        assertNotNull(retrievedChilds.get(0).getName());
        assertNotNull(retrievedChilds.get(0).getRef());

        assertEquals(name2, retrievedChilds.get(0).getName());
        assertEquals("File", retrievedChilds.get(0).getType());
    }

    public void testGetFilesDocumentRef2() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file childs
        DocumentModelIterator retrievedChilds = session.getFilesIterator(root.getRef());

        assertNotNull(retrievedChilds);

        assertTrue(retrievedChilds.hasNext());
        DocumentModel doc = retrievedChilds.next();
        assertFalse(retrievedChilds.hasNext());

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertNotNull(doc.getPathAsString());
        assertNotNull(doc.getName());
        assertNotNull(doc.getRef());

        assertEquals(name2, doc.getName());
        assertEquals("File", doc.getType());
    }

    // public void testGetFilesDocumentRefFilterSorter() {
    // not used at the moment
    //
    // }

    public void testGetFoldersDocumentRef() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder childs
        List<DocumentModel> retrievedChilds = session.getFolders(root.getRef());

        assertNotNull(retrievedChilds);
        assertEquals(1, retrievedChilds.size());

        assertNotNull(retrievedChilds.get(0));
        assertNotNull(retrievedChilds.get(0).getId());
        assertNotNull(retrievedChilds.get(0).getPathAsString());
        assertNotNull(retrievedChilds.get(0).getName());
        assertNotNull(retrievedChilds.get(0).getRef());

        assertEquals(name, retrievedChilds.get(0).getName());
        assertEquals("Folder", retrievedChilds.get(0).getType());
    }

    public void testGetFoldersDocumentRef2() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder childs
        DocumentModelIterator retrievedChilds = session.getFoldersIterator(root.getRef());

        assertNotNull(retrievedChilds);

        assertTrue(retrievedChilds.hasNext());
        DocumentModel doc = retrievedChilds.next();
        assertFalse(retrievedChilds.hasNext());

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertNotNull(doc.getPathAsString());
        assertNotNull(doc.getName());
        assertNotNull(doc.getRef());

        assertEquals(name, doc.getName());
        assertEquals("Folder", doc.getType());
    }

    // public void testGetFoldersDocumentRefFilterSorter() {
    // not used at the moment
    // }

    public void testGetParentDocument() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentModel shouldBeRoot = session.getParentDocument(returnedChildDocs.get(
                0).getRef());

        assertEquals(root.getPathAsString(), shouldBeRoot.getPathAsString());
    }

    public void testHasChildren() throws ClientException {
        DocumentModel root = session.getRootDocument();

        // the root document at the moment has no children
        assertFalse(session.hasChildren(root.getRef()));
    }

    public void testRemoveChildren() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        session.removeChildren(root.getRef());

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
    }

    public void testRemoveDocument() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        session.removeDocument(returnedChildDocs.get(0).getRef());

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
    }

    public void TODOtestQuery() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");

        String fname1 = "file1#" + generateUnique();
        DocumentModel childFile1 = new DocumentModelImpl(
                root.getPathAsString(), fname1, "File");
        childFile1.setProperty("dublincore", "title", "abc");

        String fname2 = "file2#" + generateUnique();
        DocumentModel childFile2 = new DocumentModelImpl(
                root.getPathAsString(), fname2, "HiddenFile");
        childFile2.setProperty("dublincore", "title", "def");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile1);
        childDocs.add(childFile2);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        returnedChildDocs.get(1).setProperty("file", "filename", "f1");
        returnedChildDocs.get(2).setProperty("file", "filename", "f2");

        session.saveDocuments(returnedChildDocs.toArray(new DocumentModel[0]));
        session.save();

        DocumentModelList list = session.query("SELECT name FROM File");
        assertEquals(1, list.size());
        DocumentModel docModel = list.get(0);
        List<String> schemas = Arrays.asList(docModel.getSchemas());
        // TODO: is it 3 or 4? (should "uid" be in the list or not?)
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        // if we select filename, the returned docModel
        // should have both schemas "file" and "common"
        list = session.query("SELECT filename FROM File");
        assertEquals(1, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getSchemas());
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all properties, the returned docModel
        // should have at least the schemas "file" and "common"
        // (it seems to also have "dublincore")
        list = session.query("SELECT * FROM File");
        assertEquals(1, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getSchemas());
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all files using the filter, we should get only one
        Filter facetFilter = new FacetFilter("HiddenInNavigation", true);
        list = session.query("SELECT * FROM HiddenFile", facetFilter);
        assertEquals(1, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getSchemas());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all documents, we get the folder and the two files
        list = session.query("SELECT * FROM Document");
        assertEquals(3, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getSchemas());
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("dublincore"));

        list = session.query("SELECT * FROM Document WHERE dc:title = 'abc'");
        assertEquals(1, list.size());

        list = session.query("SELECT * FROM Document WHERE dc:title = 'abc' OR dc:title = 'def'");
        assertEquals(2, list.size());

        session.removeDocument(returnedChildDocs.get(0).getRef());
        session.removeDocument(returnedChildDocs.get(1).getRef());
    }

    public void TODOtestQueryAfterEdit() throws ClientException, IOException {
        DocumentModel root = session.getRootDocument();

        String fname1 = "file1#" + generateUnique();
        DocumentModel childFile1 = new DocumentModelImpl(
                root.getPathAsString(), fname1, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFile1);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);
        assertEquals(1, returnedChildDocs.size());

        childFile1 = returnedChildDocs.get(0);
        childFile1.setProperty("file", "filename", "f1");

        // add a blob
        StringBlob sb = new StringBlob(
                "<html><head/><body>La la la!</body></html>");
        byte[] bytes = sb.getByteArray();
        Blob blob = new ByteArrayBlob(bytes, "text/html");
        childFile1.setProperty("file", "content", blob);

        session.saveDocument(childFile1);
        session.save();

        DocumentModelList list;
        DocumentModel docModel;

        list = session.query("SELECT * FROM Document");
        assertEquals(1, list.size());
        docModel = list.get(0);

        // read the properties
        docModel.getProperty("dublincore", "title");

        // XXX: FIXME: OG the following throws a class cast exception since the
        // get property returns an HashMap instance instead of a LazyBlob when
        // the tests are all run together:

        // LazyBlob blob2 = (LazyBlob) docModel.getProperty("file", "content");
        // assertEquals(-1, blob2.getLength());
        // assertEquals("text/html", blob2.getMimeType());
        // assertEquals(42, blob2.getByteArray().length);

        // edit the title without touching the blob
        docModel.setProperty("dublincore", "title", "edited title");
        docModel.setProperty("dublincore", "description", "edited description");
        session.saveDocument(docModel);
        session.save();

        list = session.query("SELECT * FROM Document");
        assertEquals(1, list.size());
        docModel = list.get(0);

        session.removeDocument(docModel.getRef());
    }

    public void testRemoveDocuments() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentRef[] refs = { returnedChildDocs.get(0).getRef(),
                returnedChildDocs.get(1).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));

    }

    /*
     * case where some documents are actually children of other ones from the
     * list
     */
    public void testRemoveDocumentsWithDeps() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        // careless removing this one after the folder would fail
        String name2 = "file#" + generateUnique();
        DocumentModel folderChildFile = new DocumentModelImpl(
                childFolder.getPathAsString(), name2, "File");
        // one more File object, whose path is greater than the folder's
        String name3 = "file#" + generateUnique();
        DocumentModel folderChildFile2 = new DocumentModelImpl(
                childFolder.getPathAsString(), name3, "File");
        // one more File object at the root,
        // whose path is greater than the folder's
        String name4 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name4, "File");
        // one more File object at the root, whose path is greater than the
        // folder's and with name conflict resolved by core directly, see
        // NXP-3240
        DocumentModel childFile2 = new DocumentModelImpl(
                root.getPathAsString(), name4, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(folderChildFile);
        childDocs.add(folderChildFile2);
        childDocs.add(childFile);
        childDocs.add(childFile2);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());
        assertEquals(name3, returnedChildDocs.get(2).getName());
        assertEquals(name4, returnedChildDocs.get(3).getName());
        // not the same here: conflict resolved by core session
        String name5 = returnedChildDocs.get(4).getName();
        assertNotSame(name4, name5);
        assertTrue(name5.startsWith(name4));

        DocumentRef[] refs = { returnedChildDocs.get(0).getRef(),
                returnedChildDocs.get(1).getRef(),
                returnedChildDocs.get(2).getRef(),
                returnedChildDocs.get(3).getRef(),
                returnedChildDocs.get(4).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(3).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(4).getRef()));
    }

    /*
     * Same as testRemoveDocumentWithDeps with a different given ordering of
     * documents to delete
     */
    public void testRemoveDocumentsWithDeps2() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        // careless removing this one after the folder would fail
        String name2 = "file#" + generateUnique();
        DocumentModel folderChildFile = new DocumentModelImpl(
                childFolder.getPathAsString(), name2, "File");
        // one more File object, whose path is greater than the folder's
        String name3 = "file#" + generateUnique();
        DocumentModel folderChildFile2 = new DocumentModelImpl(
                childFolder.getPathAsString(), name3, "File");
        // one more File object at the root,
        // whose path is greater than the folder's
        String name4 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name4, "File");
        // one more File object at the root, whose path is greater than the
        // folder's and with name conflict resolved by core directly, see
        // NXP-3240
        DocumentModel childFile2 = new DocumentModelImpl(
                root.getPathAsString(), name4, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(folderChildFile);
        childDocs.add(folderChildFile2);
        childDocs.add(childFile);
        childDocs.add(childFile2);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());
        assertEquals(name3, returnedChildDocs.get(2).getName());
        assertEquals(name4, returnedChildDocs.get(3).getName());
        // not the same here: conflict resolved by core session
        String name5 = returnedChildDocs.get(4).getName();
        assertNotSame(name4, name5);
        assertTrue(name5.startsWith(name4));

        // here's the different ordering
        DocumentRef[] refs = { returnedChildDocs.get(1).getRef(),
                returnedChildDocs.get(0).getRef(),
                returnedChildDocs.get(4).getRef(),
                returnedChildDocs.get(3).getRef(),
                returnedChildDocs.get(2).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(3).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(4).getRef()));
    }

    public void testSave() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFolder.getRef()));
        assertTrue(session.exists(childFile.getRef()));
    }

    public void testSaveFolder() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();

        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        childFolder.setProperty("dublincore", "title", "f1");
        childFolder.setProperty("dublincore", "description", "desc 1");

        session.saveDocument(childFolder);

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFolder.getRef()));

        assertEquals("f1", childFolder.getProperty("dublincore", "title"));
        assertEquals("desc 1",
                childFolder.getProperty("dublincore", "description"));
    }

    public void testSaveFile() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "file#" + generateUnique();

        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name, "File");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");
        childFile.setProperty("file", "filename", "filename1");

        childFile = createChildDocument(childFile);

        Property p = childFile.getProperty("/file:/filename");
        // System.out.println(p.getPath());

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFile.getRef()));

        DocumentModel retrievedFile = session.getDocument(childFile.getRef());

        assertEquals("f1", retrievedFile.getProperty("dublincore", "title"));
        assertEquals("desc 1",
                retrievedFile.getProperty("dublincore", "description"));
        assertEquals("filename1", retrievedFile.getProperty("file", "filename"));
    }

    public void testSaveDocuments() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        DocumentModel[] docs = { childFolder, childFile };

        session.saveDocuments(docs);

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFolder.getRef()));
        assertTrue(session.exists(childFile.getRef()));
    }

    public void testGetDataModel() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        session.saveDocument(childFile);

        DataModel dm = session.getDataModel(childFile.getRef(), "dublincore");

        assertNotNull(dm);
        assertNotNull(dm.getMap());
        assertNotNull(dm.getSchema());
        assertEquals("dublincore", dm.getSchema());
        assertEquals("f1", dm.getData("title"));
        assertEquals("desc 1", dm.getData("description"));

        dm = session.getDataModel(childFile.getRef(), "file");

        assertNotNull(dm);
        assertNotNull(dm.getMap());
        assertNotNull(dm.getSchema());
        assertEquals("file", dm.getSchema());
        assertEquals("second name", dm.getData("filename"));
    }

    public void testGetDataModelField() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        session.saveDocument(childFile);

        assertEquals("f1", session.getDataModelField(childFile.getRef(),
                "dublincore", "title"));
        assertEquals("desc 1", session.getDataModelField(childFile.getRef(),
                "dublincore", "description"));
        assertEquals("second name", session.getDataModelField(
                childFile.getRef(), "file", "filename"));
    }

    public void testGetDataModelFields() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        session.saveDocument(childFile);

        String[] fields = { "title", "description" };

        Object[] values = session.getDataModelFields(childFile.getRef(),
                "dublincore", fields);

        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals("f1", values[0]);
        assertEquals("desc 1", values[1]);

        String[] fields2 = { "filename" };

        values = session.getDataModelFields(childFile.getRef(), "file", fields2);

        assertNotNull(values);
        assertEquals(1, values.length);
        assertEquals("second name", values[0]);
    }

    public void testDocumentReferenceEqualityDifferentInstances()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentModel retrievedChild = session.getChild(root.getRef(), name);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());
        assertEquals(name, retrievedChild.getName());

        assertEquals(root.getRef(), retrievedChild.getParentRef());

        retrievedChild = session.getChild(root.getRef(), name2);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());

        assertEquals(name2, retrievedChild.getName());
        assertEquals(root.getRef(), retrievedChild.getParentRef());
    }

    public void testDocumentReferenceNonEqualityDifferentInstances()
            throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentModel retrievedChild = session.getChild(root.getRef(), name);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());
        assertEquals(name, retrievedChild.getName());

        assertFalse(retrievedChild.getRef().equals(root.getRef()));
        assertFalse(retrievedChild.getRef().equals(
                retrievedChild.getParentRef()));
    }

    public void testFolderFacet() throws Exception {
        DocumentModel child1 = new DocumentModelImpl("/", "file1", "File");
        DocumentModel child2 = new DocumentModelImpl("/", "fold1", "Folder");
        DocumentModel child3 = new DocumentModelImpl("/", "ws1", "Workspace");
        List<DocumentModel> returnedChildFiles = createChildDocuments(Arrays.asList(
                child1, child2, child3));
        assertFalse(returnedChildFiles.get(0).isFolder());
        assertTrue(returnedChildFiles.get(1).isFolder());
        assertTrue(returnedChildFiles.get(2).isFolder());
    }

    public void testFacetAPI() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc = session.createDocument(doc);
        session.save();
        DocumentModelList dml;

        // facet not yet present
        assertFalse(doc.hasFacet("Aged"));
        assertFalse(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        Set<String> baseFacets = new HashSet<String>(Arrays.asList(
                FacetNames.DOWNLOADABLE, FacetNames.VERSIONABLE,
                FacetNames.PUBLISHABLE, FacetNames.COMMENTABLE,
                FacetNames.HAS_RELATED_TEXT));
        assertEquals(baseFacets, doc.getFacets());
        try {
            doc.setPropertyValue("age:age", "123");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }
        dml = session.query("SELECT * FROM File WHERE ecm:mixinType = 'Aged'");
        assertEquals(0, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Aged'");
        assertEquals(1, dml.size());

        // cannot add nonexistent facet
        try {
            doc.addFacet("nosuchfacet");
            fail();
        } catch (ClientRuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("No such facet"));
        }
        assertEquals(baseFacets, doc.getFacets());
        assertFalse(doc.removeFacet("nosuchfacet"));
        assertEquals(baseFacets, doc.getFacets());

        // add facet
        assertTrue(doc.addFacet("Aged"));
        assertTrue(doc.hasFacet("Aged"));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());
        doc.setPropertyValue("age:age", "123");
        doc = session.saveDocument(doc);
        assertTrue(doc.hasFacet("Aged"));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());
        assertEquals("123", doc.getPropertyValue("age:age"));
        session.save();
        closeSession();
        openSession();
        doc = session.getDocument(doc.getRef());
        assertTrue(doc.hasFacet("Aged"));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());
        assertEquals("123", doc.getPropertyValue("age:age"));
        dml = session.query("SELECT * FROM File WHERE ecm:mixinType = 'Aged'");
        assertEquals(1, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Aged'");
        assertEquals(0, dml.size());

        // add twice
        assertFalse(doc.addFacet("Aged"));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());

        // add other facet with no schema
        assertTrue(doc.addFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertTrue(doc.hasFacet("Aged"));
        assertTrue(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertEquals(baseFacets.size() + 2, doc.getFacets().size());

        // remove first facet
        assertTrue(doc.removeFacet("Aged"));
        assertFalse(doc.hasFacet("Aged"));
        assertTrue(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());
        try {
            doc.getPropertyValue("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }
        doc = session.saveDocument(doc);
        assertFalse(doc.hasFacet("Aged"));
        assertTrue(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());
        try {
            doc.getPropertyValue("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }
        session.save();
        closeSession();
        openSession();
        doc = session.getDocument(doc.getRef());
        assertFalse(doc.hasFacet("Aged"));
        assertTrue(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());
        try {
            doc.getPropertyValue("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }
        dml = session.query("SELECT * FROM File WHERE ecm:mixinType = 'Aged'");
        assertEquals(0, dml.size());
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Aged'");
        assertEquals(1, dml.size());

        // remove twice
        assertFalse(doc.removeFacet("Aged"));
        assertEquals(baseFacets.size() + 1, doc.getFacets().size());

        // remove other facet
        assertTrue(doc.removeFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertFalse(doc.hasFacet("Aged"));
        assertFalse(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        assertEquals(baseFacets, doc.getFacets());
    }

    public void testFacetIncludedInPrimaryType() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "DocWithAge");
        doc.setPropertyValue("age:age", "123");
        doc = session.createDocument(doc);
        session.save();

        // new session
        closeSession();
        openSession();
        doc = session.getDocument(doc.getRef());
        assertEquals("123", doc.getPropertyValue("age:age"));

        // API on doc whose type has a facet
        assertEquals(Collections.singleton("Aged"), doc.getFacets());
        assertTrue(doc.hasFacet("Aged"));
        assertFalse(doc.addFacet("Aged"));
        assertFalse(doc.removeFacet("Aged"));
    }

    public void testFacetAddRemove() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc = session.createDocument(doc);
        session.save();

        // mixin not there
        try {
            doc.getPropertyValue("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }

        // add
        assertTrue(doc.addFacet("Aged"));
        doc.setPropertyValue("age:age", "123");
        session.save();

        // remove
        assertTrue(doc.removeFacet("Aged"));
        session.save();

        // mixin not there anymore
        try {
            doc.getPropertyValue("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }
    }

    // mixin on doc with same schema in primary type does no harm
    public void testFacetAddRemove2() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "DocWithAge");
        doc.setPropertyValue("age:age", "123");
        doc = session.createDocument(doc);
        session.save();

        assertFalse(doc.addFacet("Aged"));
        assertEquals("123", doc.getPropertyValue("age:age"));

        assertFalse(doc.removeFacet("Aged"));
        assertEquals("123", doc.getPropertyValue("age:age"));
    }

    public void testFacetWithSamePropertyName() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.setPropertyValue("dc:title", "bar");
        doc = session.createDocument(doc);
        session.save();

        doc.addFacet("Aged");
        doc.setPropertyValue("age:title", "gee");
        doc = session.saveDocument(doc);
        session.save();

        assertEquals("bar", doc.getPropertyValue("dc:title"));
        assertEquals("gee", doc.getPropertyValue("age:title"));

        // refetch
        doc = session.getDocument(doc.getRef());
        assertEquals("bar", doc.getPropertyValue("dc:title"));
        assertEquals("gee", doc.getPropertyValue("age:title"));
    }

    public void testFacetCopy() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "123");
        doc = session.createDocument(doc);
        session.save();

        // copy the doc
        DocumentModel copy = session.copy(doc.getRef(),
                session.getRootDocument().getRef(), "bar");
        assertTrue(copy.hasFacet("Aged"));
        assertEquals("123", copy.getPropertyValue("age:age"));
    }

    public void testFacetFulltext() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "barbar");
        doc = session.createDocument(doc);
        session.save();
        DatabaseHelper.DATABASE.sleepForFulltext();

        DocumentModelList list = session.query("SELECT * FROM File WHERE ecm:fulltext = 'barbar'");
        assertEquals(1, list.size());
    }

    public void testFacetQueryContent() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "barbar");
        doc = session.createDocument(doc);
        session.save();

        DocumentModelList list = session.query("SELECT * FROM File WHERE age:age = 'barbar'");
        assertEquals(1, list.size());
    }

    public void testLifeCycleAPI() throws ClientException {
        DocumentModel root = session.getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        childFile = createChildDocument(childFile);

        assertEquals("default", session.getLifeCyclePolicy(childFile.getRef()));

        assertEquals("project",
                session.getCurrentLifeCycleState(childFile.getRef()));

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(childFile.getRef());
        assertEquals(3, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("approve"));
        assertTrue(allowedStateTransitions.contains("obsolete"));
        assertTrue(allowedStateTransitions.contains("delete"));

        assertTrue(session.followTransition(childFile.getRef(), "approve"));
        assertEquals("approved",
                session.getCurrentLifeCycleState(childFile.getRef()));
        allowedStateTransitions = session.getAllowedStateTransitions(childFile.getRef());
        assertEquals(2, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("delete"));
        assertTrue(allowedStateTransitions.contains("backToProject"));

        session.reinitLifeCycleState(childFile.getRef());
        assertEquals("project",
                session.getCurrentLifeCycleState(childFile.getRef()));
    }

    public void testDataModelLifeCycleAPI() throws ClientException {
        DocumentModel root = session.getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        childFile = createChildDocument(childFile);

        assertEquals("default", childFile.getLifeCyclePolicy());
        assertEquals("project", childFile.getCurrentLifeCycleState());

        Collection<String> allowedStateTransitions = childFile.getAllowedStateTransitions();
        assertEquals(3, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("approve"));
        assertTrue(allowedStateTransitions.contains("obsolete"));
        assertTrue(allowedStateTransitions.contains("delete"));

        assertTrue(childFile.followTransition("obsolete"));
        assertEquals("obsolete", childFile.getCurrentLifeCycleState());
        allowedStateTransitions = childFile.getAllowedStateTransitions();
        assertEquals(2, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("delete"));
        assertTrue(allowedStateTransitions.contains("backToProject"));
    }

    public void testCopy() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");

        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(),
                "folder2", "Folder");

        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(),
                "file", "File");

        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        file = createChildDocument(file);

        session.save();

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertFalse(session.exists(new PathRef("folder2/file")));

        // copy using orig name
        DocumentModel copy1 = session.copy(file.getRef(), folder2.getRef(),
                null);

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertTrue(session.exists(new PathRef("folder2/file")));
        assertFalse(session.exists(new PathRef("folder2/fileCopy")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy1));

        // copy using another name
        DocumentModel copy2 = session.copy(file.getRef(), folder2.getRef(),
                "fileCopy");

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertTrue(session.exists(new PathRef("folder2/file")));
        assertTrue(session.exists(new PathRef("folder2/fileCopy")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy2));

        // copy again to same space
        DocumentModel copy3 = session.copy(file.getRef(), folder2.getRef(),
                null);

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertTrue(session.exists(new PathRef("folder2/file")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy3));
        assertNotSame(copy1.getName(), copy3.getName());

        // copy again again to same space
        DocumentModel copy4 = session.copy(file.getRef(), folder2.getRef(),
                null);

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertTrue(session.exists(new PathRef("folder2/file")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy4));
        assertNotSame(copy1.getName(), copy4.getName());
        assertNotSame(copy3.getName(), copy4.getName());

        // copy inplace
        DocumentModel copy5 = session.copy(file.getRef(), folder1.getRef(),
                null);

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertTrue(session.exists(new PathRef("folder2/file")));
        assertTrue(session.getChildren(folder1.getRef()).contains(copy5));
        assertNotSame(copy1.getName(), copy5.getName());

        session.cancel();
    }

    public void testCopyProxyAsDocument() throws Exception {
        // create a folder tree
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");
        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(),
                "folder2", "Folder");
        DocumentModel folder3 = new DocumentModelImpl(root.getPathAsString(),
                "folder3", "Folder");
        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(),
                "copyProxyAsDocument_test", "File");
        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        folder3 = createChildDocument(folder3);
        file = createChildDocument(file);
        session.save();

        // create a file in folder 1
        file.setProperty("dublincore", "title", "the title");
        file = session.saveDocument(file);

        // create a proxy in folder2
        DocumentModel proxy = session.publishDocument(file, folder2);
        assertTrue(proxy.isProxy());

        // copy proxy into folder3
        DocumentModel copy1 = session.copyProxyAsDocument(proxy.getRef(),
                folder3.getRef(), null);
        assertFalse(copy1.isProxy());
        assertEquals(proxy.getName(), copy1.getName());
        assertEquals(proxy.getProperty("dublincore", "title"),
                copy1.getProperty("dublincore", "title"));

        // copy proxy using another name
        DocumentModel copy2 = session.copyProxyAsDocument(proxy.getRef(),
                folder3.getRef(), "foo");
        assertFalse(copy2.isProxy());
        assertEquals("foo", copy2.getName());
        assertEquals(file.getProperty("dublincore", "title"),
                copy2.getProperty("dublincore", "title"));

        session.cancel();
    }

    public void testCopyVersionable() throws Exception {
        DocumentModel note = new DocumentModelImpl("/", "note", "Note");
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        note = session.createDocument(note);
        folder = session.createDocument(folder);
        session.save();

        assertTrue(session.exists(new PathRef("note")));
        assertTrue(session.exists(new PathRef("folder")));

        // no versions at first
        List<DocumentRef> versions = session.getVersionsRefs(note.getRef());
        assertEquals(0, versions.size());

        // version the note
        note.setProperty("dublincore", "title", "blah");
        ScopedMap context = note.getContextData();
        context.putScopedValue(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        session.saveDocument(note);
        session.save();

        // check versions
        versions = session.getVersionsRefs(note.getRef());
        assertEquals(1, versions.size());

        // copy
        DocumentModel copy = session.copy(note.getRef(), folder.getRef(), null);

        // check no versions on copy
        versions = session.getVersionsRefs(copy.getRef());
        assertEquals(0, versions.size());

        session.cancel();
    }

    public void testCopyFolderOfVersionable() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        DocumentModel note = new DocumentModelImpl("/folder", "note", "Note");
        folder = session.createDocument(folder);
        note = session.createDocument(note);
        session.save();

        assertTrue(session.exists(new PathRef("/folder")));
        assertTrue(session.exists(new PathRef("/folder/note")));

        // no versions at first
        List<DocumentRef> versions = session.getVersionsRefs(note.getRef());
        assertEquals(0, versions.size());

        // version the note
        note.setProperty("dublincore", "title", "blah");
        ScopedMap context = note.getContextData();
        context.putScopedValue(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        session.saveDocument(note);
        session.save();

        // check versions
        versions = session.getVersionsRefs(note.getRef());
        assertEquals(1, versions.size());

        // copy folder, use an all-digit name to test for xpath escaping
        DocumentModel copy = session.copy(folder.getRef(), root.getRef(), "123");

        // check no versions on copied note
        DocumentModel note2 = session.getChild(copy.getRef(), "note");
        versions = session.getVersionsRefs(note2.getRef());
        assertEquals(0, versions.size());

        session.cancel();
    }

    public void testMove() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");

        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(),
                "folder2", "Folder");

        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(),
                "file", "File");

        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        file = createChildDocument(file);

        assertTrue(session.exists(new PathRef("folder1/file")));
        assertFalse(session.exists(new PathRef("folder2/file")));
        assertFalse(session.exists(new PathRef("folder1/fileMove")));

        // move using orig name
        session.move(file.getRef(), folder2.getRef(), null);

        assertFalse(session.exists(new PathRef("folder1/file")));
        assertTrue(session.exists(new PathRef("folder2/file")));

        file = session.getChild(folder2.getRef(), "file");
        session.move(file.getRef(), folder1.getRef(), "fileMove");

        assertTrue(session.exists(new PathRef("folder1/fileMove")));

        DocumentModel file2 = new DocumentModelImpl(folder2.getPathAsString(),
                "file2", "File");
        file2 = createChildDocument(file2);
        assertTrue(session.exists(new PathRef("folder2/file2")));
        DocumentModel newFile2 = session.move(file.getRef(), folder2.getRef(),
                "file2"); // collision
        String newName = newFile2.getName();
        assertFalse("file2".equals(newName));
        assertTrue(session.exists(new PathRef("folder2/file2")));
        assertTrue(session.exists(new PathRef("folder2/" + newName)));

        // move with null dest (rename)
        DocumentModel newFile3 = session.move(file.getRef(), null, "file3");
        assertEquals("file3", newFile3.getName());
    }

    // TODO: fix this test
    public void XXXtestScalarList() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        String[] str = { "a", "b", "c" };
        childFile.setProperty("dublincore", "participants", str);
        session.saveDocument(childFile);

        childFile = session.getChild(root.getRef(), childFile.getName());

        str = (String[]) childFile.getProperty("dublincore", "participants");

        assertNotNull(str);
        List<String> list = Arrays.asList(str);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains("c"));

        // modify the array

        str = new String[] { "a", "b" };
        childFile.setProperty("dublincore", "participants", str);
        session.saveDocument(childFile);

        str = (String[]) childFile.getProperty("dublincore", "participants");

        childFile = session.getChild(root.getRef(), childFile.getName());
        str = (String[]) childFile.getProperty("dublincore", "participants");

        assertNotNull(str);
        list = Arrays.asList(str);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
    }

    public void testBlob() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        byte[] bytes = FileUtils.readBytes(Blob.class.getResourceAsStream("Blob.class"));
        Blob blob = new ByteArrayBlob(bytes, "java/class");
        blob.setDigest("XXX");
        blob.setFilename("blob.txt");
        blob.setEncoding("UTF8");
        long length = blob.getLength();
        byte[] content = blob.getByteArray();

        childFile.setProperty("file", "filename", "deprectaed filename");
        childFile.setProperty("dublincore", "title", "Blob test");
        childFile.setProperty("dublincore", "description", "this is a test");
        childFile.setProperty("file", "content", blob);

        session.saveDocument(childFile);

        childFile = session.getDocument(childFile.getRef());
        blob = (Blob) childFile.getProperty("file", "content");

        assertEquals("XXX", blob.getDigest());
        assertEquals("blob.txt", blob.getFilename());
        assertEquals(length, blob.getLength());
        assertEquals("UTF8", blob.getEncoding());
        assertEquals("java/class", blob.getMimeType());
        assertTrue(Arrays.equals(content, blob.getByteArray()));
    }

    public void testRetrieveSamePropertyInAncestors() throws ClientException {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");
        folder1 = createChildDocument(folder1);
        folder1.setProperty("dublincore", "title", "folder #1");
        assertEquals("folder #1", folder1.getProperty("dublincore", "title"));

        DocumentModel folder2 = new DocumentModelImpl(
                folder1.getPathAsString(), "folder2", "Folder");
        folder2 = createChildDocument(folder2);
        folder2.setProperty("dublincore", "title", "folder #2");
        assertEquals("folder #2", folder2.getProperty("dublincore", "title"));

        DocumentModel file = new DocumentModelImpl(folder2.getPathAsString(),
                "file", "File");
        file = createChildDocument(file);
        file.setProperty("dublincore", "title", "file ##");
        assertEquals("file ##", file.getProperty("dublincore", "title"));

        assertTrue(session.exists(new PathRef("/folder1")));
        assertTrue(session.exists(new PathRef("folder1/folder2")));
        assertTrue(session.exists(new PathRef("folder1/folder2/file")));

        // need to save them before getting properties from schemas...
        session.saveDocument(folder1);
        session.saveDocument(folder2);
        session.saveDocument(file);
        session.save();

        final DocumentRef[] ancestorRefs = session.getParentDocumentRefs(file.getRef());
        assertNotNull(ancestorRefs);
        assertEquals(3, ancestorRefs.length);
        assertEquals(folder2.getRef(), ancestorRefs[0]);
        assertEquals(folder1.getRef(), ancestorRefs[1]);
        assertEquals(root.getRef(), ancestorRefs[2]);

        final Object[] fieldValues = session.getDataModelsField(ancestorRefs,
                "dublincore", "title");
        assertNotNull(fieldValues);
        assertEquals(3, fieldValues.length);
        assertEquals("folder #2", fieldValues[0]);
        assertEquals("folder #1", fieldValues[1]);

        final Object[] fieldValuesBis = session.getDataModelsFieldUp(
                file.getRef(), "dublincore", "title");
        assertNotNull(fieldValuesBis);
        assertEquals(4, fieldValuesBis.length);
        assertEquals("file ##", fieldValuesBis[0]);
        assertEquals("folder #2", fieldValuesBis[1]);
        assertEquals("folder #1", fieldValuesBis[2]);
    }

    // TODO: fix and reenable.
    public void XXXtestDocumentAdapter() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "file", "File");

        file = createChildDocument(file);

        /*
         * AnnotatedDocument adoc = file.getAdapter(AnnotatedDocument.class);
         * assertNotNull(adoc); adoc.putAnnotation("key1", "val1");
         * adoc.putAnnotation("key2", "val2"); assertEquals("val1",
         * adoc.getAnnotation("key1")); assertEquals("val2",
         * adoc.getAnnotation("key2"));
         *
         * adoc = file.getAdapter(AnnotatedDocument.class); assertEquals("val1",
         * adoc.getAnnotation("key1")); assertEquals("val2",
         * adoc.getAnnotation("key2"));
         */
    }

    public void testGetSourceId() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        // Same identifier here since no version yet.
        String sourceId = childFile.getSourceId();
        assertNotNull(sourceId);
        assertEquals(childFile.getId(), sourceId);

        session.save();
        session.checkIn(childFile.getRef(), null, null);

        // Different source ids now.
        assertNotNull(childFile.getSourceId());
        assertEquals(sourceId, childFile.getSourceId());
        // TODO: look at this test.
        // assertFalse(childFile.getId().equals(childFile.getSourceId()));
    }

    public void testGetRepositoryName() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);
        assertNotNull(childFile.getRepositoryName());
        assertEquals("test", childFile.getRepositoryName());
    }

    // TODO: fix and reenable, is this a bug?
    public void XXXtestRetrieveProxies() throws ClientException {
        DocumentModel root = session.getRootDocument();

        // Section A
        String name = "section" + generateUnique();
        DocumentModel sectionA = new DocumentModelImpl(root.getPathAsString(),
                name, "Section");
        sectionA = createChildDocument(sectionA);

        assertEquals("Section", sectionA.getType());
        assertEquals(name, sectionA.getName());

        // Section B
        name = "section" + generateUnique();
        DocumentModel sectionB = new DocumentModelImpl(root.getPathAsString(),
                name, "Section");
        sectionB = createChildDocument(sectionB);

        assertEquals("Section", sectionB.getType());
        assertEquals(name, sectionB.getName());

        // File
        name = "file" + generateUnique();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                name, "File");
        file = createChildDocument(file);

        assertEquals("File", file.getType());
        assertEquals(name, file.getName());

        // Versioning
        // session.saveDocumentAsNewVersion(file);

        // Publishing
        session.publishDocument(file, sectionA);
        // session.publishDocument(file, sectionB);

        // Retrieving proxies
        DocumentModelList proxies = session.getProxies(file.getRef(),
                sectionA.getRef());

        assertFalse(proxies.isEmpty());
        assertEquals(1, proxies.size());
        // assertEquals(2, proxies.size());
    }

    public void testCreateDocumentModel() throws ClientException {
        // first method: only the typename
        DocumentModel docModel = session.createDocumentModel("File");
        assertEquals("File", docModel.getType());

        // bad type should fail with ClientException
        try {
            session.createDocumentModel("NotAValidTypeName");
            fail();
        } catch (ClientException e) {
        }

        // same as previously with path info
        docModel = session.createDocumentModel("/path/to/parent", "some-id",
                "File");
        assertEquals("File", docModel.getType());
        assertEquals("/path/to/parent/some-id", docModel.getPathAsString());

        // providing additional contextual data to feed a core event listener
        // with
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("Meteo", "Today is a beautiful day");
        docModel = session.createDocumentModel("File", context);
        assertEquals("File", docModel.getType());
    }

    @SuppressWarnings({ "unchecked" })
    public void testCopyContent() throws ClientException {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "original", "File");
        doc.setProperty("dublincore", "title", "t");
        doc.setProperty("dublincore", "description", "d");
        doc.setProperty("dublincore", "subjects", new String[] { "a", "b" });
        doc.setProperty("file", "filename", "f");
        List<Object> files = new ArrayList<Object>(2);
        Map<String, Object> f = new HashMap<String, Object>();
        f.put("filename", "f1");
        files.add(f);
        f = new HashMap<String, Object>();
        f.put("filename", "f2");
        f.put("file", new StringBlob("myfile", "text/test", "UTF-8"));
        files.add(f);
        doc.setProperty("files", "files", files);
        doc = session.createDocument(doc);
        session.save();

        DocumentModel copy = new DocumentModelImpl(root.getPathAsString(),
                "copy", "File");
        copy.copyContent(doc);
        copy = session.createDocument(copy);
        session.save();

        assertEquals("t", copy.getProperty("dublincore", "title"));
        assertEquals("d", copy.getProperty("dublincore", "description"));
        assertEquals(Arrays.asList("a", "b"),
                Arrays.asList((String[]) copy.getProperty("dublincore",
                        "subjects")));
        assertEquals("f", copy.getProperty("file", "filename"));
        Object fileso = copy.getProperty("files", "files");
        assertNotNull(fileso);
        List<Map<String, Object>> newfiles = (List<Map<String, Object>>) fileso;
        assertEquals(2, newfiles.size());
        assertEquals("f1", newfiles.get(0).get("filename"));
        assertEquals("f2", newfiles.get(1).get("filename"));
        Blob bb = (Blob) newfiles.get(1).get("file");
        assertNotNull(bb);
        assertEquals("text/test", bb.getMimeType());
        assertEquals("UTF-8", bb.getEncoding());
        String content;
        try {
            content = bb.getString();
        } catch (IOException e) {
            throw new ClientException(e);
        }
        assertEquals("myfile", content);
    }

    @SuppressWarnings("unchecked")
    public void testDocumentModelTreeSort() throws Exception {
        // create a folder tree
        DocumentModel root = session.getRootDocument();
        DocumentModel a_folder = new DocumentModelImpl(root.getPathAsString(),
                "a_folder", "Folder");
        a_folder.setProperty("dublincore", "title", "Z title for a_folder");
        DocumentModel b_folder = new DocumentModelImpl(root.getPathAsString(),
                "b_folder", "Folder");
        b_folder.setProperty("dublincore", "title", "B title for b_folder");
        DocumentModel c_folder = new DocumentModelImpl(root.getPathAsString(),
                "c_folder", "Folder");
        c_folder.setProperty("dublincore", "title", "C title for c_folder");

        DocumentModel a1_folder = new DocumentModelImpl(
                a_folder.getPathAsString(), "a1_folder", "Folder");
        a1_folder.setProperty("dublincore", "title", "ZZ title for a1_folder");
        DocumentModel a2_folder = new DocumentModelImpl(
                a_folder.getPathAsString(), "a2_folder", "Folder");
        a2_folder.setProperty("dublincore", "title", "AA title for a2_folder");

        DocumentModel b1_folder = new DocumentModelImpl(
                b_folder.getPathAsString(), "b1_folder", "Folder");
        b1_folder.setProperty("dublincore", "title", "A title for b1_folder");
        DocumentModel b2_folder = new DocumentModelImpl(
                b_folder.getPathAsString(), "b2_folder", "Folder");
        b2_folder.setProperty("dublincore", "title", "B title for b2_folder");

        a_folder = createChildDocument(a_folder);
        b_folder = createChildDocument(b_folder);
        c_folder = createChildDocument(c_folder);
        a1_folder = createChildDocument(a1_folder);
        a2_folder = createChildDocument(a2_folder);
        b1_folder = createChildDocument(b1_folder);
        b2_folder = createChildDocument(b2_folder);

        DocumentModelTreeImpl tree = new DocumentModelTreeImpl();
        tree.add(a_folder, 1);
        tree.add(a1_folder, 2);
        tree.add(a2_folder, 2);
        tree.add(b_folder, 1);
        tree.add(b1_folder, 2);
        tree.add(b2_folder, 2);
        tree.add(c_folder, 1);

        // sort using title
        DocumentModelTreeNodeComparator comp = new DocumentModelTreeNodeComparator(
                tree.getPathTitles());
        Collections.sort((ArrayList) tree, comp);

        assertEquals(b_folder, tree.get(0).getDocument());
        assertEquals(b1_folder, tree.get(1).getDocument());
        assertEquals(b2_folder, tree.get(2).getDocument());

        assertEquals(c_folder, tree.get(3).getDocument());

        assertEquals(a_folder, tree.get(4).getDocument());
        assertEquals(a2_folder, tree.get(5).getDocument());
        assertEquals(a1_folder, tree.get(6).getDocument());

        session.cancel();
    }

    // ------------------------------------
    // ----- copied from TestLocalAPI -----
    // ------------------------------------

    public void testPropertyModel() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "theDoc", "MyDocType");

        doc = session.createDocument(doc);

        DocumentPart dp = doc.getPart("myschema");
        Property p = dp.get("long");

        assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(12);
        assertEquals(new Long(12), p.getValue());
        session.saveDocument(doc);

        dp = doc.getPart("myschema");
        p = dp.get("long");
        assertFalse(p.isPhantom());
        assertEquals(new Long(12), p.getValue());
        p.setValue(null);
        assertFalse(p.isPhantom());
        assertNull(p.getValue());

        session.saveDocument(doc);

        dp = doc.getPart("myschema");
        p = dp.get("long");
        // assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(new Long(13));
        p.remove();
        assertTrue(p.isRemoved());
        assertNull(p.getValue());

        session.saveDocument(doc);

        dp = doc.getPart("myschema");
        p = dp.get("long");
        // assertTrue(p.isPhantom()); not applicable to SQL
        assertNull(p.getValue());
    }

    public void testOrdering() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(),
                "theParent", "OrderedFolder");

        parent = session.createDocument(parent);

        DocumentModel doc1 = new DocumentModelImpl(parent.getPathAsString(),
                "the1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl(parent.getPathAsString(),
                "the2", "File");
        doc2 = session.createDocument(doc2);
        session.save(); // XXX

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

    public void testPropertyXPath() throws Exception {
        DocumentModel root = session.getRootDocument();
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
    public void testComplexList() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "MyDocType");

        doc = session.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        /*
         * diff.add(new Attachment("at1", "value1").asMap()); diff.add(new
         * Attachment("at2", "value2").asMap()); doc.setProperty("testList",
         * "attachments", diff); doc = session.saveDocument(doc);
         *
         * list = (List) doc.getProperty("testList", "attachments");
         * assertNotNull(list); assertEquals(2, list.size());
         *
         * Blob blob; blob = (Blob) ((Map) list.get(0)).get("content");
         * assertEquals("value1", blob.getString()); blob = (Blob) ((Map)
         * list.get(1)).get("content"); assertEquals("value2",
         * blob.getString());
         *
         * diff = new ListDiff(); diff.remove(0); diff.insert(0, new
         * Attachment("at1.bis", "value1.bis").asMap());
         * doc.setProperty("testList", "attachments", diff); doc =
         * session.saveDocument(doc);
         *
         * list = (List) doc.getProperty("testList", "attachments");
         * assertNotNull(list); assertEquals(2, list.size());
         *
         * blob = (Blob) ((Map) list.get(0)).get("content");
         * assertEquals("value1.bis", blob.getString()); blob = (Blob) ((Map)
         * list.get(1)).get("content"); assertEquals("value2",
         * blob.getString());
         *
         * diff = new ListDiff(); diff.move(0, 1); doc.setProperty("testList",
         * "attachments", diff); doc = session.saveDocument(doc);
         *
         * list = (List) doc.getProperty("testList", "attachments");
         * assertNotNull(list); assertEquals(2, list.size()); blob = (Blob)
         * ((Map) list.get(0)).get("content"); assertEquals("value2",
         * blob.getString()); blob = (Blob) ((Map) list.get(1)).get("content");
         * assertEquals("value1.bis", blob.getString());
         *
         * diff = new ListDiff(); diff.removeAll(); doc.setProperty("testList",
         * "attachments", diff); doc = session.saveDocument(doc);
         *
         * list = (List) doc.getProperty("testList", "attachments");
         * assertNotNull(list); assertEquals(0, list.size());
         */
    }

    public void testDataModel() throws Exception {
        DocumentModel root = session.getRootDocument();
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
        /*
         * diff.add(new Attachment("at1", "value1").asMap()); diff.add(new
         * Attachment("at2", "value2").asMap()); doc.setProperty("testList",
         * "attachments", diff); doc = session.saveDocument(doc);
         *
         * dm = doc.getDataModel("testList");
         *
         * dm.setValue("attachments/item[0]/name", "at1-modif");
         * assertEquals("at1-modif", dm.getValue("attachments/item[0]/name"));
         * dm.setValue("attachments/item[0]/name", "at1-modif2");
         * assertEquals("at1-modif2", dm.getValue("attachments/item[0]/name"));
         * dm.setValue("attachments/item[1]/name", "at2-modif");
         * assertEquals("at2-modif", dm.getValue("attachments/item[1]/name"));
         * dm.setValue("attachments/item[1]/name", "at2-modif2");
         * assertEquals("at2-modif2", dm.getValue("attachments/item[1]/name"));
         */
    }

    public void testGetChildrenRefs() throws Exception {
        DocumentModel root = session.getRootDocument();
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

    public void testProxyChildren() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc1 = new DocumentModelImpl(root.getPathAsString(),
                "doc1", "Book");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl(root.getPathAsString(),
                "doc2", "Book");
        doc2 = session.createDocument(doc2);

        // create proxy pointing to doc1
        DocumentModel proxy1 = session.publishDocument(doc1, root);

        // check proxy1 children methods
        DocumentModelList children = session.getChildren(proxy1.getRef());
        assertEquals(0, children.size());
        assertFalse(session.hasChildren(proxy1.getRef()));

        // create proxy pointing to doc2, under proxy1
        DocumentModel proxy2 = session.publishDocument(doc2, proxy1);
        session.save();

        // check that sub proxy really exists
        assertEquals(proxy2, session.getDocument(proxy2.getRef()));

        // check proxy1 children methods
        children = session.getChildren(proxy1.getRef());
        assertEquals(1, children.size());
        assertEquals(proxy2, children.get(0));
        assertEquals(proxy2,
                session.getChild(proxy1.getRef(), proxy2.getName()));
        assertTrue(session.hasChildren(proxy1.getRef()));
    }

    public static byte[] createBytes(int size, byte val) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, val);
        return bytes;
    }

    // badly named
    public void testLazyBlob() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "mydoc", "File");

        doc = session.createDocument(doc);
        byte[] bytes = createBytes(1024 * 1024, (byte) 24);

        Blob blob = new ByteArrayBlob(bytes);
        doc.getPart("file").get("content").setValue(blob);
        doc = session.saveDocument(doc);

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

        // reset not implemented (not needed) for SQLBlob's Binary
        // XXX blob.getStream().reset();

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));
    }

    public void testProxy() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "the title");
        doc = session.saveDocument(doc);

        DocumentModel proxy = session.publishDocument(doc, root);
        session.save();

        // re-modify doc
        doc.setProperty("dublincore", "title", "the title modified");
        doc = session.saveDocument(doc);

        assertEquals("the title", proxy.getProperty("dublincore", "title"));
        assertEquals("the title modified",
                doc.getProperty("dublincore", "title"));

        // make another proxy
        session.publishDocument(doc, root);

        DocumentModelList list = session.getChildren(root.getRef());
        assertEquals(2, list.size());

        for (DocumentModel model : list) {
            assertEquals("File", model.getType());
        }

        session.removeDocument(proxy.getRef());
        list = session.getChildren(root.getRef());
        assertEquals(1, list.size());

        // create folder to hold proxies
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        // publishDocument API
        proxy = session.publishDocument(doc, root);
        session.save(); // needed for publish-by-copy to work
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());
        assertTrue(proxy.isProxy());
        assertFalse(proxy.isVersion());
        assertTrue(proxy.isImmutable());
        assertTrue(proxy.hasFacet(FacetNames.IMMUTABLE)); // dynamic facet
        assertTrue(proxy.hasFacet(FacetNames.VERSIONABLE)); // facet from type

        // republish a proxy
        DocumentModel proxy2 = session.publishDocument(proxy, folder);
        session.save();
        assertTrue(proxy2.isProxy());
        assertFalse(proxy2.isVersion());
        assertTrue(proxy2.isImmutable());
        assertEquals(1, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());

        // a second time to check overwrite
        session.publishDocument(proxy, folder);
        session.save();
        assertEquals(1, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());

        // and without overwrite
        session.publishDocument(proxy, folder, false);
        session.save();
        assertEquals(2, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());
    }

    public void testProxyLive() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "the title");
        doc = session.saveDocument(doc);
        session.save();

        // create live proxy
        DocumentModel proxy = session.createProxy(doc.getRef(), root.getRef());
        assertTrue(proxy.isProxy());
        assertFalse(proxy.isVersion());
        assertFalse(proxy.isImmutable());
        session.save();
        assertEquals("the title", proxy.getProperty("dublincore", "title"));
        assertEquals("the title", doc.getProperty("dublincore", "title"));

        // modify live doc
        doc.setProperty("dublincore", "title", "the title modified");
        doc = session.saveDocument(doc);
        session.save();

        // check visible from proxy
        proxy = session.getDocument(proxy.getRef());
        assertTrue(proxy.isProxy());
        assertFalse(proxy.isVersion());
        assertFalse(proxy.isImmutable());
        assertEquals("the title modified",
                proxy.getProperty("dublincore", "title"));

        // modify proxy
        proxy.setProperty("dublincore", "title", "the title again");
        doc = session.saveDocument(proxy);
        session.save();

        // check visible from live doc
        doc = session.getDocument(doc.getRef());
        assertEquals("the title again", doc.getProperty("dublincore", "title"));
    }

    public void testUpdatePublishedDocument() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "the title");
        doc = session.saveDocument(doc);

        // create folder to hold proxies
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        // publishDocument API
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();
        assertEquals(1, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals("the title", proxy.getProperty("dublincore", "title"));
        assertEquals("the title", doc.getProperty("dublincore", "title"));
        assertTrue(proxy.isProxy());
        assertFalse(proxy.isVersion());

        // republish a proxy
        DocumentModel proxy2 = session.publishDocument(doc, folder);
        session.save();
        assertTrue(proxy2.isProxy());
        assertFalse(proxy2.isVersion());
        assertEquals(1, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(proxy.getId(), proxy2.getId());
    }

    public void testImport() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder.setProperty("dublincore", "title", "the title");
        folder = session.createDocument(folder);
        session.save();
        String folderId = folder.getId();

        // create a version by import
        String id = "aaaaaaaa-1234-1234-1234-fedcba987654"; // versionable
        String vid = "12345678-1234-1234-1234-fedcba987654"; // ver id
        String typeName = "File";
        DocumentRef parentRef = null;
        String name = "foobar";
        DocumentModel ver = new DocumentModelImpl((String) null, typeName, vid,
                new Path(name), null, null, parentRef, null, null, null, null);
        Calendar vcr = new GregorianCalendar(2009, Calendar.JANUARY, 1, 2, 3, 4);
        ver.putContextData(CoreSession.IMPORT_VERSION_VERSIONABLE_ID, id);
        ver.putContextData(CoreSession.IMPORT_VERSION_CREATED, vcr);
        ver.putContextData(CoreSession.IMPORT_VERSION_LABEL, "v1");
        ver.putContextData(CoreSession.IMPORT_VERSION_DESCRIPTION, "v descr");
        ver.putContextData(CoreSession.IMPORT_IS_VERSION, Boolean.TRUE);
        ver.putContextData(CoreSession.IMPORT_VERSION_IS_LATEST, Boolean.TRUE);
        ver.putContextData(CoreSession.IMPORT_VERSION_IS_LATEST_MAJOR,
                Boolean.FALSE);
        ver.putContextData(CoreSession.IMPORT_VERSION_MAJOR, Long.valueOf(3));
        ver.putContextData(CoreSession.IMPORT_VERSION_MINOR, Long.valueOf(14));
        ver.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, "v lcp");
        ver.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, "v lcst");
        ver.setProperty("dublincore", "title", "Ver title");
        Calendar mod = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34,
                56);
        ver.setProperty("dublincore", "modified", mod);
        session.importDocuments(Collections.singletonList(ver));
        session.save();
        closeSession();
        openSession();
        ver = session.getDocument(new IdRef(vid));
        // assertEquals(name, doc.getName()); // no path -> no name...
        assertEquals("Ver title",
                (String) ver.getProperty("dublincore", "title"));
        assertEquals(mod, ver.getProperty("dublincore", "modified"));
        assertEquals("v lcp", ver.getLifeCyclePolicy());
        assertEquals("v lcst", ver.getCurrentLifeCycleState());
        assertEquals(Long.valueOf(3), ver.getProperty("uid", "major_version"));
        assertEquals(Long.valueOf(14), ver.getProperty("uid", "minor_version"));
        assertTrue(ver.isVersion());
        assertFalse(ver.isProxy());
        // lookup version by label
        VersionModel versionModel = new VersionModelImpl();
        versionModel.setLabel("v1");
        ver = session.getVersion(id, versionModel);
        assertNotNull(ver);
        assertEquals(vid, ver.getId());
        assertEquals("v descr", versionModel.getDescription());
        assertEquals(vcr, versionModel.getCreated());

        // create a proxy by import
        String pid = "00000000-1234-1234-1234-fedcba987654"; // proxy id
        typeName = CoreSession.IMPORT_PROXY_TYPE;
        parentRef = new IdRef(folderId);
        name = "myproxy";
        DocumentModel proxy = new DocumentModelImpl((String) null, typeName,
                pid, new Path(name), null, null, parentRef, null, null, null,
                null);
        proxy.putContextData(CoreSession.IMPORT_PROXY_TARGET_ID, vid);
        proxy.putContextData(CoreSession.IMPORT_PROXY_VERSIONABLE_ID, id);
        session.importDocuments(Collections.singletonList(proxy));
        session.save();
        closeSession();
        openSession();
        proxy = session.getDocument(new IdRef(pid));
        assertEquals(name, proxy.getName());
        assertEquals("Ver title",
                (String) proxy.getProperty("dublincore", "title"));
        assertEquals(mod, proxy.getProperty("dublincore", "modified"));
        assertEquals("v lcp", proxy.getLifeCyclePolicy());
        assertEquals("v lcst", proxy.getCurrentLifeCycleState());
        assertFalse(proxy.isVersion());
        assertTrue(proxy.isProxy());

        // create a normal doc by import
        typeName = "File";
        parentRef = new IdRef(folderId);
        name = "mydoc";
        DocumentModel doc = new DocumentModelImpl((String) null, typeName, id,
                new Path(name), null, null, parentRef, null, null, null, null);
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, "lcp");
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, "lcst");
        Calendar lockCreated = new GregorianCalendar(2011, Calendar.JANUARY, 1,
                5, 5, 5);
        doc.putContextData(CoreSession.IMPORT_LOCK_OWNER, "bob");
        doc.putContextData(CoreSession.IMPORT_LOCK_CREATED, lockCreated);
        doc.putContextData(CoreSession.IMPORT_CHECKED_IN, Boolean.TRUE);
        doc.putContextData(CoreSession.IMPORT_BASE_VERSION_ID, vid);
        doc.putContextData(CoreSession.IMPORT_VERSION_MAJOR, Long.valueOf(8));
        doc.putContextData(CoreSession.IMPORT_VERSION_MINOR, Long.valueOf(1));
        doc.setProperty("dublincore", "title", "Live title");
        session.importDocuments(Collections.singletonList(doc));
        session.save();
        closeSession();
        openSession();
        doc = session.getDocument(new IdRef(id));
        assertEquals(name, doc.getName());
        assertEquals("Live title",
                (String) doc.getProperty("dublincore", "title"));
        assertEquals(folderId, doc.getParentRef().toString());
        assertEquals("lcp", doc.getLifeCyclePolicy());
        assertEquals("lcst", doc.getCurrentLifeCycleState());
        assertEquals(Long.valueOf(8), doc.getProperty("uid", "major_version"));
        assertEquals(Long.valueOf(1), doc.getProperty("uid", "minor_version"));
        assertTrue(doc.isLocked());
        assertEquals("bob", doc.getLockInfo().getOwner());
        assertEquals(lockCreated, doc.getLockInfo().getCreated());
        assertFalse(doc.isVersion());
        assertFalse(doc.isProxy());
    }

    /**
     * Check that lifecycle and dc:issued can be updated on a version. (Fields
     * defined in SQLSimpleProperty.VERSION_WRITABLE_PROPS).
     */
    public void testVersionUpdatableFields() throws Exception {
        Calendar cal1 = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34,
                56);
        Calendar cal2 = new GregorianCalendar(2010, Calendar.JANUARY, 1, 0, 0,
                0);
        Calendar cal3 = new GregorianCalendar(2010, Calendar.APRIL, 11, 11, 11,
                11);

        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "doc", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "t1");
        doc.setProperty("dublincore", "issued", cal1);
        doc = session.saveDocument(doc);

        session.checkIn(doc.getRef(), null, null);
        session.checkOut(doc.getRef());
        doc.setProperty("dublincore", "title", "t2");
        doc.setProperty("dublincore", "issued", cal2);
        doc = session.saveDocument(doc);

        // get version
        DocumentModel ver = session.getLastDocumentVersion(doc.getRef());
        assertTrue(ver.isVersion());
        assertEquals("project", ver.getCurrentLifeCycleState());
        assertEquals("t1", ver.getProperty("dublincore", "title"));
        assertEquals(cal1, ver.getProperty("dublincore", "issued"));

        // change lifecycle
        ver.followTransition("approve");
        // change dc:issued
        ver.setProperty("dublincore", "issued", cal3);
        session.saveDocument(ver);
        session.save();

        closeSession();
        openSession();
        doc = session.getDocument(new PathRef("/doc"));
        ver = session.getLastDocumentVersion(doc.getRef());
        assertEquals("t1", ver.getProperty("dublincore", "title"));
        assertEquals("approved", ver.getCurrentLifeCycleState());
        assertEquals(cal3, ver.getProperty("dublincore", "issued"));
    }

    /**
     * Check that the "incrementBeforeUpdate" and "beforeDocumentModification"
     * are not fired on a DocumentModel where the {@code isImmutable()} returns
     * {@code true}.
     */
    public void testDoNotFireBeforeUpdateEventsOnVersion() throws Exception {
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-listeners-contrib.xml");

        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "doc", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "t1");
        doc = session.saveDocument(doc);

        session.checkIn(doc.getRef(), null, null);
        session.checkOut(doc.getRef());
        doc.setProperty("dublincore", "title", "t2");
        doc = session.saveDocument(doc);
        session.save();

        // Reset the listener
        DummyTestListener.EVENTS_RECEIVED.clear();
        DocumentModel versionDoc = session.getLastDocumentVersion(doc.getRef());
        versionDoc.setProperty("dublincore", "issued", new GregorianCalendar());
        session.saveDocument(versionDoc);
        session.save();

        assertTrue(DummyTestListener.EVENTS_RECEIVED.isEmpty());
    }

    @SuppressWarnings("unchecked")
    public void testInvalidationEvents() throws Exception {
        Event event;
        Boolean local;
        Set<String> set;
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-listeners-invalidations-contrib.xml");

        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "doc", "File");
        doc = session.createDocument(doc);

        DummyTestListener.EVENTS_RECEIVED.clear();
        session.save(); // should send invalidations
        assertEquals(1, DummyTestListener.EVENTS_RECEIVED.size());
        event = DummyTestListener.EVENTS_RECEIVED.get(0);
        // NXP-5808 cannot distinguish cluster invalidations
        // local = (Boolean) event.getContext().getProperty(
        // EventConstants.INVAL_LOCAL);
        // assertEquals(Boolean.TRUE, local);
        set = (Set<String>) event.getContext().getProperty(
                EventConstants.INVAL_MODIFIED_DOC_IDS);
        assertEquals(1, set.size()); // doc created seen as modified
        set = (Set<String>) event.getContext().getProperty(
                EventConstants.INVAL_MODIFIED_PARENT_IDS);
        assertEquals(2, set.size());
        // spurious doc "parent" modified, due to its complex property
        assertEquals(
                new HashSet<String>(Arrays.asList(doc.getId(), root.getId())),
                set);

        // change just one property
        doc.setProperty("dublincore", "title", "t1");
        doc = session.saveDocument(doc);
        DummyTestListener.EVENTS_RECEIVED.clear();
        session.save(); // should send invalidations
        assertEquals(1, DummyTestListener.EVENTS_RECEIVED.size());
        event = DummyTestListener.EVENTS_RECEIVED.get(0);
        // NXP-5808 cannot distinguish cluster invalidations
        // local = (Boolean) event.getContext().getProperty(
        // EventConstants.INVAL_LOCAL);
        // assertEquals(Boolean.TRUE, local);
        set = (Set<String>) event.getContext().getProperty(
                EventConstants.INVAL_MODIFIED_DOC_IDS);
        assertEquals(1, set.size());
        assertEquals(doc.getId(), set.iterator().next());
        set = (Set<String>) event.getContext().getProperty(
                EventConstants.INVAL_MODIFIED_PARENT_IDS);
        assertEquals(0, set.size());
    }

    public void testPlacelessDocument() throws Exception {
        DocumentModel doc = new DocumentModelImpl((String) null, "mydoc",
                "MyDocType");
        doc.setProperty("dublincore", "title", "The title");
        doc = session.createDocument(doc);
        assertNull(doc.getParentRef()); // placeless
        session.save();

        DocumentModel doc2 = session.createDocumentModel(null, "other",
                "MyDocType");
        doc2.setProperty("dublincore", "title", "Other");
        doc2 = session.createDocument(doc2);
        assertNull(doc2.getParentRef()); // placeless
        session.save();

        closeSession();
        // ----- new session -----
        openSession();
        doc = session.getDocument(new IdRef(doc.getId()));
        assertNull(doc.getParentRef());

        assertEquals("The title",
                (String) doc.getProperty("dublincore", "title"));
        assertNull(doc.getProperty("dublincore", "description"));

        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertNull(doc2.getParentRef());

        // remove
        session.removeDocument(doc.getRef());
        session.save();
    }

    public void testRelation() throws Exception {
        DocumentModel rel = session.createDocumentModel(null, "myrel",
                "Relation");
        rel.setProperty("relation", "source", "1234");
        rel.setProperty("dublincore", "title", "My Rel");
        rel = session.createDocument(rel);
        assertNull(rel.getParentRef()); // placeless
        session.save();

        // query
        String query = "SELECT * FROM Relation WHERE relation:source = '1234'";
        DocumentModelList list = session.query(query);
        assertEquals(1, list.size());

        DocumentModel doc = list.get(0);
        assertNull(doc.getParentRef());
        assertEquals("My Rel", doc.getProperty("dublincore", "title"));

        // remove
        session.removeDocument(rel.getRef());
        session.save();
        list = session.query(query);
        assertEquals(0, list.size());
    }

    /**
     * Checks that a before document modification event can change the
     * DocumentModel name and provoke a rename. And that PREVIOUS_DOCUMENT_MODEL
     * received by the event holds the correct info.
     */
    public void testBeforeModificationListenerRename() throws Exception {
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-listener-beforemod-contrib.xml");

        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setProperty("dublincore", "title", "t1");
        doc = session.createDocument(doc);
        session.save();
        assertEquals("t1-rename", doc.getName());

        doc.setProperty("dublincore", "title", "t2");
        DummyBeforeModificationListener.previousTitle = null;
        doc = session.saveDocument(doc);
        session.save();
        assertEquals("t2-rename", doc.getName());
        assertEquals("/t2-rename", doc.getPathAsString());
        assertEquals("t1", DummyBeforeModificationListener.previousTitle);
    }

    public void testObsoleteType() throws Throwable {
        DocumentRef rootRef = session.getRootDocument().getRef();
        DocumentModel doc = session.createDocumentModel("/", "doc", "MyDocType");
        doc = session.createDocument(doc);
        DocumentRef docRef = new IdRef(doc.getId());
        session.save();
        assertEquals(1, session.getChildren(rootRef).size());
        assertNotNull(session.getDocument(docRef));
        assertNotNull(session.getChild(rootRef, "doc"));
        closeSession();
        openSession();

        // remove MyDocType from known types
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        schemaManager.unregisterDocumentType("MyDocType");

        assertEquals(0, session.getChildren(rootRef).size());
        try {
            session.getDocument(docRef);
            fail("shouldn't be able to get doc with obsolete type");
        } catch (ClientException e) {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Failed to get document"));
        }
        try {
            session.getChild(rootRef, "doc");
            fail("shouldn't be able to get doc with obsolete type");
        } catch (ClientException e) {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Failed to get child doc"));
        }
    }

}
