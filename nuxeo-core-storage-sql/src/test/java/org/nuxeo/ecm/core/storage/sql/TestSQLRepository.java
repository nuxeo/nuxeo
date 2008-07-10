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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
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
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepository extends SQLRepositoryTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @Override
    protected void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void testBasics() throws Exception {
        DocumentModel root = session.getRootDocument();
        String name = "domain123";
        DocumentModel child = new DocumentModelImpl(root.getPathAsString(),
                name, "Domain");
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
        root = session.getRootDocument();
        child = session.getChild(root.getRef(), name);

        String title = (String) child.getProperty("dublincore", "title");
        assertEquals("The title", title);
        String description = (String) child.getProperty("dublincore",
                "description");
        assertNull(description);
        Calendar modified = (Calendar) child.getProperty("dublincore",
                "modified");
        assertEquals(cal, modified);
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

    @SuppressWarnings( { "SimplifiableJUnitAssertion" })
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

        assertFalse(session.exists(childFolder.getRef()));
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
         * Filter filter = new NameFilter(name2);
         *
         * // get folder childs List<DocumentModel> retrievedChilds =
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
         * Filter filter = new NameFilter(name2);
         *
         * // get folder childs DocumentModelIterator retrievedChilds =
         * session.getChildrenIterator( root.getRef(), null, null, filter);
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

        DocumentModel returnedDocument = session.getDocument(
                childFile.getRef(), new String[] { "common" });

        assertNotNull(returnedDocument);
        assertNotNull(returnedDocument.getRef());
        assertNotNull(returnedDocument.getId());
        assertNotNull(returnedDocument.getName());
        assertNotNull(returnedDocument.getPathAsString());
        assertNotNull(returnedDocument.getType());
        assertNotNull(returnedDocument.getDeclaredSchemas());

        // TODO: should it contain 3 or 1 schemas? not sure about that.
        List<String> schemas = Arrays.asList(returnedDocument.getDeclaredSchemas());
        assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        assertEquals("f1", returnedDocument.getProperty("dublincore", "title"));
        assertEquals("desc 1", returnedDocument.getProperty("dublincore",
                "description"));
        assertNull(returnedDocument.getProperty("file", "filename"));

        returnedDocument = session.getDocument(childFile.getRef(),
                new String[] { "common", "file" });

        assertNotNull(returnedDocument);
        assertNotNull(returnedDocument.getRef());
        assertNotNull(returnedDocument.getId());
        assertNotNull(returnedDocument.getName());
        assertNotNull(returnedDocument.getPathAsString());
        assertNotNull(returnedDocument.getType());
        assertNotNull(returnedDocument.getDeclaredSchemas());

        schemas = Arrays.asList(returnedDocument.getDeclaredSchemas());
        assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        assertEquals("f1", returnedDocument.getProperty("dublincore", "title"));
        assertEquals("desc 1", returnedDocument.getProperty("dublincore",
                "description"));
        assertEquals("second name", returnedDocument.getProperty("file",
                "filename"));
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

    public void testQuery() throws ClientException {
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
        List<String> schemas = Arrays.asList(docModel.getDeclaredSchemas());
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
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
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
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all files using the filter, we should get only one
        Filter facetFilter = new FacetFilter("HiddenInNavigation", true);
        list = session.query("SELECT * FROM HiddenFile", facetFilter);
        assertEquals(1, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all documents, we get the folder and the two files
        list = session.query("SELECT * FROM Document");
        assertEquals(3, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
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

    public void testQueryAfterEdit() throws ClientException, IOException {
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

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(folderChildFile);
        childDocs.add(folderChildFile2);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());
        assertEquals(name3, returnedChildDocs.get(2).getName());
        assertEquals(name4, returnedChildDocs.get(3).getName());

        DocumentRef[] refs = { returnedChildDocs.get(0).getRef(),
                returnedChildDocs.get(1).getRef(),
                returnedChildDocs.get(2).getRef(),
                returnedChildDocs.get(3).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(3).getRef()));
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

        List<DocumentModel> childDocs = new ArrayList<DocumentModel>();
        childDocs.add(childFolder);
        childDocs.add(folderChildFile);
        childDocs.add(folderChildFile2);
        childDocs.add(childFile);

        List<DocumentModel> returnedChildDocs = createChildDocuments(childDocs);

        assertEquals(name, returnedChildDocs.get(0).getName());
        assertEquals(name2, returnedChildDocs.get(1).getName());
        assertEquals(name3, returnedChildDocs.get(2).getName());
        assertEquals(name4, returnedChildDocs.get(3).getName());

        // here's the different ordering
        DocumentRef[] refs = { returnedChildDocs.get(1).getRef(),
                returnedChildDocs.get(0).getRef(),
                returnedChildDocs.get(3).getRef(),
                returnedChildDocs.get(2).getRef() };
        session.removeDocuments(refs);

        assertFalse(session.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(session.exists(returnedChildDocs.get(3).getRef()));
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
        assertEquals("desc 1", childFolder.getProperty("dublincore",
                "description"));
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
        assertEquals("desc 1", retrievedFile.getProperty("dublincore",
                "description"));
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

    public void testGetVersionsForDocument() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        version.setDescription("d1");
        // only label and description are currently supported
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);

        session.save();
        session.checkIn(childFile.getRef(), version);

        List<VersionModel> versions = session.getVersionsForDocument(childFile.getRef());

        assertNotNull(versions);
        assertEquals(1, versions.size());
        assertNotNull(versions.get(0));
        assertNotNull(versions.get(0));
        assertEquals("v1", versions.get(0).getLabel());
        assertEquals("d1", versions.get(0).getDescription());
        // only label and descriptions are currently supported
        // assertEquals(cal.getTime().getTime(),
        // versions.get(0).getCreated().getTime().getTime());

        // creating a second version without description
        session.checkOut(childFile.getRef());
        VersionModel version2 = new VersionModelImpl();
        version2.setLabel("v2");

        session.save();
        session.checkIn(childFile.getRef(), version2);

        List<VersionModel> versions2 = session.getVersionsForDocument(childFile.getRef());

        assertNotNull(versions2);
        assertEquals(2, versions2.size());
        assertNotNull(versions2.get(0));
        assertNotNull(versions2.get(0));
        assertEquals("v1", versions2.get(0).getLabel());
        assertEquals("d1", versions2.get(0).getDescription());
        assertNotNull(versions2.get(1));
        assertNotNull(versions2.get(1));
        assertEquals("v2", versions2.get(1).getLabel());
        assertNull(versions2.get(1).getDescription());
    }

    // TODO: fix and reenable SF 2007/05/23
    public void XXXtestRestoreToVersion() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setDescription("d1");
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);

        session.save();
        session.checkIn(childFile.getRef(), version);

        assertFalse(session.isCheckedOut(childFile.getRef()));

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        version = new VersionModelImpl();
        version.setLabel("v2");

        session.checkOut(childFile.getRef());

        assertTrue(session.isCheckedOut(childFile.getRef()));

        session.saveDocument(childFile);
        session.save();
        session.checkIn(childFile.getRef(), version);

        DocumentModel newDoc = session.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        version.setLabel("v1");
        DocumentModel restoredDoc = session.restoreToVersion(
                childFile.getRef(), version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        version.setLabel("v2");
        restoredDoc = session.restoreToVersion(childFile.getRef(), version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertEquals("second name", restoredDoc.getProperty("file", "filename"));
    }

    public void testCheckIn() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setCreated(Calendar.getInstance());

        session.save();

        session.checkIn(childFile.getRef(), version);
    }

    // TODO: fix and reenable SF 2007/05/23
    public void XXXtestCheckOut() throws ClientException {
        // restore to versions does checkout in order to submit more than one
        // version and to restore afterwards, so its a worthy test
        XXXtestRestoreToVersion();
    }

    public void testGetCheckedOut() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setCreated(Calendar.getInstance());

        session.save();

        session.checkIn(childFile.getRef(), version);
        assertFalse(session.isCheckedOut(childFile.getRef()));
        session.checkOut(childFile.getRef());
        assertTrue(session.isCheckedOut(childFile.getRef()));
    }

    // TODO: fix and reenable SF 2007/05/23
    public void XXXtestGetDocumentWithVersion() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setDescription("d1");
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);

        session.save();
        session.checkIn(childFile.getRef(), version);

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        version = new VersionModelImpl();
        version.setLabel("v2");

        session.checkOut(childFile.getRef());
        session.saveDocument(childFile);
        session.save();
        session.checkIn(childFile.getRef(), version);

        DocumentModel newDoc = session.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        version.setLabel("v1");

        DocumentModel restoredDoc = session.restoreToVersion(
                childFile.getRef(), version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        version.setLabel("v2");
        // TODO: this fails as there is a NPE because a document version does
        // not currently have a document type
        restoredDoc = session.getDocumentWithVersion(childFile.getRef(),
                version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertEquals("second name", restoredDoc.getProperty("file", "filename"));
    }

    public void xxxtestGetAvailableSecurityPermissions() throws ClientException {
        List<String> permissions = session.getAvailableSecurityPermissions();

        // TODO
        assertTrue(permissions.contains("Everything"));
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

    // TODO: Fix this test!
    public void XXXtestGetContentData() throws ClientException {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");
        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("file", "content", new StringBlob("the content"));

        session.saveDocument(childFile);

        /*
         * this block is commented because the exception it generates makes the
         * next block fail try { session.getContentData(childFile.getRef(),
         * "title"); fail("Content nodes must be of type: content"); } catch
         * (ClientException e) { // do nothing }
         */

        byte[] content = session.getContentData(childFile.getRef(), "content");
        assertNotNull(content);

        String strContent = String.valueOf(content);

        assertNotNull(strContent);
        assertEquals("the content", strContent);
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

    public void testFacets() throws Exception {
        DocumentModel root = session.getRootDocument();

        assertTrue(root.isFolder());

        String name = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name, "File");
        String name2 = "file#" + generateUnique();
        DocumentModel childFile2 = new DocumentModelImpl(
                root.getPathAsString(), name2, "Folder");

        String name3 = "file#" + generateUnique();
        DocumentModel childFile3 = new DocumentModelImpl(
                root.getPathAsString(), name3, "Workspace");

        List<DocumentModel> childFiles = new ArrayList<DocumentModel>();
        childFiles.add(childFile);
        childFiles.add(childFile2);
        childFiles.add(childFile3);

        List<DocumentModel> returnedChildFiles = createChildDocuments(childFiles);

        assertFalse(returnedChildFiles.get(0).isFolder());
        assertTrue(returnedChildFiles.get(1).isFolder());
        assertTrue(returnedChildFiles.get(2).isFolder());
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
        assertEquals(2, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("approve"));
        assertTrue(allowedStateTransitions.contains("obsolete"));

        assertTrue(session.followTransition(childFile.getRef(), "approve"));
        assertEquals("approved",
                session.getCurrentLifeCycleState(childFile.getRef()));
        allowedStateTransitions = session.getAllowedStateTransitions(childFile.getRef());
        assertEquals(0, allowedStateTransitions.size());

        assertEquals("default", session.getLifeCyclePolicy(childFile.getRef()));

        session.cancel();
        assertFalse(session.exists(childFile.getRef()));
    }

    // TODO: fix and reenable SF 2007/05/23
    public void testDataModelLifeCycleAPI() throws ClientException {
        DocumentModel root = session.getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        childFile = createChildDocument(childFile);

        assertEquals("default", childFile.getLifeCyclePolicy());
        assertEquals("project", childFile.getCurrentLifeCycleState());

        Collection<String> allowedStateTransitions = childFile.getAllowedStateTransitions();
        assertEquals(2, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("approve"));
        assertTrue(allowedStateTransitions.contains("obsolete"));

        assertTrue(childFile.followTransition("obsolete"));
        assertEquals("obsolete", childFile.getCurrentLifeCycleState());

        allowedStateTransitions = childFile.getAllowedStateTransitions();
        assertEquals(0, allowedStateTransitions.size());
        assertEquals("default", childFile.getLifeCyclePolicy());

        session.cancel();
        assertFalse(session.exists(childFile.getRef()));
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

        VersionModel version = new VersionModelImpl();
        version.setCreated(Calendar.getInstance());
        version.setLabel("v1");
        session.checkIn(file.getRef(), version);
        session.save();

        // create a proxy in folder2
        DocumentModel proxy = session.createProxy(folder2.getRef(),
                file.getRef(), version, true);
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

        session.move(file.getRef(), folder2.getRef(), null); // move using orig
        // name

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

        session.cancel();
    }

    // TODO: fix this test
    public void XXXtestScalarList() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        String[] str = new String[] { "a", "b", "c" };
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

        byte[] bytes = FileUtils.readBytes(Blob.class.getResourceAsStream("TestAPI.class"));
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

    /**
     * This test should be done on a repo that contains deprecated blob node
     * types (nt:resource blobs) You should specify the File document UID to
     * test
     *
     * @throws Exception
     */
    public void xxx_testBlobCompat() throws Exception {
        String UID = "6c0f8723-25b2-4a28-a93f-d03096057b92";
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        DocumentModel doc = session.getDocument(new IdRef(UID));
        Blob blob = (Blob) doc.getProperty("file", "content");
        String digest = blob.getDigest();
        String filename = blob.getFilename();
        long length = blob.getLength();
        String mimetype = blob.getMimeType();
        String encoding = blob.getEncoding();
        byte[] content = blob.getByteArray();
        assertNull(digest);
        assertNull(filename);

        Blob b2 = StreamingBlob.createFromByteArray(content);
        b2.setDigest("XXX");
        b2.setFilename("blob.txt");
        b2.setMimeType(mimetype);
        b2.setEncoding(encoding);
        length = b2.getLength();
        doc.setProperty("file", "content", b2);
        session.saveDocument(doc);

        session.getDocument(doc.getRef());
        b2 = (Blob) doc.getProperty("file", "content");
        assertEquals("XXX", b2.getDigest());
        assertEquals("blob.txt", b2.getFilename());
        assertEquals(length, b2.getLength());
        assertEquals(encoding, b2.getEncoding());
        assertTrue(Arrays.equals(content, b2.getByteArray()));
        assertEquals(mimetype, b2.getMimeType());

    }

    public void xxx__testUploadBigBlob() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        session.save();

        byte[] bytes = FileUtils.readBytes(Blob.class.getResourceAsStream("/blob.mp3"));
        Blob blob = new ByteArrayBlob(bytes, "audio/mpeg");

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        childFile.setProperty("file", "content", blob);

        session.saveDocument(childFile);
        session.save();
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

    public void testLock() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");

        folder1 = createChildDocument(folder1);
        assertNull(folder1.getLock());
        assertFalse(folder1.isLocked());
        folder1.setLock("bstefanescu");
        assertEquals("bstefanescu", folder1.getLock());
        assertTrue(folder1.isLocked());

        folder1 = session.getChild(root.getRef(), "folder1");
        assertEquals("bstefanescu", folder1.getLock());
        assertTrue(folder1.isLocked());

        folder1.unlock();
        assertNull(folder1.getLock());
        assertFalse(folder1.isLocked());
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

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        version.setDescription("d1");
        // only label and description are currently supported
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);

        session.save();
        session.checkIn(childFile.getRef(), version);

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
        assertEquals("default", childFile.getRepositoryName());
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
        session.saveDocumentAsNewVersion(file);

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

        DocumentPart dp = doc.getPart("MySchema");
        Property p = dp.get("long");

        assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(12);
        assertEquals(new Long(12), p.getValue());
        session.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        assertFalse(p.isPhantom());
        assertEquals(new Long(12), p.getValue());
        p.setValue(null);
        assertFalse(p.isPhantom());
        assertNull(p.getValue());

        session.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        // assertTrue(p.isPhantom());
        assertNull(p.getValue());
        p.setValue(new Long(13));
        p.remove();
        assertTrue(p.isRemoved());
        assertNull(p.getValue());

        session.saveDocument(doc);

        dp = doc.getPart("MySchema");
        p = dp.get("long");
        assertTrue(p.isPhantom());
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

    public static byte[] createBytes(int size, byte val) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, val);
        return bytes;
    }

    @SuppressWarnings("unchecked")
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

        // test that reset works
        blob.getStream().reset();

        blob = (Blob) doc.getPart("file").get("content").getValue();
        assertTrue(Arrays.equals(bytes, blob.getByteArray()));

    }

    public void testProxy() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "proxy_test", "File");

        doc = session.createDocument(doc);
        doc.setProperty("common", "title", "the title");
        doc = session.saveDocument(doc);
        // session.save();

        VersionModel version = new VersionModelImpl();
        version.setCreated(Calendar.getInstance());
        version.setLabel("v1");
        session.checkIn(doc.getRef(), version);
        // session.save();

        // checkout the doc to modify it
        session.checkOut(doc.getRef());
        doc.setProperty("common", "title", "the title modified");
        doc = session.saveDocument(doc);
        // session.save();

        DocumentModel proxy = session.createProxy(root.getRef(), doc.getRef(),
                version, true);
        // session.save();
        // assertEquals("the title", proxy.getProperty("common", "title"));
        // assertEquals("the title modified", doc.getProperty("common",
        // "title"));

        // make another new version
        VersionModel version2 = new VersionModelImpl();
        version2.setCreated(Calendar.getInstance());
        version2.setLabel("v2");
        session.checkIn(doc.getRef(), version2);
        // session.save();

        DocumentModelList list = session.getChildren(root.getRef());
        assertEquals(2, list.size());

        for (DocumentModel model : list) {
            assertEquals("File", model.getType());
        }

        session.removeDocument(proxy.getRef());
        // session.save();

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

        // republish a proxy
        DocumentModel proxy2 = session.publishDocument(proxy, folder);
        session.save();
        assertTrue(proxy2.isProxy());
        assertEquals(1, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());

        // a second time to check overwrite
        // XXX this test fails for mysterious reasons (hasNode doesn't detect
        // the child node that was added by the first copy -- XASession pb?)
        // session.publishDocument(proxy, folder);
        // session.save();
        // assertEquals(1, session.getChildrenRefs(folder.getRef(),
        // null).size());
        // assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());

        // and without overwrite
        session.publishDocument(proxy, folder, false);
        session.save();
        assertEquals(2, session.getChildrenRefs(folder.getRef(), null).size());
        assertEquals(3, session.getChildrenRefs(root.getRef(), null).size());
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

}
