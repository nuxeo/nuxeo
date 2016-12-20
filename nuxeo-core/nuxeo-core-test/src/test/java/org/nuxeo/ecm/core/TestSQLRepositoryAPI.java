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
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
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

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.LockException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyBeforeModificationListener;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyTestListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule.IgnoreWindows;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins", //
})
@LocalDeploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml",
        "org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml" })
public class TestSQLRepositoryAPI {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected RepositoryService repositoryService;

    @After
    public void clearDummyTestListener() {
        DummyTestListener.clear();
    }

    protected CoreSession openSessionAs(String username) {
        return CoreInstance.openCoreSession(session.getRepositoryName(), username);
    }

    protected CoreSession openSessionAs(NuxeoPrincipal principal) {
        return CoreInstance.openCoreSession(session.getRepositoryName(), principal);
    }

    protected void reopenSession() {
        session = coreFeature.reopenCoreSession();
    }

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected boolean isDBS() {
        return coreFeature.getStorageConfiguration().isDBS();
    }

    /**
     * Sleep 1s, useful for stupid databases (like MySQL) that don't have subsecond resolution in TIMESTAMP fields.
     */
    protected void maybeSleepToNextSecond() {
        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();
    }

    /**
     * @since 8.4
     */
    @Test
    public void testIsVersionWritable() {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc = session.createDocument(doc);
        session.save();

        assertTrue(doc.addFacet("Aged"));
        assertTrue(doc.hasFacet("Aged"));
        doc = session.saveDocument(doc);

        DocumentRef refVersion = doc.checkIn(VersioningOption.MAJOR, "blbabla");

        DocumentModel version = session.getDocument(refVersion);

        version.setPropertyValue("age:age", "123");
        session.saveDocument(version);
    }

    @Test
    public void testBasics() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel child = new DocumentModelImpl("/", "domain", "MyDocType");
        child = session.createDocument(child);
        session.save();

        child.setProperty("dublincore", "title", "The title");
        // use local tz
        Calendar cal = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34, 56);
        child.setProperty("dublincore", "modified", cal);
        session.saveDocument(child);
        session.save();

        // ----- new session -----
        reopenSession();
        // root = session.getRootDocument();
        child = session.getChild(root.getRef(), "domain");

        String title = (String) child.getProperty("dublincore", "title");
        assertEquals("The title", title);
        String description = (String) child.getProperty("dublincore", "description");
        assertNull(description);
        Calendar modified = (Calendar) child.getProperty("dublincore", "modified");
        assertEquals(cal, modified);
    }

    @Test
    public void testLists() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel child = new DocumentModelImpl("/", "domain", "MyDocType");
        child = session.createDocument(child);
        session.save();

        // simple list as array
        child.setProperty("dublincore", "subjects", new String[] { "a", "b" });
        // simple list as List
        child.setProperty("dublincore", "contributors", new ArrayList<>(Arrays.asList("c", "d")));
        // simple list as non-serializable array
        child.setProperty("testList", "strings", new Object[] { "e", "f" });
        // complex list as List
        child.setProperty("testList", "participants", new ArrayList<>(Arrays.asList("c", "d")));
        session.saveDocument(child);
        session.save();

        child = session.getDocument(child.getRef());
        Object s = child.getProperty("testList", "strings");
        assertTrue(s.getClass().isArray());
        // Objet[] has been normalized to String[] when re-fetched from mapper cache
        assertEquals(String.class, s.getClass().getComponentType());

        // ----- new session -----
        reopenSession();
        root = session.getRootDocument();
        child = session.getChild(root.getRef(), "domain");

        Object subjects = child.getProperty("dublincore", "subjects");
        assertTrue(subjects instanceof String[]);
        assertEquals(Arrays.asList("a", "b"), Arrays.asList((String[]) subjects));
        Object contributors = child.getProperty("dublincore", "contributors");
        assertTrue(contributors instanceof String[]);
        assertEquals(Arrays.asList("c", "d"), Arrays.asList((String[]) contributors));
        Object strings = child.getProperty("testList", "strings");
        assertTrue(strings.getClass().isArray());
        // Objet[] has been normalized to String[] when re-read from database
        assertEquals(String.class, strings.getClass().getComponentType());
        assertEquals(Arrays.asList("e", "f"), Arrays.asList((Serializable[]) strings));
        Object participants = child.getProperty("testList", "participants");
        assertTrue(participants.getClass().isArray());
        // List<> has been normalized to String[] when re-read from database
        assertEquals(String.class, participants.getClass().getComponentType());
        assertEquals(Arrays.asList("c", "d"), Arrays.asList((Serializable[]) participants));
    }

    @Test
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

    @Test
    public void testComplexType() throws Exception {
        // boiler plate to handle the asynchronous full-text indexing of blob
        // content in a deterministic way

        DocumentModel doc = new DocumentModelImpl("/", "complex-doc", "ComplexDoc");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();
        session.save();
        waitForAsyncCompletion();

        // test setting and reading a map with an empty list
        reopenSession();
        doc = session.getDocument(docRef);
        Map<String, Object> attachedFile = new HashMap<>();
        List<Map<String, Object>> vignettes = new ArrayList<>();
        attachedFile.put("name", "somename");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        doc = session.getDocument(docRef);
        assertEquals(attachedFile, doc.getProperty("cmpf:attachedFile").getValue());
        assertEquals(attachedFile.get("vignettes"), doc.getProperty("cmpf:attachedFile/vignettes").getValue());

        // test setting and reading a list of maps without a complex type in the
        // maps
        Map<String, Object> vignette = new HashMap<>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        vignette.put("content", Blobs.createBlob("textblob content"));
        vignette.put("label", "vignettelabel");
        vignettes.add(vignette);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        doc = session.getDocument(docRef);
        assertEquals("text/plain",
                doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/content/mime-type").getValue());
        assertEquals(Long.valueOf(0), doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/height").getValue());
        assertEquals("vignettelabel", doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/label").getValue());

        // test setting and reading a list of maps with a blob inside the map
        Blob blob = Blobs.createBlob("01AB", "application/octet-stream", null, "file.bin");
        vignette.put("content", blob);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        session.saveDocument(doc);
        session.save();

        doc = session.getDocument(docRef);
        assertEquals(Long.valueOf(0), doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/height").getValue());
        assertEquals("vignettelabel", doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/label").getValue());

        // this doesn't work due to core restrictions (BlobProperty):
        // assertEquals(blob.getFilename(), doc.getProperty(
        // "cmpf:attachedFile/vignettes/vignette[0]/content/name").getValue());
        Blob b = (Blob) doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/content").getValue();
        assertEquals("file.bin", b.getFilename());
    }

    @Test
    public void testComplexTypeOrdering() throws Exception {
        // Oracle has problems opening and closing many connections in a short time span (Listener refused the
        // connection with the following error: ORA-12519, TNS:no appropriate service handler found).
        // It seems to have something to do with how closed sessions are not immediately accounted for by Oracle's PMON
        // (process monitor).
        // So don't run this test with Oracle.
        assumeTrue("Test does too many open/close for Oracle", !coreFeature.getStorageConfiguration().isVCSOracle());

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

    protected void createComplexDocs(int iMin, int iMax) {
        for (int i = iMin; i < iMax; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i, "ComplexDoc");

            Map<String, Object> attachedFile = new HashMap<>();
            List<Map<String, Object>> vignettes = new ArrayList<>();
            attachedFile.put("name", "some name");
            attachedFile.put("vignettes", vignettes);

            for (int j = 0; j < 3; j++) {
                Map<String, Object> vignette = new HashMap<>();
                vignette.put("width", Long.valueOf(j));
                vignette.put("height", Long.valueOf(j));
                vignette.put("content", Blobs.createBlob(String.format("document %d, vignette %d", i, j)));
                vignettes.add(vignette);
            }
            doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
            doc = session.createDocument(doc);

            session.save();
        }
    }

    protected void checkComplexDocs(int iMin, int iMax) throws IOException {
        for (int i = iMin; i < iMax; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/doc" + i));

            for (int j = 0; j < 3; j++) {
                String propertyPath = String.format("cmpf:attachedFile/vignettes/%d/", j);
                assertEquals(Long.valueOf(j), doc.getProperty(propertyPath + "height").getValue());
                assertEquals(Long.valueOf(j), doc.getProperty(propertyPath + "width").getValue());
                assertEquals(String.format("document %d, vignette %d", i, j),
                        doc.getProperty(propertyPath + "content").getValue(Blob.class).getString());
            }
        }
    }

    @Test
    public void testMarkDirty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "MyDocType");
        doc = session.createDocument(doc);
        session.save();

        doc.setProperty("dublincore", "title", "title1");
        doc.setProperty("testList", "participants", new ArrayList<>(Arrays.asList("a", "b")));
        session.saveDocument(doc);
        session.save();

        doc.setProperty("dublincore", "title", "title2");
        doc.setProperty("testList", "participants", new ArrayList<>(Arrays.asList("c", "d")));
        session.saveDocument(doc);
        session.save();

        // ----- new session -----
        reopenSession();
        // root = session.getRootDocument();
        doc = session.getDocument(new PathRef("/doc"));
        String title = (String) doc.getProperty("dublincore", "title");
        assertEquals("title2", title);
        Object participants = doc.getProperty("testList", "participants");
        assertEquals(Arrays.asList("c", "d"), Arrays.asList((Serializable[]) participants));
    }

    @Test
    public void testMarkDirtyForList() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");
        Map<String, Object> attachedFile = new HashMap<>();
        List<Map<String, Object>> vignettes = new ArrayList<>();
        attachedFile.put("vignettes", vignettes);
        Map<String, Object> vignette = new HashMap<>();
        vignette.put("width", 111L);
        vignettes.add(vignette);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);
        doc = session.createDocument(doc);
        session.save();

        doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/width").setValue(222L);
        session.saveDocument(doc);
        session.save();

        doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/width").setValue(333L);
        session.saveDocument(doc);
        session.save();

        // ----- new session -----
        reopenSession();
        doc = session.getDocument(new PathRef("/doc"));
        assertEquals(333L, doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/width").getValue());
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

    protected DocumentModel createChildDocument(DocumentModel childFolder) {
        DocumentModel ret = session.createDocument(childFolder);
        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());
        return ret;
    }

    protected List<DocumentModel> createChildDocuments(List<DocumentModel> childFolders) {
        List<DocumentModel> rets = new ArrayList<>();
        Collections.addAll(rets, session.createDocument(childFolders.toArray(new DocumentModel[0])));

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

    @Test
    public void testGetRootDocument() {
        DocumentModel root = session.getRootDocument();
        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getRef());
        assertNotNull(root.getPathAsString());
    }

    @SuppressWarnings({ "SimplifiableJUnitAssertion" })
    @Test
    public void testDocumentReferenceEqualitySameInstance() {
        DocumentModel root = session.getRootDocument();

        assertTrue(root.getRef().equals(root.getRef()));
    }

    @Test
    public void testCancel() {
        DocumentModel root = session.getRootDocument();

        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), "folder#" + generateUnique(),
                "Folder");
        childFolder = createChildDocument(childFolder);

        session.cancel();
        // TODO NXP-2514, cancel unimplemented
        // assertFalse(session.exists(childFolder.getRef()));
    }

    @Test
    public void testCreateDomainDocumentRefDocumentModel() {
        DocumentModel root = session.getRootDocument();

        String name = "domain#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Domain");
        childFolder = createChildDocument(childFolder);

        assertEquals("Domain", childFolder.getType());
        assertEquals(name, childFolder.getName());
    }

    @Test
    public void testCreateFolderDocumentRefDocumentModel() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        assertEquals("Folder", childFolder.getType());
        assertEquals(name, childFolder.getName());
    }

    @Test
    public void testCreateFileDocumentRefDocumentModel() {
        DocumentModel root = session.getRootDocument();

        String name = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name, "File");

        childFile = createChildDocument(childFile);

        assertEquals("File", childFile.getType());
        assertEquals(name, childFile.getName());
    }

    @Test
    public void testCreateFolderDocumentRefDocumentModelArray() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "folder#" + generateUnique();
        DocumentModel childFolder2 = new DocumentModelImpl(root.getPathAsString(), name2, "Folder");

        List<DocumentModel> childFolders = new ArrayList<>();
        childFolders.add(childFolder);
        childFolders.add(childFolder2);

        List<DocumentModel> returnedChildFolders = createChildDocuments(childFolders);

        assertEquals(name, returnedChildFolders.get(0).getName());
        assertEquals(name2, returnedChildFolders.get(1).getName());
    }

    @Test
    public void testCreateFileDocumentRefDocumentModelArray() {
        DocumentModel root = session.getRootDocument();

        String name = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name, "File");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile2 = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childFiles = new ArrayList<>();
        childFiles.add(childFile);
        childFiles.add(childFile2);

        List<DocumentModel> returnedChildFiles = createChildDocuments(childFiles);

        assertEquals(name, returnedChildFiles.get(0).getName());
        assertEquals(name2, returnedChildFiles.get(1).getName());
    }

    @Test
    public void testExists() {
        DocumentModel root = session.getRootDocument();

        assertTrue(session.exists(root.getRef()));
    }

    @Test
    public void testGetChild() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        assertFalse(session.hasChild(root.getRef(), name));
        assertFalse(session.hasChild(root.getRef(), name2));

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        assertTrue(session.hasChild(root.getRef(), name));
        assertTrue(session.hasChild(root.getRef(), name2));

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

        try {
            session.getChild(root.getRef(), "nosuchchild");
            fail("Should fail with DocumentNotFoundException");
        } catch (DocumentNotFoundException e) {
            assertEquals("nosuchchild", e.getMessage());
        }
    }

    @Test
    public void testGetChildrenDocumentRef() {
        DocumentModel root = session.getRootDocument();

        List<DocumentModel> docs = session.getChildren(root.getRef());

        assertEquals(0, docs.size());
    }

    @Test
    public void testGetChildrenIteratorRoot() {
        DocumentModel root = session.getRootDocument();

        DocumentModelIterator docs = session.getChildrenIterator(root.getRef());

        assertFalse(docs.hasNext());
    }

    @Test
    public void testGetFileChildrenDocumentRefString() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file children
        List<DocumentModel> retrievedChilds = session.getChildren(root.getRef(), "File");

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

    @Test
    public void testGetChildrenIteratorFile() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file children
        DocumentModelIterator retrievedChilds = session.getChildrenIterator(root.getRef(), "File", null, null);

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

    @Test
    public void testGetChildrenIterator() {
        int n = 200;
        Set<String> names = new HashSet<>();
        for (int i = 0; i < n; i++) {
            String name = "doc" + i;
            names.add(name);
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc = session.createDocument(doc);
        }
        session.save();

        DocumentModelIterator it = session.getChildrenIterator(new PathRef("/"), "File", null, null);
        for (DocumentModel doc : it) {
            String name = doc.getName();
            if (!names.remove(name)) {
                fail("Unknown or duplicate name" + name);
            }
        }
        if (!names.isEmpty()) {
            fail("Remaining names " + names);
        }
    }

    @Test
    public void testGetChildrenIteratorFilter() {
        int n = 200;
        Set<String> names = new HashSet<>();
        for (int i = 0; i < n; i++) {
            String name = "doc" + i;
            names.add(name);
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc = session.createDocument(doc);
            // create spurious docs in the middle
            DocumentModel doc2 = session.createDocumentModel("/", "foo" + i, "File");
            doc2 = session.createDocument(doc2);
            DocumentModel doc3 = session.createDocumentModel("/", "docNote" + i, "Note");
            doc3 = session.createDocument(doc3);
        }
        session.save();

        Filter filter = new Filter() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean accept(DocumentModel docModel) {
                return docModel.getName().startsWith("doc");
            }
        };
        DocumentModelIterator it = session.getChildrenIterator(new PathRef("/"), "File", null, filter);
        for (DocumentModel doc : it) {
            String name = doc.getName();
            if (!names.remove(name)) {
                fail("Unknown or duplicate name " + name);
            }
        }
        if (!names.isEmpty()) {
            fail("Remaining names " + names);
        }
    }

    @Test
    public void testGetFolderChildrenDocumentRefString() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder children
        List<DocumentModel> retrievedChilds = session.getChildren(root.getRef(), "Folder");

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

    @Test
    public void testGetChildrenIteratorFolder() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder children
        DocumentModelIterator retrievedChilds = session.getChildrenIterator(root.getRef(), "Folder", null, null);

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

    @Test
    public void testGetChildrenIteratorRestrictedAccess() {
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2 = session.createDocument(doc2);
        DocumentModel doc3 = session.createDocumentModel("/", "doc3", "File");
        doc3 = session.createDocument(doc3);

        // set ACP on root
        ACP acp;
        ACL acl;
        acp = new ACPImpl();
        acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE("bob", "Everything", true));
        acp.addACL(acl);
        session.getRootDocument().setACP(acp, true);

        // set ACP on doc2 to block bob
        acp = new ACPImpl();
        acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(ACE.BLOCK);
        acp.addACL(acl);
        doc2.setACP(acp, true);
        session.save();

        // system sees all
        Set<String> names = new HashSet<>();
        for (DocumentModel doc : session.getChildrenIterator(new PathRef("/"))) {
            names.add(doc.getName());
        }
        assertEquals(new HashSet<>(Arrays.asList("doc1", "doc2", "doc3")), names);

        // bob doesn't see doc2
        try (CoreSession bobSession = openSessionAs("bob")) {
            names.clear();
            for (DocumentModel doc : bobSession.getChildrenIterator(new PathRef("/"))) {
                names.add(doc.getName());
            }
            // doc2 is not in the returned set
            assertEquals(new HashSet<>(Arrays.asList("doc1", "doc3")), names);
        }
    }

    @Test
    public void testGetChildrenDocumentRefStringFilter() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "folder#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "Folder");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        /*
         * Filter filter = new NameFilter(name2); // get folder children List<DocumentModel> retrievedChilds =
         * session.getChildren(root.getRef(), null, null, filter, null); assertNotNull(retrievedChilds); assertEquals(1,
         * retrievedChilds.size()); assertNotNull(retrievedChilds.get(0));
         * assertNotNull(retrievedChilds.get(0).getId()); assertNotNull(retrievedChilds.get(0).getName());
         * assertNotNull(retrievedChilds.get(0).getPathAsString()); assertNotNull(retrievedChilds.get(0).getRef());
         * assertEquals(name2, retrievedChilds.get(0).getName());
         */
    }

    /**
     * Test for NXP-741: Search based getChildren.
     */
    @Test
    public void testGetChildrenInFolderWithSearch() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(), name, "FolderWithSearch");

        folder = createChildDocument(folder);

        // create more children
        List<DocumentModel> childDocs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            name = "File_" + i;
            DocumentModel childFile = new DocumentModelImpl(folder.getPathAsString(), name, "File");
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

    @Test
    public void testGetDocumentDocumentRef() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
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

    // TODO NXP-2514: fix this test.
    @Test
    @Ignore
    public void testGetDocumentDocumentRefStringArray() {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

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

        // TODO NXP-2514: should it contain 3 or 1 schemas? not sure about that.
        List<String> schemas = Arrays.asList(returnedDocument.getSchemas());
        assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        assertEquals("f1", returnedDocument.getProperty("dublincore", "title"));
        assertEquals("desc 1", returnedDocument.getProperty("dublincore", "description"));

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
        assertEquals("desc 1", returnedDocument.getProperty("dublincore", "description"));
    }

    @Test
    public void testGetFilesDocumentRef() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get file children
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

    @Test
    @Ignore
    public void testGetFilesDocumentRefFilterSorter() {
        // not used at the moment
    }

    @Test
    public void testGetFoldersDocumentRef() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        // get folder children
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

    @Test
    public void testGetFoldersDocumentRefFilterSorter() {
        DocumentModel root = session.getRootDocument();
        // init children
        DocumentModel childFolder1 = new DocumentModelImpl(root.getPathAsString(), "folder1#" + generateUnique(),
                "Folder");
        DocumentModel childFolder2 = new DocumentModelImpl(root.getPathAsString(), "folder2#" + generateUnique(),
                "Folder");
        DocumentModel childFolder3 = new DocumentModelImpl(root.getPathAsString(), "folder3#" + generateUnique(),
                "OrderedFolder");

        // persist
        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder1);
        childDocs.add(childFolder2);
        childDocs.add(childFolder3);
        createChildDocuments(childDocs);

        // test no filter, no sorter
        List<DocumentModel> folders = session.getFolders(root.getRef(), null, null);
        assertNotNull(folders);
        assertEquals(childDocs.size(), folders.size());

        // test with filter, no sorter
        Filter filter = new FacetFilter(FacetNames.ORDERABLE, true);
        folders = session.getFolders(root.getRef(), filter, null);
        assertNotNull(folders);
        assertEquals(1, folders.size());
        assertEquals(childDocs.get(childDocs.indexOf(childFolder3)).getName(), folders.get(0).getName());
    }

    @Test
    public void testGetParentDocument() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentModel shouldBeRoot = session.getParentDocument(returnedChildDocs.get(0).getRef());

        assertEquals(root.getPathAsString(), shouldBeRoot.getPathAsString());
    }

    @Test
    public void testGetParentDocuments() {
        List<DocumentModel> docs;

        DocumentModel folder1 = new DocumentModelImpl("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel folder2 = new DocumentModelImpl("/folder1", "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel file1 = new DocumentModelImpl("/folder1/folder2", "file1", "File");
        file1 = session.createDocument(file1);
        session.save();
        docs = session.getParentDocuments(file1.getRef());
        assertEquals(3, docs.size());
        assertEquals("folder1", docs.get(0).getName());
        assertEquals("folder2", docs.get(1).getName());
        assertEquals("file1", docs.get(2).getName());

        // root

        docs = session.getParentDocuments(session.getRootDocument().getRef());
        assertEquals(0, docs.size());

        // relation, check as admin

        try (CoreSession admSession = openSessionAs(new UserPrincipal("adm", null, false, true))) {
            DocumentModel rel = admSession.createDocumentModel(null, "myrel", "Relation");
            rel = admSession.createDocument(rel);
            admSession.save();
            docs = admSession.getParentDocuments(rel.getRef());
            assertEquals(1, docs.size());
            assertEquals("myrel", docs.get(0).getName());
        }
    }

    @Test
    public void testHasChildren() {
        DocumentModel root = session.getRootDocument();

        // the root document at the moment has no children
        assertFalse(session.hasChildren(root.getRef()));
    }

    @Test
    public void testRemoveChildren() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        session.removeChildren(root.getRef());

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
    }

    @Test
    public void testRemoveChildren2() {
        DocumentModel folder1 = session.createDocumentModel("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel folder2 = session.createDocumentModel("/folder1", "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel doc1 = session.createDocumentModel("/folder1/folder2", "doc1", "File");
        doc1 = session.createDocument(doc1);
        session.save();
        // now remove
        session.removeDocument(folder2.getRef());
        // don't save
        // check that we don't get phantom docs when getting children
        // for instance when updating ACLs recursively
        session.setACP(folder1.getRef(), new ACPImpl(), true);
        session.save();
    }

    protected void dumpAllDocuments(CoreSession session) {
        DocumentModelList docs = session.query("select * from Document");
        System.out.println("List all documents");
        for (DocumentModel doc : docs) {
            System.out.println(String.format("- type: %s, proxy: %s, version: %s %s", doc.getType(), doc.isProxy(),
                    doc.isVersion(), doc));
        }
    }

    @Test
    @Ignore("NXP-14686")
    public void testRemoveChildrenWithVersion() {
        DocumentModel root = session.getRootDocument();

        DocumentModel doc = session.createDocumentModel(root.getPathAsString(), "somefile", "File");
        session.createDocument(doc);

        DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "somefolder", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = session.getDocument(doc.getRef());
        session.publishDocument(file, folder);
        session.save();
        // TransactionHelper.commitOrRollbackTransaction();
        // TransactionHelper.startTransaction();

        dumpAllDocuments(session);

        session.removeChildren(root.getRef());
        session.save();
        // TransactionHelper.commitOrRollbackTransaction();
        // TransactionHelper.startTransaction();
        dumpAllDocuments(session);

        assertTrue("NXP-14686 removeChildren fails to delete orphan version",
                session.query("SELECT * FROM Document").isEmpty());
    }

    @Test
    public void testRemoveDocument() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        session.removeDocument(returnedChildDocs.get(0).getRef());

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
    }

    @Test
    public void testRemoveVersion() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        // create version
        DocumentRef version = doc.checkIn(VersioningOption.MAJOR, null);
        // create proxy
        DocumentModel proxy = session.createProxy(version, new PathRef("/"));

        // cannot remove version due to existing proxy
        try {
            session.removeDocument(version);
            fail();
        } catch (DocumentSecurityException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("targets version"));
        }

        // remove proxy
        session.removeDocument(proxy.getRef());
        // create version 2
        doc.checkOut();
        doc.checkIn(VersioningOption.MAJOR, null);
        // restore doc to version 1
        session.restoreToVersion(doc.getRef(), version);

        // cannot remove version due to live doc having it as base version
        try {
            session.removeDocument(version);
            fail();
        } catch (DocumentSecurityException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("is checked in with base version"));
        }

        // checkout live doc
        doc.checkOut();
        // now we can remove the version
        session.removeDocument(version);
    }

    public void TODOtestQuery() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");

        String fname1 = "file1#" + generateUnique();
        DocumentModel childFile1 = new DocumentModelImpl(root.getPathAsString(), fname1, "File");
        childFile1.setProperty("dublincore", "title", "abc");

        String fname2 = "file2#" + generateUnique();
        DocumentModel childFile2 = new DocumentModelImpl(root.getPathAsString(), fname2, "HiddenFile");
        childFile2.setProperty("dublincore", "title", "def");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile1);
        childDocs.add(childFile2);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        returnedChildDocs.get(1).setProperty("file", "content", Blobs.createBlob("blob1", "text/plain", "UTF-8", "f1"));
        returnedChildDocs.get(2).setProperty("file", "content", Blobs.createBlob("blob1", "text/plain", "UTF-8", "f2"));

        session.saveDocuments(returnedChildDocs.toArray(new DocumentModel[0]));
        session.save();

        DocumentModelList list = session.query("SELECT name FROM File");
        assertEquals(1, list.size());
        DocumentModel docModel = list.get(0);
        List<String> schemas = Arrays.asList(docModel.getSchemas());
        // TODO NXP-2514: is it 3 or 4? (should "uid" be in the list or not?)
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        // if we select filename, the returned docModel
        // should have both schemas "file" and "common"
        list = session.query("SELECT content/filename FROM File");
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

    public void TODOtestQueryAfterEdit() {
        DocumentModel root = session.getRootDocument();

        String fname1 = "file1#" + generateUnique();
        DocumentModel childFile1 = new DocumentModelImpl(root.getPathAsString(), fname1, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFile1);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);
        assertEquals(1, returnedChildDocs.size());

        childFile1 = returnedChildDocs.get(0);

        // add a blob
        Blob blob = Blobs.createBlob("<html><head/><body>La la la!</body></html>", "text/html", "UTF-8", "f1");
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

    @Test
    public void testRemoveDocuments() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
        childDocs.add(childFolder);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());

        DocumentRef[] refs = { returnedChildDocs.get(0).getRef(), returnedChildDocs.get(1).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));

    }

    /*
     * case where some documents are actually children of other ones from the list
     */
    @Test
    public void testRemoveDocumentsWithDeps() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        // careless removing this one after the folder would fail
        String name2 = "file#" + generateUnique();
        DocumentModel folderChildFile = new DocumentModelImpl(childFolder.getPathAsString(), name2, "File");
        // one more File object, whose path is greater than the folder's
        String name3 = "file#" + generateUnique();
        DocumentModel folderChildFile2 = new DocumentModelImpl(childFolder.getPathAsString(), name3, "File");
        // one more File object at the root,
        // whose path is greater than the folder's
        String name4 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name4, "File");
        // one more File object at the root, whose path is greater than the
        // folder's and with name conflict resolved by core directly, see
        // NXP-3240
        DocumentModel childFile2 = new DocumentModelImpl(root.getPathAsString(), name4, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
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

        DocumentRef[] refs = { returnedChildDocs.get(0).getRef(), returnedChildDocs.get(1).getRef(),
                returnedChildDocs.get(2).getRef(), returnedChildDocs.get(3).getRef(), returnedChildDocs.get(4).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(3).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(4).getRef()));
    }

    /*
     * Same as testRemoveDocumentWithDeps with a different given ordering of documents to delete
     */
    @Test
    public void testRemoveDocumentsWithDeps2() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        // careless removing this one after the folder would fail
        String name2 = "file#" + generateUnique();
        DocumentModel folderChildFile = new DocumentModelImpl(childFolder.getPathAsString(), name2, "File");
        // one more File object, whose path is greater than the folder's
        String name3 = "file#" + generateUnique();
        DocumentModel folderChildFile2 = new DocumentModelImpl(childFolder.getPathAsString(), name3, "File");
        // one more File object at the root,
        // whose path is greater than the folder's
        String name4 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name4, "File");
        // one more File object at the root, whose path is greater than the
        // folder's and with name conflict resolved by core directly, see
        // NXP-3240
        DocumentModel childFile2 = new DocumentModelImpl(root.getPathAsString(), name4, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
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
        DocumentRef[] refs = { returnedChildDocs.get(1).getRef(), returnedChildDocs.get(0).getRef(),
                returnedChildDocs.get(4).getRef(), returnedChildDocs.get(3).getRef(), returnedChildDocs.get(2).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(3).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(4).getRef()));
    }

    @Test
    public void testRemoveDocumentTreeWithSecurity() throws Exception {
        ACP acp;
        ACL acl;
        DocumentModelList dml;

        DocumentModel root = session.getRootDocument();
        DocumentModel f1 = session.createDocumentModel("/", "f1", "Folder");
        f1 = session.createDocument(f1);
        DocumentModel doc1 = session.createDocumentModel("/f1", "doc1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/f1", "doc2", "File");
        doc2 = session.createDocument(doc2);
        // set ACP on root
        acp = new ACPImpl();
        acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE("bob", "Everything", true));
        acp.addACL(acl);
        root.setACP(acp, true);
        // set ACP on doc1 to block bob
        acp = new ACPImpl();
        acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(ACE.BLOCK);
        acp.addACL(acl);
        doc1.setACP(acp, true);
        session.save();

        // check admin sees doc1 and doc2
        dml = session.query("SELECT * FROM Document WHERE ecm:path STARTSWITH '/f1'");
        assertEquals(2, dml.size());

        // as bob
        try (CoreSession bobSession = openSessionAs("bob")) {
            // check bob doesn't see doc1
            dml = bobSession.query("SELECT * FROM Document WHERE ecm:path STARTSWITH '/f1'");
            assertEquals(1, dml.size());

            // do copy
            bobSession.copy(f1.getRef(), root.getRef(), "f2");

            // save is mandatory to propagate read acls after a copy
            bobSession.save();

            // check bob doesn't see doc1's copy
            dml = bobSession.query("SELECT * FROM Document WHERE ecm:path STARTSWITH '/f2'");
            assertEquals(1, dml.size());
        }
    }

    @Test
    public void testSave() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        // TODO NXP-2514: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFolder.getRef()));
        assertTrue(session.exists(childFile.getRef()));
    }

    @Test
    public void testSaveFolder() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();

        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        childFolder.setProperty("dublincore", "title", "f1");
        childFolder.setProperty("dublincore", "description", "desc 1");

        session.saveDocument(childFolder);

        // TODO NXP-2514: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFolder.getRef()));

        assertEquals("f1", childFolder.getProperty("dublincore", "title"));
        assertEquals("desc 1", childFolder.getProperty("dublincore", "description"));
    }

    @Test
    public void testSaveFile() {
        DocumentModel root = session.getRootDocument();

        String name = "file#" + generateUnique();

        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name, "File");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        childFile = createChildDocument(childFile);

        Property p = childFile.getProperty("/dublincore:/description");
        // System.out.println(p.getPath());

        // TODO NXP-2514: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFile.getRef()));

        DocumentModel retrievedFile = session.getDocument(childFile.getRef());

        assertEquals("f1", retrievedFile.getProperty("dublincore", "title"));
        assertEquals("desc 1", retrievedFile.getProperty("dublincore", "description"));
    }

    @Test
    public void testSaveDocuments() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
        childFile = createChildDocument(childFile);

        DocumentModel[] docs = { childFolder, childFile };

        session.saveDocuments(docs);

        // TODO NXP-2514: this should be tested across sessions - when it can be done
        assertTrue(session.exists(childFolder.getRef()));
        assertTrue(session.exists(childFile.getRef()));
    }

    @Test
    public void testGetDataModel() {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");
        childFile.setProperty("file", "content", Blobs.createBlob("b1", "text/plain", "UTF-8", "second name"));

        session.saveDocument(childFile);

        Schema dublincore = childFile.getDocumentType().getSchema("dublincore");
        DataModel dm = session.getDataModel(childFile.getRef(), dublincore);

        assertNotNull(dm);
        assertNotNull(dm.getMap());
        assertNotNull(dm.getSchema());
        assertEquals("dublincore", dm.getSchema());
        assertEquals("f1", dm.getData("title"));
        assertEquals("desc 1", dm.getData("description"));

        Schema file = childFile.getDocumentType().getSchema("file");
        dm = session.getDataModel(childFile.getRef(), file);

        assertNotNull(dm);
        assertNotNull(dm.getMap());
        assertNotNull(dm.getSchema());
        assertEquals("file", dm.getSchema());
        assertEquals("second name", ((Blob) dm.getData("content")).getFilename());
    }

    @Test
    public void testDocumentReferenceEqualityDifferentInstances() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
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

    @Test
    public void testDocumentReferenceNonEqualityDifferentInstances() {
        DocumentModel root = session.getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(root.getPathAsString(), name, "Folder");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");

        List<DocumentModel> childDocs = new ArrayList<>();
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
        assertFalse(retrievedChild.getRef().equals(retrievedChild.getParentRef()));
    }

    @Test
    public void testFolderFacet() throws Exception {
        DocumentModel child1 = new DocumentModelImpl("/", "file1", "File");
        DocumentModel child2 = new DocumentModelImpl("/", "fold1", "Folder");
        DocumentModel child3 = new DocumentModelImpl("/", "ws1", "Workspace");
        List<DocumentModel> returnedChildFiles = createChildDocuments(Arrays.asList(child1, child2, child3));
        assertFalse(returnedChildFiles.get(0).isFolder());
        assertTrue(returnedChildFiles.get(1).isFolder());
        assertTrue(returnedChildFiles.get(2).isFolder());
    }

    @Test
    public void testFacetAPI() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc = session.createDocument(doc);
        session.save();
        DocumentModelList dml;

        // facet not yet present
        assertFalse(doc.hasFacet("Aged"));
        assertFalse(doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION));
        Set<String> baseFacets = new HashSet<>(Arrays.asList(FacetNames.DOWNLOADABLE, FacetNames.VERSIONABLE,
                FacetNames.PUBLISHABLE, FacetNames.COMMENTABLE, FacetNames.HAS_RELATED_TEXT));
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
        } catch (IllegalArgumentException e) {
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

        reopenSession();
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

        reopenSession();
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

    @Test
    public void testFacetIncludedInPrimaryType() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "DocWithAge");
        doc.setPropertyValue("age:age", "123");
        doc = session.createDocument(doc);
        session.save();

        // new session
        reopenSession();
        doc = session.getDocument(doc.getRef());
        assertEquals("123", doc.getPropertyValue("age:age"));

        // API on doc whose type has a facet
        assertEquals(Collections.singleton("Aged"), doc.getFacets());
        assertTrue(doc.hasFacet("Aged"));
        assertFalse(doc.addFacet("Aged"));
        assertFalse(doc.removeFacet("Aged"));
    }

    @Test
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
    @Test
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

    @Test
    public void testFacetAddRemoveWhenSchemaAlreadyPresent() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("hello"));
        doc = session.createDocument(doc);
        session.save();

        doc = session.getDocument(doc.getRef());
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("hello", blob.getString());

        // add WithFile facet, where file schema is already present in doc
        assertTrue(doc.addFacet("WithFile"));
        doc = session.saveDocument(doc);
        session.save();
        reopenSession();

        // check getting the blob works (complex child "file:content" not re-added)
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("hello", blob.getString());

        // now remove facet
        doc.removeFacet("WithFile");
        doc = session.saveDocument(doc);
        session.save();
        reopenSession();

        // schema still present because of primary type
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("hello", blob.getString());

        /*
         * two facets with the same schema
         */

        doc = new DocumentModelImpl("/", "bar", "Folder");
        doc.addFacet("WithFile2");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("hello"));
        doc = session.createDocument(doc);
        session.save();

        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("hello", blob.getString());

        // add WithFile facet, where file schema is already present in doc
        assertTrue(doc.addFacet("WithFile"));
        doc = session.saveDocument(doc);
        session.save();
        reopenSession();

        // check getting the blob works (complex child "file:content" not re-added)
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("hello", blob.getString());

        // now remove facet, but other facet WithFile2 is still present
        doc.removeFacet("WithFile");
        doc = session.saveDocument(doc);
        session.save();
        reopenSession();

        // schema still present because of other facet
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("hello", blob.getString());
    }

    @Test
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

    @Test
    public void testFacetCopy() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "123");
        doc = session.createDocument(doc);
        session.save();

        // copy the doc
        DocumentModel copy = session.copy(doc.getRef(), session.getRootDocument().getRef(), "bar");
        assertTrue(copy.hasFacet("Aged"));
        assertEquals("123", copy.getPropertyValue("age:age"));
    }

    @Test
    public void testFacetQueryContent() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "barbar");
        doc = session.createDocument(doc);
        session.save();

        DocumentModelList list = session.query("SELECT * FROM File WHERE age:age = 'barbar'");
        assertEquals(1, list.size());
    }

    @Test
    public void testFacetRefresh() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "foo", "File");
        doc = session.createDocument(doc);
        assertFalse(doc.hasFacet("Aged"));

        // fetch another DocumentModel instance of the same doc
        DocumentModel doc2 = session.getDocument(doc.getRef());
        // facet not yet present
        assertFalse(doc2.hasFacet("Aged"));
        // cannot write property
        try {
            doc2.setPropertyValue("age:age", "123");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }

        // add facet in first doc
        assertTrue(doc.addFacet("Aged"));
        assertTrue(doc.hasFacet("Aged"));
        doc.setPropertyValue("age:age", "123");
        doc = session.saveDocument(doc);

        // after refresh should be also visible in second doc
        doc2.refresh();
        assertTrue(doc2.hasFacet("Aged"));
        assertEquals("123", doc2.getPropertyValue("age:age"));

        // change value in first doc
        doc.setPropertyValue("age:age", "456");
        doc = session.saveDocument(doc);

        // after refresh should be also visible in second doc
        doc2.refresh();
        assertTrue(doc2.hasFacet("Aged"));
        assertEquals("456", doc2.getPropertyValue("age:age"));

        // remove facet in first doc
        assertTrue(doc.removeFacet("Aged"));
        doc = session.saveDocument(doc);

        // after refresh should be removed in second doc
        doc2.refresh();
        assertFalse(doc2.hasFacet("Aged"));
        try {
            doc2.setPropertyValue("age:age", "123");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }

        // check immutable facet
        DocumentRef verRef = doc.checkIn(VersioningOption.MINOR, null);
        DocumentModel version = session.getDocument(verRef);
        assertTrue(version.hasFacet(FacetNames.IMMUTABLE));
        version.refresh();
        assertTrue(version.hasFacet(FacetNames.IMMUTABLE));

        // check that REFRESH_STATE does not overwrite facets
        // this is called by doc.isCheckedOut() for instance
        doc = session.getDocument(doc.getRef());
        doc.addFacet("Aged");
        assertTrue(doc.hasFacet("Aged"));
        assertTrue(doc.hasSchema("age"));
        doc.refresh(DocumentModel.REFRESH_STATE, null);
        assertTrue(doc.hasFacet("Aged"));
        assertTrue(doc.hasSchema("age"));
        doc = session.saveDocument(doc);
        doc.refresh(DocumentModel.REFRESH_ALL, null);
    }

    @Test
    public void testLifeCycleAPI() {
        DocumentModel root = session.getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), "file#" + generateUnique(), "File");
        childFile = createChildDocument(childFile);

        assertEquals("default", session.getLifeCyclePolicy(childFile.getRef()));

        assertEquals("project", session.getCurrentLifeCycleState(childFile.getRef()));

        Collection<String> allowedStateTransitions = session.getAllowedStateTransitions(childFile.getRef());
        assertEquals(3, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("approve"));
        assertTrue(allowedStateTransitions.contains("obsolete"));
        assertTrue(allowedStateTransitions.contains("delete"));

        assertTrue(session.followTransition(childFile.getRef(), "approve"));
        assertEquals("approved", session.getCurrentLifeCycleState(childFile.getRef()));
        allowedStateTransitions = session.getAllowedStateTransitions(childFile.getRef());
        assertEquals(2, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("delete"));
        assertTrue(allowedStateTransitions.contains("backToProject"));

        session.reinitLifeCycleState(childFile.getRef());
        assertEquals("project", session.getCurrentLifeCycleState(childFile.getRef()));
    }

    @Test
    public void testLifeCycleVersioning() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), "file#" + generateUnique(), "File");
        childFile = createChildDocument(childFile);

        assertEquals("default", session.getLifeCyclePolicy(childFile.getRef()));
        assertEquals("project", session.getCurrentLifeCycleState(childFile.getRef()));
        assertTrue("Document should be checkout after creation", childFile.isCheckedOut());

        session.checkIn(childFile.getRef(), VersioningOption.MAJOR, "Increment major version");
        childFile = session.getDocument(childFile.getRef());
        assertEquals("1.0", childFile.getVersionLabel());
        assertFalse("Document should be checkin after version ", childFile.isCheckedOut());

        boolean success = session.followTransition(childFile.getRef(), "approve");
        assertTrue(success);
        childFile = session.getDocument(childFile.getRef());
        assertEquals("1.0+", childFile.getVersionLabel());
        assertTrue("Document should be checkout after following a transition", childFile.isCheckedOut());
    }

    @Test
    public void testDataModelLifeCycleAPI() {
        DocumentModel root = session.getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), "file#" + generateUnique(), "File");
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

    @Test
    public void testCopy() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(), "folder1", "Folder");

        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(), "folder2", "Folder");

        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(), "file", "File");

        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        file = createChildDocument(file);

        session.save();

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertFalse(session.exists(new PathRef("/folder2/file")));

        // copy using orig name
        DocumentModel copy1 = session.copy(file.getRef(), folder2.getRef(), null);

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertTrue(session.exists(new PathRef("/folder2/file")));
        assertFalse(session.exists(new PathRef("/folder2/fileCopy")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy1));

        // copy using another name
        DocumentModel copy2 = session.copy(file.getRef(), folder2.getRef(), "fileCopy");

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertTrue(session.exists(new PathRef("/folder2/file")));
        assertTrue(session.exists(new PathRef("/folder2/fileCopy")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy2));

        // copy again to same space
        DocumentModel copy3 = session.copy(file.getRef(), folder2.getRef(), null);

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertTrue(session.exists(new PathRef("/folder2/file")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy3));
        assertNotSame(copy1.getName(), copy3.getName());

        // copy again again to same space
        if (SystemUtils.IS_OS_WINDOWS) {
            // windows has too coarse time granularity for SQLSession.findFreeName
            Thread.sleep(1000);
        }
        DocumentModel copy4 = session.copy(file.getRef(), folder2.getRef(), null);

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertTrue(session.exists(new PathRef("/folder2/file")));
        assertTrue(session.getChildren(folder2.getRef()).contains(copy4));
        assertNotSame(copy1.getName(), copy4.getName());
        assertNotSame(copy3.getName(), copy4.getName());

        // copy inplace
        DocumentModel copy5 = session.copy(file.getRef(), folder1.getRef(), null);

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertTrue(session.exists(new PathRef("/folder2/file")));
        assertTrue(session.getChildren(folder1.getRef()).contains(copy5));
        assertNotSame(copy1.getName(), copy5.getName());

        session.cancel();
    }

    @Test
    public void testCopyProxyAsDocument() throws Exception {
        // create a folder tree
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(), "folder1", "Folder");
        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(), "folder2", "Folder");
        DocumentModel folder3 = new DocumentModelImpl(root.getPathAsString(), "folder3", "Folder");
        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(), "copyProxyAsDocument_test", "File");
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
        DocumentModel copy1 = session.copyProxyAsDocument(proxy.getRef(), folder3.getRef(), null);
        assertFalse(copy1.isProxy());
        assertEquals(proxy.getName(), copy1.getName());
        assertEquals(proxy.getProperty("dublincore", "title"), copy1.getProperty("dublincore", "title"));

        // copy proxy using another name
        DocumentModel copy2 = session.copyProxyAsDocument(proxy.getRef(), folder3.getRef(), "foo");
        assertFalse(copy2.isProxy());
        assertEquals("foo", copy2.getName());
        assertEquals(file.getProperty("dublincore", "title"), copy2.getProperty("dublincore", "title"));

        session.cancel();
    }

    @Test
    public void testCopyVersionable() throws Exception {
        DocumentModel note = new DocumentModelImpl("/", "note", "Note");
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        note = session.createDocument(note);
        folder = session.createDocument(folder);
        session.save();

        assertTrue(session.exists(new PathRef("/note")));
        assertTrue(session.exists(new PathRef("/folder")));

        // no versions at first
        List<DocumentRef> versions = session.getVersionsRefs(note.getRef());
        assertEquals(0, versions.size());

        // version the note
        note.setProperty("dublincore", "title", "blah");
        ScopedMap context = note.getContextData();
        context.putScopedValue(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
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

    @Test
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
        context.putScopedValue(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
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

    @Test
    public void testMove() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(), "folder1", "Folder");

        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(), "folder2", "Folder");

        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(), "file", "File");

        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        file = createChildDocument(file);

        assertTrue(session.exists(new PathRef("/folder1/file")));
        assertFalse(session.exists(new PathRef("/folder2/file")));
        assertFalse(session.exists(new PathRef("/folder1/fileMove")));

        // move using orig name
        session.move(file.getRef(), folder2.getRef(), null);

        assertFalse(session.exists(new PathRef("/folder1/file")));
        assertTrue(session.exists(new PathRef("/folder2/file")));

        file = session.getChild(folder2.getRef(), "file");
        session.move(file.getRef(), folder1.getRef(), "fileMove");

        assertTrue(session.exists(new PathRef("/folder1/fileMove")));

        DocumentModel file2 = new DocumentModelImpl(folder2.getPathAsString(), "file2", "File");
        file2 = createChildDocument(file2);
        assertTrue(session.exists(new PathRef("/folder2/file2")));
        DocumentModel newFile2 = session.move(file.getRef(), folder2.getRef(), "file2"); // collision
        String newName = newFile2.getName();
        assertFalse("file2".equals(newName));
        assertTrue(session.exists(new PathRef("/folder2/file2")));
        assertTrue(session.exists(new PathRef("/folder2/" + newName)));

        // move with null dest (rename)
        DocumentModel newFile3 = session.move(file.getRef(), null, "file3");
        assertEquals("file3", newFile3.getName());
    }

    // TODO NXP-2514: fix this test
    @Test
    @Ignore
    public void testScalarList() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
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

    @Test
    public void testBlob() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        byte[] bytes = FileUtils.readBytes(Blob.class.getResourceAsStream("Blob.class"));
        Blob blob = Blobs.createBlob(bytes, "java/class", "UTF8");
        blob.setFilename("blob.txt");
        blob.setDigest("XXX");
        long length = blob.getLength();
        byte[] content = blob.getByteArray();

        childFile.setProperty("dublincore", "title", "Blob test");
        childFile.setProperty("dublincore", "description", "this is a test");
        childFile.setProperty("file", "content", blob);

        session.saveDocument(childFile);

        childFile = session.getDocument(childFile.getRef());
        blob = (Blob) childFile.getProperty("file", "content");

        // digest algorithm is null for any type of blob other than BinaryBlob
        assertNull(blob.getDigestAlgorithm());
        assertEquals("XXX", blob.getDigest());
        assertEquals("blob.txt", blob.getFilename());
        assertEquals(length, blob.getLength());
        assertEquals("UTF8", blob.getEncoding());
        assertEquals("java/class", blob.getMimeType());
        assertTrue(Arrays.equals(content, blob.getByteArray()));

        // blob from a stream, with no known length
        URL url = getClass().getClassLoader().getResource("META-INF/MANIFEST.MF");
        blob = new URLBlob(url, "java/manifest", null);
        blob.setFilename("manifest.mf");
        blob.setDigest("YYY");
        childFile.setPropertyValue("content", (Serializable) blob);
        session.saveDocument(childFile);
        childFile = session.getDocument(childFile.getRef());
        blob = (Blob) childFile.getPropertyValue("content");
        assertEquals("YYY", blob.getDigest());
        assertEquals("manifest.mf", blob.getFilename());
        assertEquals(null, blob.getEncoding());
        assertEquals("java/manifest", blob.getMimeType());
        assertEquals(FileUtils.readBytes(url).length, blob.getLength());
    }

    @Test
    public void testRetrieveSamePropertyInAncestors() {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(), "folder1", "Folder");
        folder1 = createChildDocument(folder1);
        folder1.setProperty("dublincore", "title", "folder #1");
        assertEquals("folder #1", folder1.getProperty("dublincore", "title"));

        DocumentModel folder2 = new DocumentModelImpl(folder1.getPathAsString(), "folder2", "Folder");
        folder2 = createChildDocument(folder2);
        folder2.setProperty("dublincore", "title", "folder #2");
        assertEquals("folder #2", folder2.getProperty("dublincore", "title"));

        DocumentModel file = new DocumentModelImpl(folder2.getPathAsString(), "file", "File");
        file = createChildDocument(file);
        file.setProperty("dublincore", "title", "file ##");
        assertEquals("file ##", file.getProperty("dublincore", "title"));

        assertTrue(session.exists(new PathRef("/folder1")));
        assertTrue(session.exists(new PathRef("/folder1/folder2")));
        assertTrue(session.exists(new PathRef("/folder1/folder2/file")));

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

        final Object[] fieldValues = session.getDataModelsField(ancestorRefs, "dublincore", "title");
        assertNotNull(fieldValues);
        assertEquals(3, fieldValues.length);
        assertEquals("folder #2", fieldValues[0]);
        assertEquals("folder #1", fieldValues[1]);

        final Object[] fieldValuesBis = session.getDataModelsFieldUp(file.getRef(), "dublincore", "title");
        assertNotNull(fieldValuesBis);
        assertEquals(4, fieldValuesBis.length);
        assertEquals("file ##", fieldValuesBis[0]);
        assertEquals("folder #2", fieldValuesBis[1]);
        assertEquals("folder #1", fieldValuesBis[2]);
    }

    // TODO NXP-2514: fix and reenable.
    @Test
    @Ignore
    public void testDocumentAdapter() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(), "file", "File");

        file = createChildDocument(file);

        /*
         * AnnotatedDocument adoc = file.getAdapter(AnnotatedDocument.class); assertNotNull(adoc);
         * adoc.putAnnotation("key1", "val1"); adoc.putAnnotation("key2", "val2"); assertEquals("val1",
         * adoc.getAnnotation("key1")); assertEquals("val2", adoc.getAnnotation("key2")); adoc =
         * file.getAdapter(AnnotatedDocument.class); assertEquals("val1", adoc.getAnnotation("key1"));
         * assertEquals("val2", adoc.getAnnotation("key2"));
         */
    }

    @Test
    public void testGetSourceId() {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
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
        // TODO NXP-2514: look at this test.
        // assertFalse(childFile.getId().equals(childFile.getSourceId()));
    }

    @Test
    public void testGetRepositoryName() {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(), name2, "File");
        childFile = createChildDocument(childFile);
        assertNotNull(childFile.getRepositoryName());
        assertEquals("test", childFile.getRepositoryName());
    }

    // TODO NXP-2514: fix and reenable, is this a bug?
    @Test
    @Ignore
    public void testRetrieveProxies() {
        DocumentModel root = session.getRootDocument();

        // Section A
        String name = "section" + generateUnique();
        DocumentModel sectionA = new DocumentModelImpl(root.getPathAsString(), name, "Section");
        sectionA = createChildDocument(sectionA);

        assertEquals("Section", sectionA.getType());
        assertEquals(name, sectionA.getName());

        // Section B
        name = "section" + generateUnique();
        DocumentModel sectionB = new DocumentModelImpl(root.getPathAsString(), name, "Section");
        sectionB = createChildDocument(sectionB);

        assertEquals("Section", sectionB.getType());
        assertEquals(name, sectionB.getName());

        // File
        name = "file" + generateUnique();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(), name, "File");
        file = createChildDocument(file);

        assertEquals("File", file.getType());
        assertEquals(name, file.getName());

        // Versioning
        // session.saveDocumentAsNewVersion(file);

        // Publishing
        session.publishDocument(file, sectionA);
        // session.publishDocument(file, sectionB);

        // Retrieving proxies
        DocumentModelList proxies = session.getProxies(file.getRef(), sectionA.getRef());

        assertFalse(proxies.isEmpty());
        assertEquals(1, proxies.size());
        // assertEquals(2, proxies.size());
    }

    @Test
    public void testCreateDocumentModel() {
        // first method: only the typename
        DocumentModel docModel = session.createDocumentModel("File");
        assertEquals("File", docModel.getType());

        // bad type should fail
        try {
            session.createDocumentModel("NotAValidTypeName");
            fail();
        } catch (IllegalArgumentException e) {
        }

        // same as previously with path info
        docModel = session.createDocumentModel("/path/to/parent", "some-id", "File");
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
    @Test
    public void testCopyContent() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "original", "File");
        doc.setProperty("dublincore", "title", "t");
        doc.setProperty("dublincore", "description", "d");
        doc.setProperty("dublincore", "subjects", new String[] { "a", "b" });
        List<Object> files = new ArrayList<>(1);
        Map<String, Object> f = new HashMap<>();
        f.put("file", Blobs.createBlob("myfile", "text/test", "UTF-8"));
        files.add(f);
        doc.setProperty("files", "files", files);
        // add a dynamic facet too
        doc.addFacet("Aged");
        doc.setPropertyValue("age:age", "123");
        doc = session.createDocument(doc);
        session.save();

        DocumentModel copy = new DocumentModelImpl(root.getPathAsString(), "copy", "File");
        copy.copyContent(doc);
        copy = session.createDocument(copy);
        session.save();

        assertEquals("t", copy.getProperty("dublincore", "title"));
        assertEquals("d", copy.getProperty("dublincore", "description"));
        assertEquals(Arrays.asList("a", "b"), Arrays.asList((String[]) copy.getProperty("dublincore", "subjects")));
        Object fileso = copy.getProperty("files", "files");
        assertNotNull(fileso);
        List<Map<String, Object>> newfiles = (List<Map<String, Object>>) fileso;
        assertEquals(1, newfiles.size());
        Blob bb = (Blob) newfiles.get(0).get("file");
        assertNotNull(bb);
        assertEquals("text/test", bb.getMimeType());
        assertEquals("UTF-8", bb.getEncoding());
        String content = bb.getString();
        assertEquals("myfile", content);
        // check dynamic facet
        assertTrue(copy.hasFacet("Aged"));
        assertEquals("123", copy.getPropertyValue("age:age"));
    }

    @Test
    public void testCopyContentTransTyping() {
        DocumentModel note = session.createDocumentModel("/", "original", "Note");
        note.setPropertyValue("dc:title", "a title");
        note.setPropertyValue("note:note", "this is a note");
        note.setPropertyValue("note:mime_type", "text/plain");
        // add a dynamic facet too
        note.addFacet("Aged");
        note.setPropertyValue("age:age", "123");
        note = session.createDocument(note);
        session.save();

        DocumentModel copy = session.createDocumentModel("/", "copy", "File");
        copy.copyContent(note);
        copy = session.createDocument(copy);
        session.save();

        assertEquals("a title", copy.getPropertyValue("dc:title"));
        // the note schema was dropped as it's not present in File
        // check dynamic facet
        assertTrue(copy.hasFacet("Aged"));
        assertEquals("123", copy.getPropertyValue("age:age"));
    }

    // ------------------------------------
    // ----- copied from TestLocalAPI -----
    // ------------------------------------

    @Test
    public void testPropertyModel() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "theDoc", "MyDocType");

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

    @Test
    public void testOrdering() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(), "theParent", "OrderedFolder");

        parent = session.createDocument(parent);

        DocumentModel doc1 = new DocumentModelImpl(parent.getPathAsString(), "the1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl(parent.getPathAsString(), "the2", "File");
        doc2 = session.createDocument(doc2);
        session.save(); // XXX

        String name1 = doc1.getName();
        String name2 = doc2.getName();

        DocumentModelList children = session.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name1, children.get(0).getName());
        assertEquals(name2, children.get(1).getName());
        assertEquals(Long.valueOf(0), children.get(0).getPos());
        assertEquals(Long.valueOf(1), children.get(1).getPos());

        session.orderBefore(parent.getRef(), name2, name1);

        children = session.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name2, children.get(0).getName());
        assertEquals(name1, children.get(1).getName());
        assertEquals(Long.valueOf(0), children.get(0).getPos());
        assertEquals(Long.valueOf(1), children.get(1).getPos());

        session.orderBefore(parent.getRef(), name2, null);

        children = session.getChildren(parent.getRef());
        assertEquals(2, children.size());
        assertEquals(name1, children.get(0).getName());
        assertEquals(name2, children.get(1).getName());
        assertEquals(Long.valueOf(0), children.get(0).getPos());
        assertEquals(Long.valueOf(1), children.get(1).getPos());

        // check in a non-ordered folder
        DocumentModel parent2 = session.createDocumentModel("/", "folder", "Folder");
        parent2 = session.createDocument(parent2);
        DocumentModel doc3 = session.createDocumentModel("/folder", "doc3", "MyDocType");
        doc3 = session.createDocument(doc3);
        session.save();
        doc3 = session.getDocument(doc3.getRef());
        assertNull(doc3.getPos());
    }

    @Test
    public void testOrderingAfterCopy() throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "folder", "OrderedFolder");
        folder = session.createDocument(folder);
        DocumentModel doc1 = session.createDocumentModel("/folder", "doc1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/folder", "doc2", "File");
        doc2 = session.createDocument(doc2);

        // copy doc1 as doc3, should be positioned last
        session.copy(doc1.getRef(), folder.getRef(), "doc3");

        DocumentModelList children = session.getChildren(folder.getRef());
        assertEquals(3, children.size());
        assertEquals("doc1", children.get(0).getName());
        assertEquals("doc2", children.get(1).getName());
        assertEquals("doc3", children.get(2).getName());
    }

    @Test
    public void testPropertyXPath() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel parent = new DocumentModelImpl(root.getPathAsString(), "theParent", "OrderedFolder");

        parent = session.createDocument(parent);

        DocumentModel doc = new DocumentModelImpl(parent.getPathAsString(), "theDoc", "File");

        doc.setProperty("dublincore", "title", "my title");
        assertEquals("my title", doc.getPropertyValue("title"));
        assertEquals("my title", doc.getPropertyValue("dc:title"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testComplexList() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "mydoc", "MyDocType");

        doc = session.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        /*
         * diff.add(new Attachment("at1", "value1").asMap()); diff.add(new Attachment("at2", "value2").asMap());
         * doc.setProperty("testList", "attachments", diff); doc = session.saveDocument(doc); list = (List)
         * doc.getProperty("testList", "attachments"); assertNotNull(list); assertEquals(2, list.size()); Blob blob;
         * blob = (Blob) ((Map) list.get(0)).get("content"); assertEquals("value1", blob.getString()); blob = (Blob)
         * ((Map) list.get(1)).get("content"); assertEquals("value2", blob.getString()); diff = new ListDiff();
         * diff.remove(0); diff.insert(0, new Attachment("at1.bis", "value1.bis").asMap()); doc.setProperty("testList",
         * "attachments", diff); doc = session.saveDocument(doc); list = (List) doc.getProperty("testList",
         * "attachments"); assertNotNull(list); assertEquals(2, list.size()); blob = (Blob) ((Map)
         * list.get(0)).get("content"); assertEquals("value1.bis", blob.getString()); blob = (Blob) ((Map)
         * list.get(1)).get("content"); assertEquals("value2", blob.getString()); diff = new ListDiff(); diff.move(0,
         * 1); doc.setProperty("testList", "attachments", diff); doc = session.saveDocument(doc); list = (List)
         * doc.getProperty("testList", "attachments"); assertNotNull(list); assertEquals(2, list.size()); blob = (Blob)
         * ((Map) list.get(0)).get("content"); assertEquals("value2", blob.getString()); blob = (Blob) ((Map)
         * list.get(1)).get("content"); assertEquals("value1.bis", blob.getString()); diff = new ListDiff();
         * diff.removeAll(); doc.setProperty("testList", "attachments", diff); doc = session.saveDocument(doc); list =
         * (List) doc.getProperty("testList", "attachments"); assertNotNull(list); assertEquals(0, list.size());
         */
    }

    @Test
    public void testDataModel() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "mydoc", "Book");

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

        doc = new DocumentModelImpl(root.getPathAsString(), "mydoc2", "MyDocType");

        doc = session.createDocument(doc);

        List list = (List) doc.getProperty("testList", "attachments");
        assertNotNull(list);
        assertTrue(list.isEmpty());

        ListDiff diff = new ListDiff();
        /*
         * diff.add(new Attachment("at1", "value1").asMap()); diff.add(new Attachment("at2", "value2").asMap());
         * doc.setProperty("testList", "attachments", diff); doc = session.saveDocument(doc); dm =
         * doc.getDataModel("testList"); dm.setValue("attachments/item[0]/name", "at1-modif"); assertEquals("at1-modif",
         * dm.getValue("attachments/item[0]/name")); dm.setValue("attachments/item[0]/name", "at1-modif2");
         * assertEquals("at1-modif2", dm.getValue("attachments/item[0]/name")); dm.setValue("attachments/item[1]/name",
         * "at2-modif"); assertEquals("at2-modif", dm.getValue("attachments/item[1]/name"));
         * dm.setValue("attachments/item[1]/name", "at2-modif2"); assertEquals("at2-modif2",
         * dm.getValue("attachments/item[1]/name"));
         */
    }

    @Test
    public void testGetChildrenRefs() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "mydoc", "Book");
        doc = session.createDocument(doc);
        DocumentModel doc2 = new DocumentModelImpl(root.getPathAsString(), "mydoc2", "MyDocType");
        doc2 = session.createDocument(doc2);
        List<DocumentRef> childrenRefs = session.getChildrenRefs(root.getRef(), null);
        assertEquals(2, childrenRefs.size());
        Set<String> expected = new HashSet<>();
        expected.add(doc.getId());
        expected.add(doc2.getId());
        Set<String> actual = new HashSet<>();
        actual.add(childrenRefs.get(0).toString());
        actual.add(childrenRefs.get(1).toString());
        assertEquals(expected, actual);
    }

    @Test
    public void testProxyChildren() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc1 = new DocumentModelImpl(root.getPathAsString(), "doc1", "Book");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = new DocumentModelImpl(root.getPathAsString(), "doc2", "Book");
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
        assertEquals(proxy2, session.getChild(proxy1.getRef(), proxy2.getName()));
        assertTrue(session.hasChildren(proxy1.getRef()));
    }

    public static byte[] createBytes(int size, byte val) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, val);
        return bytes;
    }

    @Test
    public void testBlob2() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "mydoc", "File");

        doc = session.createDocument(doc);
        byte[] bytes = createBytes(1024 * 1024, (byte) 24);

        Blob blob = Blobs.createBlob(bytes);
        doc.getPart("file").get("content").setValue(blob);
        doc = session.saveDocument(doc);

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

        // reset not implemented (not needed) for StorageBlob's Binary
        // XXX blob.getStream().reset();

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));
    }

    @Test
    public void testProxy() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "the title");
        doc = session.saveDocument(doc);

        DocumentModel proxy = session.publishDocument(doc, root);
        session.save();

        // re-modify doc
        doc.setProperty("dublincore", "title", "the title modified");
        doc = session.saveDocument(doc);

        assertEquals("the title", proxy.getProperty("dublincore", "title"));
        assertEquals("the title modified", doc.getProperty("dublincore", "title"));

        // make another proxy
        maybeSleepToNextSecond();
        session.publishDocument(doc, root);

        DocumentModelList list = session.getChildren(root.getRef());
        assertEquals(2, list.size());

        for (DocumentModel model : list) {
            assertEquals("File", model.getType());
        }

        session.removeDocument(proxy.getRef());
        session.save();
        waitForAsyncCompletion(); // orphan version removal
        session.save(); // process invalidations
        list = session.getChildren(root.getRef());
        assertEquals(1, list.size());

        // create folder to hold proxies
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(), "folder", "Folder");
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

        // publish a restored version
        List<DocumentRef> versions = session.getVersionsRefs(doc.getRef());
        assertEquals(2, versions.size());
        doc = session.restoreToVersion(doc.getRef(), versions.get(0));
        assertEquals("0.1", doc.getVersionLabel());
        proxy = session.publishDocument(doc, folder);
        assertEquals("0.1", proxy.getVersionLabel());

        // publish a version directly
        DocumentModel ver = session.getLastDocumentVersion(doc.getRef());
        DocumentModel proxy3 = session.publishDocument(ver, folder, false);
        session.save();
        assertFalse(proxy3.isVersion());
        assertTrue(proxy3.isProxy());
        assertEquals(doc.getVersionSeriesId(), proxy3.getVersionSeriesId());
        assertEquals(ver.getVersionLabel(), proxy3.getVersionLabel());
    }

    @Test
    public void testProxyLive() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "proxy_test", "File");

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
        assertEquals("the title modified", proxy.getProperty("dublincore", "title"));

        // modify proxy
        proxy.setProperty("dublincore", "title", "the title again");
        proxy = session.saveDocument(proxy);
        session.save();

        // check visible from live doc
        doc = session.getDocument(doc.getRef());
        assertEquals("the title again", doc.getProperty("dublincore", "title"));
    }

    @Test
    public void testProxySchemas() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = new DocumentModelImpl("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();
        try {
            doc.setPropertyValue("info:info", "docinfo");
            doc = session.saveDocument(doc);
            session.save();
        } catch (PropertyNotFoundException e) {
            assertTrue(e.getMessage().contains("info:info"));
        }

        assertNull(proxy.getPropertyValue("info:info"));
        proxy.setPropertyValue("info:info", "proxyinfo");
        proxy = session.saveDocument(proxy);
        session.save();

        // new session
        reopenSession();
        DocumentModel root = session.getRootDocument();
        proxy = session.getDocument(proxy.getRef());
        assertEquals("proxyinfo", proxy.getPropertyValue("info:info"));

        // test a query
        String nxql;
        DocumentModelList list;
        nxql = "SELECT * FROM Document WHERE info:info = 'proxyinfo' AND ecm:isProxy = 1";
        list = session.query(nxql);
        assertEquals(1, list.size());
        nxql = "SELECT * FROM Document WHERE info:info = 'proxyinfo'";
        list = session.query(nxql);
        assertEquals(1, list.size());
        nxql = "SELECT * FROM Document WHERE info:info = 'proxyinfo' AND ecm:isProxy = 0";
        list = session.query(nxql);
        assertEquals(0, list.size());

        // queryAndFetch
        nxql = "SELECT ecm:uuid, info:info FROM File WHERE info:info IS NOT NULL";
        IterableQueryResult res = session.queryAndFetch(nxql, "NXQL");
        Map<Serializable, String> actual = new HashMap<>();
        for (Map<String, Serializable> map : res) {
            Serializable uuid = map.get("ecm:uuid");
            String info = (String) map.get("info:info");
            actual.put(uuid.toString(), info); // toString() for sequence ids
        }
        res.close();
        assertEquals(Collections.singletonMap(proxy.getId(), "proxyinfo"), actual);

        // test that the copy has the extra schema values
        session.copy(folder.getRef(), root.getRef(), "folderCopy");
        DocumentModel proxyCopy = session.getDocument(new PathRef("/folderCopy/file"));
        assertTrue(proxyCopy.isProxy());
        assertEquals("proxyinfo", proxyCopy.getPropertyValue("info:info"));
    }

    @Test
    public void testUpdatePublishedDocument() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "the title");
        doc = session.saveDocument(doc);

        // create folder to hold proxies
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(), "folder", "Folder");
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

    @Test
    public void testImport() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder.setProperty("dublincore", "title", "the title");
        folder = session.createDocument(folder);
        session.save();
        String folderId = folder.getId();

        // create a version by import
        String id; // versionable
        String vid; // ver id
        String pid; // proxy id
        if (folderId.length() == 36) {
            id = "aaaaaaaa-1234-1234-1234-fedcba987654";
            vid = "12345678-1234-1234-1234-fedcba987654";
            pid = "00000000-1234-1234-1234-fedcba987654";
        } else {
            id = "888001";
            vid = "777002";
            pid = "666003";
        }
        String typeName = "File";
        DocumentRef parentRef = null;
        String name = "foobar";
        DocumentModel ver = new DocumentModelImpl((String) null, typeName, vid, new Path(name), null, null, parentRef,
                null, null, null, null);
        Calendar vcr = new GregorianCalendar(2009, Calendar.JANUARY, 1, 2, 3, 4);
        ver.putContextData(CoreSession.IMPORT_VERSION_VERSIONABLE_ID, id);
        ver.putContextData(CoreSession.IMPORT_VERSION_CREATED, vcr);
        ver.putContextData(CoreSession.IMPORT_VERSION_LABEL, "v1");
        ver.putContextData(CoreSession.IMPORT_VERSION_DESCRIPTION, "v descr");
        ver.putContextData(CoreSession.IMPORT_IS_VERSION, Boolean.TRUE);
        ver.putContextData(CoreSession.IMPORT_VERSION_IS_LATEST, Boolean.TRUE);
        ver.putContextData(CoreSession.IMPORT_VERSION_IS_LATEST_MAJOR, Boolean.FALSE);
        ver.putContextData(CoreSession.IMPORT_VERSION_MAJOR, Long.valueOf(3));
        ver.putContextData(CoreSession.IMPORT_VERSION_MINOR, Long.valueOf(14));
        ver.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, "v lcp");
        ver.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, "v lcst");
        ver.setProperty("dublincore", "title", "Ver title");
        Calendar mod = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34, 56);
        ver.setProperty("dublincore", "modified", mod);
        session.importDocuments(Collections.singletonList(ver));
        session.save();

        reopenSession();
        ver = session.getDocument(new IdRef(vid));
        // assertEquals(name, doc.getName()); // no path -> no name...
        assertEquals("Ver title", ver.getProperty("dublincore", "title"));
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
        typeName = CoreSession.IMPORT_PROXY_TYPE;
        parentRef = new IdRef(folderId);
        name = "myproxy";
        DocumentModel proxy = new DocumentModelImpl((String) null, typeName, pid, new Path(name), null, null,
                parentRef, null, null, null, null);
        proxy.putContextData(CoreSession.IMPORT_PROXY_TARGET_ID, vid);
        proxy.putContextData(CoreSession.IMPORT_PROXY_VERSIONABLE_ID, id);
        session.importDocuments(Collections.singletonList(proxy));
        session.save();

        reopenSession();
        proxy = session.getDocument(new IdRef(pid));
        assertEquals(name, proxy.getName());
        assertEquals("Ver title", proxy.getProperty("dublincore", "title"));
        assertEquals(mod, proxy.getProperty("dublincore", "modified"));
        assertEquals("v lcp", proxy.getLifeCyclePolicy());
        assertEquals("v lcst", proxy.getCurrentLifeCycleState());
        assertFalse(proxy.isVersion());
        assertTrue(proxy.isProxy());

        // create a normal doc by import
        typeName = "File";
        parentRef = new IdRef(folderId);
        name = "mydoc";
        DocumentModel doc = new DocumentModelImpl((String) null, typeName, id, new Path(name), null, null, parentRef,
                null, null, null, null);
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, "lcp");
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, "lcst");
        Calendar lockCreated = new GregorianCalendar(2011, Calendar.JANUARY, 1, 5, 5, 5);
        doc.putContextData(CoreSession.IMPORT_LOCK_OWNER, "bob");
        doc.putContextData(CoreSession.IMPORT_LOCK_CREATED, lockCreated);
        doc.putContextData(CoreSession.IMPORT_CHECKED_IN, Boolean.TRUE);
        doc.putContextData(CoreSession.IMPORT_BASE_VERSION_ID, vid);
        doc.putContextData(CoreSession.IMPORT_VERSION_MAJOR, Long.valueOf(8));
        doc.putContextData(CoreSession.IMPORT_VERSION_MINOR, Long.valueOf(1));
        doc.setProperty("dublincore", "title", "Live title");
        session.importDocuments(Collections.singletonList(doc));
        session.save();

        reopenSession();
        doc = session.getDocument(new IdRef(id));
        assertEquals(name, doc.getName());
        assertEquals("Live title", doc.getProperty("dublincore", "title"));
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
     * Check that lifecycle and dc:issued can be updated on a version. (Fields defined in
     * SQLDocumentLive#VERSION_WRITABLE_PROPS).
     */
    @Test
    public void testVersionUpdatableFields() throws Exception {
        Calendar cal1 = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34, 56);
        Calendar cal2 = new GregorianCalendar(2010, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar cal3 = new GregorianCalendar(2010, Calendar.APRIL, 11, 11, 11, 11);

        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "doc", "File");

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

        reopenSession();
        doc = session.getDocument(new PathRef("/doc"));
        ver = session.getLastDocumentVersion(doc.getRef());
        assertEquals("t1", ver.getProperty("dublincore", "title"));
        assertEquals("approved", ver.getCurrentLifeCycleState());
        assertEquals(cal3, ver.getProperty("dublincore", "issued"));
    }

    /**
     * Check that the "incrementBeforeUpdate" is not fired on a DocumentModel where the {@code isImmutable()} returns
     * {@code true}.
     */
    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-listeners-contrib.xml")
    public void testDoNotFireIncrementBeforeUpdateEventsOnVersion() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(), "doc", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "t1");
        doc = session.saveDocument(doc);

        session.checkIn(doc.getRef(), null, null);
        session.checkOut(doc.getRef());
        doc.setProperty("dublincore", "title", "t2");
        doc = session.saveDocument(doc);
        session.save();

        // Reset the listener
        DummyTestListener.clear();
        DocumentModel versionDoc = session.getLastDocumentVersion(doc.getRef());
        versionDoc.setProperty("dublincore", "issued", new GregorianCalendar());
        session.saveDocument(versionDoc);
        session.save();

        assertEquals(1, DummyTestListener.EVENTS_RECEIVED.size());
        assertEquals("beforeDocumentModification", DummyTestListener.EVENTS_RECEIVED.get(0).getName());

    }

    private static final List<String> IGNORED_EVENTS = Arrays.asList(DocumentEventTypes.SESSION_SAVED);

    public static void assertEvents(String... expectedEventNames) {
        assertEvents(IGNORED_EVENTS, expectedEventNames);
    }

    public static void assertEvents(List<String> ignored, String... expectedEventNames) {
        assertEquals(Arrays.asList(expectedEventNames), getDummyListenerEvents(ignored));
    }

    protected static List<String> getDummyListenerEvents(List<String> ignored) {
        List<String> actual = new ArrayList<>();
        for (Event event : DummyTestListener.EVENTS_RECEIVED) {
            String eventName = event.getName();
            if (ignored != null && ignored.contains(eventName)) {
                continue;
            }
            EventContext context = event.getContext();
            if (context instanceof DocumentEventContext) {
                DocumentModel doc = ((DocumentEventContext) context).getSourceDocument();
                if (doc != null) {
                    if (doc.isProxy()) {
                        eventName += "/p";
                    } else if (doc.isVersion()) {
                        eventName += "/v";
                    } else if (doc.isFolder()) {
                        eventName += "/f";
                    }
                }
            }
            actual.add(eventName);
        }
        return actual;
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-listeners-all-contrib.xml")
    public void testVersioningEvents() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc = session.createDocument(doc);
        DocumentModel folder = new DocumentModelImpl("/", "fold", "Folder");
        folder = session.createDocument(folder);

        DummyTestListener.clear();
        DocumentRef verRef = session.checkIn(doc.getRef(), null, null);
        assertEvents( //
                "aboutToCheckIn", //
                "documentCheckedIn", //
                "documentCreated/v");

        DummyTestListener.clear();
        session.checkOut(doc.getRef());
        assertEvents( //
                "aboutToCheckout", //
                "documentCheckedOut");

        DummyTestListener.clear();
        session.createProxy(doc.getRef(), folder.getRef()); // live proxy
        assertEvents( //
                "documentCreated/p", //
                "documentProxyPublished/p", //
                "sectionContentPublished/f");

        DummyTestListener.clear();
        session.publishDocument(doc, folder, false);
        assertEvents( //
                "aboutToCheckIn", //
                "documentCheckedIn", //
                "documentCreated/v", //
                "documentCreated/p", //
                "documentProxyPublished/p", //
                "sectionContentPublished/f");

        // auto-checkout
        DummyTestListener.clear();
        doc.setPropertyValue("dc:title", "title2");
        doc = session.saveDocument(doc);
        assertEvents( //
                "beforeDocumentModification", //
                "aboutToCheckout", //
                "documentCheckedOut", //
                "documentModified");

        // save with versioning
        DummyTestListener.clear();
        doc.setPropertyValue("dc:title", "title2");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);
        assertEvents( //
                "beforeDocumentModification", //
                "aboutToCheckIn", //
                "documentCheckedIn", //
                "documentCreated/v", //
                "documentModified");

        doc.checkOut();

        // restore to version
        DummyTestListener.clear();
        session.restoreToVersion(doc.getRef(), verRef, false, false);
        assertEvents( //
                "aboutToCheckIn", //
                "documentCheckedIn", //
                "documentCreated/v", //
                "beforeRestoringDocument", //
                "documentRestored", //
                "aboutToCheckout", //
                "documentCheckedOut");
    }

    @Test
    public void testPlacelessDocument() throws Exception {
        DocumentModel doc = new DocumentModelImpl((String) null, "mydoc", "MyDocType");
        doc.setProperty("dublincore", "title", "The title");
        doc = session.createDocument(doc);
        assertNull(doc.getParentRef()); // placeless
        session.save();

        DocumentModel doc2 = session.createDocumentModel(null, "other", "MyDocType");
        doc2.setProperty("dublincore", "title", "Other");
        doc2 = session.createDocument(doc2);
        assertNull(doc2.getParentRef()); // placeless
        session.save();

        // ----- new session -----
        reopenSession();
        doc = session.getDocument(new IdRef(doc.getId()));
        assertNull(doc.getParentRef());

        assertEquals("The title", doc.getProperty("dublincore", "title"));
        assertNull(doc.getProperty("dublincore", "description"));

        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertNull(doc2.getParentRef());

        // remove
        session.removeDocument(doc.getRef());
        session.save();
    }

    @Test
    public void testRelation() throws Exception {
        DocumentModel rel = session.createDocumentModel(null, "myrel", "Relation");
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

        waitForAsyncCompletion(); // before remove
        // remove
        session.removeDocument(rel.getRef());
        session.save();
        list = session.query(query);
        assertEquals(0, list.size());
    }

    /**
     * Checks that a before document modification event can change the DocumentModel name and provoke a rename. And that
     * PREVIOUS_DOCUMENT_MODEL received by the event holds the correct info.
     */
    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-listener-beforemod-contrib.xml")
    public void testBeforeModificationListenerRename() throws Exception {
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

    @Test
    public void testObsoleteType() throws Throwable {
        DocumentRef rootRef = session.getRootDocument().getRef();
        DocumentModel doc = session.createDocumentModel("/", "doc", "MyDocType");
        doc = session.createDocument(doc);
        DocumentRef docRef = new IdRef(doc.getId());
        session.save();
        assertEquals(1, session.getChildren(rootRef).size());
        assertNotNull(session.getDocument(docRef));
        assertNotNull(session.getChild(rootRef, "doc"));

        // remove MyDocType from known types
        DocumentTypeDescriptor dtd = ((SchemaManagerImpl) schemaManager).getDocumentTypeDescriptor("MyDocType");
        ((SchemaManagerImpl) schemaManager).unregisterDocumentType(dtd);

        reopenSession();
        if (!isDBS()) {
            // DBS does not do obsolete type check on hasChildren() due to cost
            assertFalse(session.hasChildren(rootRef));
        }
        assertEquals(0, session.getChildren(rootRef).size());
        try {
            session.getDocument(docRef);
            fail("shouldn't be able to get doc with obsolete type");
        } catch (DocumentNotFoundException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Unknown document type: MyDocType"));
        }
        try {
            session.getChild(rootRef, "doc");
            fail("shouldn't be able to get doc with obsolete type");
        } catch (DocumentNotFoundException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Unknown document type: MyDocType"));
        }
    }

    @Test
    public void testVersionParent() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentRef verRef = session.checkIn(doc.getRef(), null, null);
        DocumentRef parentRef = session.getParentDocumentRef(verRef);
        assertNotNull(parentRef);
        assertEquals(session.getRootDocument().getRef(), parentRef);
        DocumentModel ver = session.getDocument(verRef);
        assertEquals("/file", ver.getPathAsString());
        // now remove live doc
        session.removeDocument(doc.getRef());
        // version parent is now null
        parentRef = session.getParentDocumentRef(verRef);
        assertNull(parentRef);
        DocumentModel parent = session.getParentDocument(verRef);
        assertNull(parent);
        ver = session.getDocument(verRef);
        assertNull(ver.getPathAsString());
    }

    @Test
    public void testRollback() {
        DocumentModel file1 = session.createDocumentModel("/", "file1", "File");
        file1.setPropertyValue("dc:title", "foo");
        file1 = session.createDocument(file1);
        DocumentModel file2 = session.createDocumentModel("/", "file2", "File");
        file2.setPropertyValue("dc:title", "smurf");
        file2 = session.createDocument(file2);
        DocumentModel file3 = session.createDocumentModel("/", "file3", "File");
        file3 = session.createDocument(file3);
        session.save();
        nextTransaction();

        // start some changes
        // modify file1 title
        file1.setPropertyValue("dc:title", "bar");
        session.saveDocument(file1);
        // modify file2 title
        file2.setPropertyValue("dc:title", "zap");
        session.saveDocument(file2);
        // remove file3
        session.removeDocument(file3.getRef());
        // create file4
        DocumentModel file4 = session.createDocumentModel("/", "file4", "File");
        file4 = session.createDocument(file4);
        // create file5
        DocumentModel file5 = session.createDocumentModel("/", "file5", "File");
        file5 = session.createDocument(file5);
        session.save();

        // more changes
        // modify again file1 title
        file1.setPropertyValue("dc:title", "gee");
        session.saveDocument(file1);
        // remove file2
        session.removeDocument(file2.getRef());
        // modify file4
        file4.setPropertyValue("dc:title", "moo");
        session.saveDocument(file4);
        // remove file5
        session.removeDocument(file5.getRef());
        session.save();

        // abort the transaction
        TransactionHelper.setTransactionRollbackOnly();
        nextTransaction();

        // check what we now have
        // file1 title unchanged
        file1 = session.getDocument(file1.getRef());
        assertEquals("foo", file1.getPropertyValue("dc:title"));
        // file2 title unchanged
        file2 = session.getDocument(file2.getRef());
        assertEquals("smurf", file2.getPropertyValue("dc:title"));
        // file3 still present
        assertTrue(session.exists(file3.getRef()));
        // file4 not present
        assertFalse(session.exists(file4.getRef()));
        // file5 not present
        assertFalse(session.exists(file5.getRef()));
    }

    @Test
    public void testRollback2() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file.setPropertyValue("dc:title", "foo");
        file = session.createDocument(file);
        session.save();
        nextTransaction();

        // start some changes
        file.setPropertyValue("dc:title", "bar");
        session.saveDocument(file);

        // abort the transaction
        TransactionHelper.setTransactionRollbackOnly();

        // attempt save in rollback-only state
        session.save();

        // more changes
        file.setPropertyValue("dc:title", "gee");
        session.saveDocument(file);
        session.save();

        nextTransaction();

        // check what we now have file1 title unchanged
        file = session.getDocument(file.getRef());
        assertEquals("foo", file.getPropertyValue("dc:title"));
    }

    @Test
    public void testRollback3() {
        // create file 1
        DocumentModel file1 = session.createDocumentModel("/", "file1", "File");
        file1 = session.createDocument(file1);
        session.save();
        nextTransaction();

        // create file 2 and rollback
        DocumentModel file2 = session.createDocumentModel("/", "file2", "File");
        file2 = session.createDocument(file2);
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        nextTransaction();

        // check just one file is here
        DocumentModelList docs = session.query("SELECT * FROM File");
        assertEquals(1, docs.size());

        // re-try to create file 2 and re-rollback
        file2 = session.createDocumentModel("/", "file2", "File");
        file2 = session.createDocument(file2);
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        nextTransaction();

        // still just one file
        docs = session.query("SELECT * FROM File");
        assertEquals(1, docs.size());
    }

    @Test
    public void testRollback4() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // set rollback-only
        TransactionHelper.setTransactionRollbackOnly();
        // then use the session (first use in this transaction, it will be reconnected)
        try {
            session.getDocument(new PathRef("/"));
            fail("should not allow use of session when marked rollback-only");
        } catch (NuxeoException e) {
            assertEquals("Cannot reconnect a CoreSession when transaction is marked rollback-only", e.getMessage());
        }
    }

    @Test
    public void testRollback5() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // set rollback-only
        TransactionHelper.setTransactionRollbackOnly();
        // then create a session
        try (CoreSession session2 = CoreInstance.openCoreSession(coreFeature.getRepositoryName())) {
            fail("should not allow creation of session when marked rollback-only");
        } catch (NuxeoException e) {
            assertEquals("Cannot create a CoreSession when transaction is marked rollback-only", e.getMessage());
        }
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreWindows.class, cause = "Not enough time granularity")
    public void testBinaryGC() throws Exception {
        // GC binaries from previous tests
        Thread.sleep(3 * 1000);
        runBinariesGC(true, false);

        // store some binaries
        for (String str : Arrays.asList("ABC", "DEF", "GHI", "JKL")) {
            addBinary(str, str);
            addBinary(str, str + "2");
        }
        session.save();
        nextTransaction();

        BinaryManagerStatus status = runBinariesGC(true, false);
        assertEquals(4, status.numBinaries); // ABC, DEF, GHI, JKL
        assertEquals(4 * 3, status.sizeBinaries);
        assertEquals(0, status.numBinariesGC);
        assertEquals(0, status.sizeBinariesGC);

        // remove some binaries
        session.removeDocument(new PathRef("/ABC"));
        session.removeDocument(new PathRef("/ABC2"));
        session.removeDocument(new PathRef("/DEF"));
        session.removeDocument(new PathRef("/DEF2"));
        session.removeDocument(new PathRef("/GHI")); // GHI2 remains
        // JKL and JKL2 remain
        session.save();
        nextTransaction();

        // run GC in non-delete mode
        Thread.sleep(3 * 1000); // sleep before GC to pass its time threshold
        status = runBinariesGC(false, false);
        assertEquals(2, status.numBinaries); // GHI, JKL
        assertEquals(2 * 3, status.sizeBinaries);
        assertEquals(2, status.numBinariesGC); // ABC, DEF
        assertEquals(2 * 3, status.sizeBinariesGC);

        // add a new binary during GC and revive one which was about to die
        status = runBinariesGC(true, true);
        assertEquals(4, status.numBinaries); // DEF3, GHI2, JKL, MNO
        assertEquals(4 * 3, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC); // ABC
        assertEquals(1 * 3, status.sizeBinariesGC);

        Thread.sleep(3 * 1000);
        status = runBinariesGC(true, false);
        assertEquals(4, status.numBinaries); // DEF3, GHI2, JKL, MNO
        assertEquals(4 * 3, status.sizeBinaries);
        assertEquals(0, status.numBinariesGC);
        assertEquals(0, status.sizeBinariesGC);
    }

    protected void addBinary(String content, String name) {
        Blob blob = Blobs.createBlob(content);
        DocumentModel doc = session.createDocumentModel("/", name, "File");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
    }

    protected BinaryManagerStatus runBinariesGC(boolean delete, boolean addDuringGC) {
        BlobProvider blobProvider = blobManager.getBlobProvider(session.getRepositoryName());
        BinaryManager binaryManager = blobProvider.getBinaryManager();
        BinaryGarbageCollector gc = binaryManager.getGarbageCollector();
        Repository repository = repositoryService.getRepository(session.getRepositoryName());

        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        repository.markReferencedBinaries();
        if (addDuringGC) {
            // while GC is in progress, add a new binary
            addBinary("MNO", "MNO");
            // and revive one that was about to be deleted
            // note that this wouldn't work if we didn't recreate the Binary object from an InputStream
            // and reused an old one
            addBinary("DEF", "DEF3");
            session.save();
            nextTransaction();
        }
        gc.stop(delete);
        return gc.getStatus();
    }

    /** Test that stores blobs in attachments (complex list). */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreWindows.class, cause = "Not enough time granularity")
    public void testBinaryGC2() throws Exception {
        // GC binaries from previous tests
        Thread.sleep(3 * 1000);
        runBinariesGC(true, false);

        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        Map<String, Object> abc = Collections.singletonMap("file", Blobs.createBlob("ABC"));
        Map<String, Object> def = Collections.singletonMap("file", Blobs.createBlob("DEF"));
        Map<String, Object> ghi = Collections.singletonMap("file", Blobs.createBlob("GHI"));
        doc.setPropertyValue("files", (Serializable) Arrays.asList(abc, def, ghi));
        doc = session.createDocument(doc);
        session.save();
        // remove GHI
        doc.setPropertyValue("files", (Serializable) Arrays.asList(abc, def));
        doc = session.saveDocument(doc);
        session.save();
        nextTransaction();

        // run GC in non-delete mode
        Thread.sleep(3 * 1000); // sleep before GC to pass its time threshold
        BinaryManagerStatus status = runBinariesGC(false, false);
        assertEquals(2, status.numBinaries); // ABC, DEF
        assertEquals(1, status.numBinariesGC); // GHI

        // actual GC
        status = runBinariesGC(true, false);
        assertEquals(2, status.numBinaries); // ABC, DEF
        assertEquals(1, status.numBinariesGC); // GHI

        // again
        status = runBinariesGC(true, false);
        assertEquals(2, status.numBinaries); // ABC, DEF
        assertEquals(0, status.numBinariesGC);
    }

    @Test
    public void testLocking() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("pete", "WriteProperties"));
        acp.addACL(acl);
        session.setACP(docRef, acp, true);
        session.save();

        Lock lock = session.getLockInfo(docRef);
        assertNull(lock);

        lock = session.setLock(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());
        assertNotNull(lock.getCreated());

        lock = session.getLockInfo(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());

        // nobody can re-lock without unlock, even Administrator
        try {
            session.setLock(docRef);
            fail();
        } catch (LockException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Document already locked"));
        }

        // cannot remove lock as pete
        try (CoreSession peteSession = openSessionAs("pete")) {
            // try to remove lock as pete
            try {
                peteSession.removeLock(docRef);
                fail();
            } catch (LockException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("Document already locked"));
            }
        }

        lock = session.removeLock(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());

        lock = session.getLockInfo(docRef);
        assertNull(lock);

        // removing lock on non-locked doesn't fail
        lock = session.removeLock(docRef);
        assertNull(lock);

        // as pete
        try (CoreSession peteSession = openSessionAs("pete")) {
            // set lock as pete
            lock = peteSession.setLock(docRef);
            assertNotNull(lock);
            assertEquals("pete", lock.getOwner());

            // remove as pete
            lock = peteSession.removeLock(docRef);
            assertNotNull(lock);
        }
    }

    @Test
    public void testLockingBeforeSave() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();

        Lock lock = session.getLockInfo(docRef);
        assertNull(lock);

        lock = session.setLock(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());

        lock = session.removeLock(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());

        lock = session.setLock(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());

        session.save();

        lock = session.getLockInfo(docRef);
        assertNotNull(lock);
        assertEquals("Administrator", lock.getOwner());
    }

    @Test
    public void testChangeToken() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:modified", Calendar.getInstance());
        doc = session.createDocument(doc);
        session.save();
        String token = doc.getChangeToken();

        nextTransaction();
        reopenSession();
        doc = session.getDocument(doc.getRef());
        assertEquals(token, doc.getChangeToken());
    }

}
