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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.adapter.AnnotatedDocument;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeComparator;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;

/**
 * @author Razvan Caraghin
 *
 */
public abstract class TestAPI extends TestConnection {
    /*
     * if: java.io.InvalidClassException: javax.resource.ResourceException;
     * local class incompatible: stream classdesc serialVersionUID see
     * http://www
     * .jboss.com/index.html?module=bb&op=viewtopic&t=65840&view=previous
     * probably this is because too many opened JCR sessions
     */

    protected final Random random = new Random(new Date().getTime());

    @Override
    protected void tearDown() throws Exception {
        cleanUp(getRootDocument().getRef());
        closeSession();
        super.tearDown();
    }

    protected String generateUnique() {
        return String.valueOf(random.nextLong());
    }

    protected DocumentModel getRootDocument() throws ClientException {
        DocumentModel root = remote.getRootDocument();

        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getRef());
        assertNotNull(root.getPathAsString());

        return root;
    }

    protected DocumentModel createChildDocument(DocumentModel childFolder)
            throws ClientException {

        DocumentModel ret = remote.createDocument(childFolder);

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
                remote.createDocument(childFolders.toArray(new DocumentModel[0])));

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

    protected void cleanUp(DocumentRef ref) throws ClientException {
        remote.removeChildren(ref);
        remote.save();
    }

    public void testCancel() throws ClientException {
        DocumentModel root = getRootDocument();

        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), "folder#" + generateUnique(), "Folder");
        childFolder = createChildDocument(childFolder);

        remote.cancel();

        assertFalse(remote.exists(childFolder.getRef()));
    }

    public void testCreateDomainDocumentRefDocumentModel()
            throws ClientException {
        DocumentModel root = getRootDocument();

        String name = "domain#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Domain");
        childFolder = createChildDocument(childFolder);

        assertEquals("Domain", childFolder.getType());
        assertEquals(name, childFolder.getName());
    }

    public void testCreateFolderDocumentRefDocumentModel()
            throws ClientException {
        DocumentModel root = getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        assertEquals("Folder", childFolder.getType());
        assertEquals(name, childFolder.getName());
    }

    public void testCreateFileDocumentRefDocumentModel() throws ClientException {
        DocumentModel root = getRootDocument();

        String name = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name, "File");

        childFile = createChildDocument(childFile);

        assertEquals("File", childFile.getType());
        assertEquals(name, childFile.getName());
    }

    public void testCreateFolderDocumentRefDocumentModelArray()
            throws ClientException {
        DocumentModel root = getRootDocument();

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
        DocumentModel root = getRootDocument();

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
        DocumentModel root = getRootDocument();

        assertTrue(remote.exists(root.getRef()));
    }

    public void testGetChild() throws ClientException {
        DocumentModel root = getRootDocument();

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

        DocumentModel retrievedChild = remote.getChild(root.getRef(), name);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());

        assertEquals(name, retrievedChild.getName());

        retrievedChild = remote.getChild(root.getRef(), name2);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());

        assertEquals(name2, retrievedChild.getName());
    }

    public void testGetChildrenDocumentRef() throws ClientException {
        DocumentModel root = getRootDocument();

        List<DocumentModel> docs = remote.getChildren(root.getRef());

        assertEquals(0, docs.size());
    }

    public void testGetChildrenDocumentRef2() throws ClientException {
        DocumentModel root = getRootDocument();

        DocumentModelIterator docs = remote.getChildrenIterator(root.getRef());

        assertFalse(docs.hasNext());
    }

    public void testGetFileChildrenDocumentRefString() throws ClientException {
        DocumentModel root = getRootDocument();

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
        List<DocumentModel> retrievedChilds = remote.getChildren(root.getRef(),
                "File");

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
        DocumentModel root = getRootDocument();

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
        DocumentModelIterator retrievedChilds = remote.getChildrenIterator(
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
        DocumentModel root = getRootDocument();

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
        List<DocumentModel> retrievedChilds = remote.getChildren(root.getRef(),
                "Folder");

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
        DocumentModel root = getRootDocument();

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
        DocumentModelIterator retrievedChilds = remote.getChildrenIterator(
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
        DocumentModel root = getRootDocument();

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

        Filter filter = new NameFilter(name2);

        // get folder childs
        List<DocumentModel> retrievedChilds = remote.getChildren(root.getRef(),
                null, null, filter, null);

        assertNotNull(retrievedChilds);
        assertEquals(1, retrievedChilds.size());

        assertNotNull(retrievedChilds.get(0));
        assertNotNull(retrievedChilds.get(0).getId());
        assertNotNull(retrievedChilds.get(0).getName());
        assertNotNull(retrievedChilds.get(0).getPathAsString());
        assertNotNull(retrievedChilds.get(0).getRef());

        assertEquals(name2, retrievedChilds.get(0).getName());
    }

    // FIXME: same as testGetChildrenDocumentRefStringFilter. Remove?
    public void testGetChildrenDocumentRefStringFilter2()
            throws ClientException {
        DocumentModel root = getRootDocument();

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

        Filter filter = new NameFilter(name2);

        // get folder childs
        DocumentModelIterator retrievedChilds = remote.getChildrenIterator(
                root.getRef(), null, null, filter);

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
    }

    /**
     * Test for NXP-741: Search based getChildren.
     *
     * @throws ClientException
     */
    public void testGetChildrenInFolderWithSearch() throws ClientException {
        DocumentModel root = getRootDocument();

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
        DocumentModel root = getRootDocument();

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

        DocumentModel doc = remote.getDocument(returnedChildDocs.get(0).getRef());

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
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        remote.saveDocument(childFile);
        remote.save();

        DocumentModel returnedDocument = remote.getDocument(childFile.getRef(),
                new String[] { "common" });

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

        returnedDocument = remote.getDocument(childFile.getRef(), new String[] {
                "common", "file" });

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
        DocumentModel root = getRootDocument();

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
        List<DocumentModel> retrievedChilds = remote.getFiles(root.getRef());

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
        DocumentModel root = getRootDocument();

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
        DocumentModelIterator retrievedChilds = remote.getFilesIterator(root.getRef());

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
        DocumentModel root = getRootDocument();

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
        List<DocumentModel> retrievedChilds = remote.getFolders(root.getRef());

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
        DocumentModel root = getRootDocument();

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
        DocumentModelIterator retrievedChilds = remote.getFoldersIterator(root.getRef());

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
        DocumentModel root = getRootDocument();

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

        DocumentModel shouldBeRoot = remote.getParentDocument(returnedChildDocs.get(
                0).getRef());

        assertEquals(root.getPathAsString(), shouldBeRoot.getPathAsString());
    }

    public void testGetRootDocument() throws ClientException {
        getRootDocument();
    }

    public void testHasChildren() throws ClientException {
        DocumentModel root = getRootDocument();

        // the root document at the moment has no children
        assertFalse(remote.hasChildren(root.getRef()));
    }

    public void testRemoveChildren() throws ClientException {
        DocumentModel root = getRootDocument();

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

        remote.removeChildren(root.getRef());

        assertFalse(remote.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(1).getRef()));
    }

    public void testRemoveDocument() throws ClientException {
        DocumentModel root = getRootDocument();

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

        remote.removeDocument(returnedChildDocs.get(0).getRef());

        assertFalse(remote.exists(returnedChildDocs.get(0).getRef()));
    }

    public void testQuery() throws ClientException {
        DocumentModel root = getRootDocument();

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

        remote.saveDocuments(returnedChildDocs.toArray(new DocumentModel[0]));
        remote.save();

        DocumentModelList list = remote.query("SELECT * FROM File");
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
        list = remote.query("SELECT * FROM File");
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
        list = remote.query("SELECT * FROM File");
        assertEquals(1, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("file"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all files using the filter, we should get only one
        Filter facetFilter = new FacetFilter("HiddenInNavigation", true);
        list = remote.query("SELECT * FROM HiddenFile", facetFilter);
        assertEquals(1, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("dublincore"));

        // if we select all documents, we get the folder and the two files
        list = remote.query("SELECT * FROM Document");
        assertEquals(3, list.size());
        docModel = list.get(0);
        schemas = Arrays.asList(docModel.getDeclaredSchemas());
        // assertEquals(3, schemas.size());
        assertTrue(schemas.contains("common"));
        assertTrue(schemas.contains("dublincore"));

        list = remote.query("SELECT * FROM Document WHERE dc:title = 'abc'");
        assertEquals(1, list.size());

        list = remote.query("SELECT * FROM Document WHERE dc:title = 'abc' OR dc:title = 'def'");
        assertEquals(2, list.size());

        remote.removeDocument(returnedChildDocs.get(0).getRef());
        remote.removeDocument(returnedChildDocs.get(1).getRef());
    }

    public void testQueryAfterEdit() throws ClientException, IOException {
        DocumentModel root = getRootDocument();

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

        remote.saveDocument(childFile1);
        remote.save();

        DocumentModelList list;
        DocumentModel docModel;

        list = remote.query("SELECT * FROM Document");
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
        remote.saveDocument(docModel);
        remote.save();

        list = remote.query("SELECT * FROM Document");
        assertEquals(1, list.size());
        docModel = list.get(0);

        remote.removeDocument(docModel.getRef());
    }

    public void testRemoveDocuments() throws ClientException {
        DocumentModel root = getRootDocument();

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
        remote.removeDocuments(refs);

        assertFalse(remote.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(1).getRef()));

    }

    /*
     * case where some documents are actually children of other ones from the
     * list
     */
    public void testRemoveDocumentsWithDeps() throws ClientException {
        DocumentModel root = getRootDocument();

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
        remote.removeDocuments(refs);

        assertFalse(remote.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(3).getRef()));
    }

    /*
     * Same as testRemoveDocumentWithDeps with a different given ordering of
     * documents to delete
     */
    public void testRemoveDocumentsWithDeps2() throws ClientException {
        DocumentModel root = getRootDocument();

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
        remote.removeDocuments(refs);

        assertFalse(remote.exists(returnedChildDocs.get(0).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(1).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(2).getRef()));
        assertFalse(remote.exists(returnedChildDocs.get(3).getRef()));
    }

    public void testSave() throws ClientException {
        DocumentModel root = getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(remote.exists(childFolder.getRef()));
        assertTrue(remote.exists(childFile.getRef()));
    }

    public void testSaveFolder() throws ClientException {
        DocumentModel root = getRootDocument();

        String name = "folder#" + generateUnique();

        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        childFolder.setProperty("dublincore", "title", "f1");
        childFolder.setProperty("dublincore", "description", "desc 1");

        remote.saveDocument(childFolder);

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(remote.exists(childFolder.getRef()));

        assertEquals("f1", childFolder.getProperty("dublincore", "title"));
        assertEquals("desc 1", childFolder.getProperty("dublincore",
                "description"));
    }

    public void testSaveFile() throws ClientException {
        DocumentModel root = getRootDocument();

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
        assertTrue(remote.exists(childFile.getRef()));

        DocumentModel retrievedFile = remote.getDocument(childFile.getRef());

        assertEquals("f1", retrievedFile.getProperty("dublincore", "title"));
        assertEquals("desc 1", retrievedFile.getProperty("dublincore",
                "description"));
        assertEquals("filename1", retrievedFile.getProperty("file", "filename"));
    }

    public void testSaveDocuments() throws ClientException {
        DocumentModel root = getRootDocument();

        String name = "folder#" + generateUnique();
        DocumentModel childFolder = new DocumentModelImpl(
                root.getPathAsString(), name, "Folder");
        childFolder = createChildDocument(childFolder);

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        DocumentModel[] docs = { childFolder, childFile };

        remote.saveDocuments(docs);

        // TODO: this should be tested across sessions - when it can be done
        assertTrue(remote.exists(childFolder.getRef()));
        assertTrue(remote.exists(childFile.getRef()));
    }

    public void testGetVersionsForDocument() throws ClientException {
        DocumentModel root = getRootDocument();

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

        remote.save();
        remote.checkIn(childFile.getRef(), version);

        List<VersionModel> versions = remote.getVersionsForDocument(childFile.getRef());

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
        remote.checkOut(childFile.getRef());
        VersionModel version2 = new VersionModelImpl();
        version2.setLabel("v2");

        remote.save();
        remote.checkIn(childFile.getRef(), version2);

        List<VersionModel> versions2 = remote.getVersionsForDocument(childFile.getRef());

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
        DocumentModel root = getRootDocument();

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

        remote.save();
        remote.checkIn(childFile.getRef(), version);

        assertFalse(remote.isCheckedOut(childFile.getRef()));

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        version = new VersionModelImpl();
        version.setLabel("v2");

        remote.checkOut(childFile.getRef());

        assertTrue(remote.isCheckedOut(childFile.getRef()));

        remote.saveDocument(childFile);
        remote.save();
        remote.checkIn(childFile.getRef(), version);

        DocumentModel newDoc = remote.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        version.setLabel("v1");
        DocumentModel restoredDoc = remote.restoreToVersion(childFile.getRef(),
                version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        version.setLabel("v2");
        restoredDoc = remote.restoreToVersion(childFile.getRef(), version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertEquals("second name", restoredDoc.getProperty("file", "filename"));
    }

    public void testCheckIn() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setCreated(Calendar.getInstance());

        remote.save();

        remote.checkIn(childFile.getRef(), version);
    }

    // TODO: fix and reenable SF 2007/05/23
    public void XXXtestCheckOut() throws ClientException {
        // restore to versions does checkout in order to submit more than one
        // version and to restore afterwards, so its a worthy test
        XXXtestRestoreToVersion();
    }

    public void testGetCheckedOut() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setCreated(Calendar.getInstance());

        remote.save();

        remote.checkIn(childFile.getRef(), version);
        assertFalse(remote.isCheckedOut(childFile.getRef()));
        remote.checkOut(childFile.getRef());
        assertTrue(remote.isCheckedOut(childFile.getRef()));
    }

    // TODO: fix and reenable SF 2007/05/23
    public void XXXtestGetDocumentWithVersion() throws ClientException {
        DocumentModel root = getRootDocument();

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

        remote.save();
        remote.checkIn(childFile.getRef(), version);

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        version = new VersionModelImpl();
        version.setLabel("v2");

        remote.checkOut(childFile.getRef());
        remote.saveDocument(childFile);
        remote.save();
        remote.checkIn(childFile.getRef(), version);

        DocumentModel newDoc = remote.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        version.setLabel("v1");

        DocumentModel restoredDoc = remote.restoreToVersion(childFile.getRef(),
                version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        version.setLabel("v2");
        // TODO: this fails as there is a NPE because a document version does
        // not currently have a document type
        restoredDoc = remote.getDocumentWithVersion(childFile.getRef(), version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertEquals("second name", restoredDoc.getProperty("file", "filename"));
    }

    public void xxxtestGetAvailableSecurityPermissions() throws ClientException {
        List<String> permissions = remote.getAvailableSecurityPermissions();

        // TODO
        assertTrue(permissions.contains("Everything"));
    }

    public void testGetDataModel() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        remote.saveDocument(childFile);

        DataModel dm = remote.getDataModel(childFile.getRef(), "dublincore");

        assertNotNull(dm);
        assertNotNull(dm.getMap());
        assertNotNull(dm.getSchema());
        assertEquals("dublincore", dm.getSchema());
        assertEquals("f1", dm.getData("title"));
        assertEquals("desc 1", dm.getData("description"));

        dm = remote.getDataModel(childFile.getRef(), "file");

        assertNotNull(dm);
        assertNotNull(dm.getMap());
        assertNotNull(dm.getSchema());
        assertEquals("file", dm.getSchema());
        assertEquals("second name", dm.getData("filename"));
    }

    public void testGetDataModelField() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        remote.saveDocument(childFile);

        assertEquals("f1", remote.getDataModelField(childFile.getRef(),
                "dublincore", "title"));
        assertEquals("desc 1", remote.getDataModelField(childFile.getRef(),
                "dublincore", "description"));
        assertEquals("second name", remote.getDataModelField(
                childFile.getRef(), "file", "filename"));
    }

    public void testGetDataModelFields() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");

        remote.saveDocument(childFile);

        String[] fields = { "title", "description" };

        Object[] values = remote.getDataModelFields(childFile.getRef(),
                "dublincore", fields);

        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals("f1", values[0]);
        assertEquals("desc 1", values[1]);

        String[] fields2 = { "filename" };

        values = remote.getDataModelFields(childFile.getRef(), "file", fields2);

        assertNotNull(values);
        assertEquals(1, values.length);
        assertEquals("second name", values[0]);
    }

    // TODO: Fix this test!
    public void XXXtestGetContentData() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");
        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("file", "content", new StringBlob("the content"));

        remote.saveDocument(childFile);

        /*
         * this block is commented because the exception it generates makes the
         * next block fail try { remote.getContentData(childFile.getRef(),
         * "title"); fail("Content nodes must be of type: content"); } catch
         * (ClientException e) { // do nothing }
         */

        byte[] content = remote.getContentData(childFile.getRef(), "content");
        assertNotNull(content);

        String strContent = String.valueOf(content);

        assertNotNull(strContent);
        assertEquals("the content", strContent);
    }

    public void testDocumentReferenceEqualityDifferentInstances()
            throws ClientException {
        DocumentModel root = getRootDocument();

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

        DocumentModel retrievedChild = remote.getChild(root.getRef(), name);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());
        assertEquals(name, retrievedChild.getName());

        assertEquals(root.getRef(), retrievedChild.getParentRef());

        retrievedChild = remote.getChild(root.getRef(), name2);

        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getId());
        assertNotNull(retrievedChild.getPathAsString());
        assertNotNull(retrievedChild.getName());
        assertNotNull(retrievedChild.getRef());

        assertEquals(name2, retrievedChild.getName());
        assertEquals(root.getRef(), retrievedChild.getParentRef());
    }

    @SuppressWarnings( { "SimplifiableJUnitAssertion" })
    public void testDocumentReferenceEqualitySameInstance()
            throws ClientException {
        DocumentModel root = getRootDocument();

        assertTrue(root.getRef().equals(root.getRef()));
    }

    public void testDocumentReferenceNonEqualityDifferentInstances()
            throws ClientException {
        DocumentModel root = getRootDocument();

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

        DocumentModel retrievedChild = remote.getChild(root.getRef(), name);

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
        DocumentModel root = getRootDocument();

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
        DocumentModel root = getRootDocument();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        childFile = createChildDocument(childFile);

        assertEquals("default", remote.getLifeCyclePolicy(childFile.getRef()));

        assertEquals("project",
                remote.getCurrentLifeCycleState(childFile.getRef()));

        Collection<String> allowedStateTransitions = remote.getAllowedStateTransitions(childFile.getRef());
        assertEquals(2, allowedStateTransitions.size());
        assertTrue(allowedStateTransitions.contains("approve"));
        assertTrue(allowedStateTransitions.contains("obsolete"));

        assertTrue(remote.followTransition(childFile.getRef(), "approve"));
        assertEquals("approved",
                remote.getCurrentLifeCycleState(childFile.getRef()));
        allowedStateTransitions = remote.getAllowedStateTransitions(childFile.getRef());
        assertEquals(0, allowedStateTransitions.size());

        assertEquals("default", remote.getLifeCyclePolicy(childFile.getRef()));

        remote.cancel();
        assertFalse(remote.exists(childFile.getRef()));
    }

    // TODO: fix and reenable SF 2007/05/23
    public void testDataModelLifeCycleAPI() throws ClientException {
        DocumentModel root = getRootDocument();
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

        remote.cancel();
        assertFalse(remote.exists(childFile.getRef()));
    }

    public void testCopy() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");

        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(),
                "folder2", "Folder");

        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(),
                "file", "File");

        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        file = createChildDocument(file);

        remote.save();

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertFalse(remote.exists(new PathRef("folder2/file")));

        // copy using orig name
        DocumentModel copy1 = remote.copy(file.getRef(), folder2.getRef(), null);

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertTrue(remote.exists(new PathRef("folder2/file")));
        assertFalse(remote.exists(new PathRef("folder2/fileCopy")));
        assertTrue(remote.getChildren(folder2.getRef()).contains(copy1));

        // copy using another name
        DocumentModel copy2 = remote.copy(file.getRef(), folder2.getRef(),
                "fileCopy");

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertTrue(remote.exists(new PathRef("folder2/file")));
        assertTrue(remote.exists(new PathRef("folder2/fileCopy")));
        assertTrue(remote.getChildren(folder2.getRef()).contains(copy2));

        // copy again to same space
        DocumentModel copy3 = remote.copy(file.getRef(), folder2.getRef(), null);

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertTrue(remote.exists(new PathRef("folder2/file")));
        assertTrue(remote.getChildren(folder2.getRef()).contains(copy3));
        assertNotSame(copy1.getName(), copy3.getName());

        // copy again again to same space
        DocumentModel copy4 = remote.copy(file.getRef(), folder2.getRef(), null);

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertTrue(remote.exists(new PathRef("folder2/file")));
        assertTrue(remote.getChildren(folder2.getRef()).contains(copy4));
        assertNotSame(copy1.getName(), copy4.getName());
        assertNotSame(copy3.getName(), copy4.getName());

        // copy inplace
        DocumentModel copy5 = remote.copy(file.getRef(), folder1.getRef(), null);

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertTrue(remote.exists(new PathRef("folder2/file")));
        assertTrue(remote.getChildren(folder1.getRef()).contains(copy5));
        assertNotSame(copy1.getName(), copy5.getName());

        remote.cancel();
    }

    public void testCopyProxyAsDocument() throws Exception {
        // create a folder tree
        DocumentModel root = getRootDocument();
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
        remote.save();

        // create a file in folder 1
        file.setProperty("dublincore", "title", "the title");
        file = remote.saveDocument(file);

        VersionModel version = new VersionModelImpl();
        version.setCreated(Calendar.getInstance());
        version.setLabel("v1");
        remote.checkIn(file.getRef(), version);
        remote.save();

        // create a proxy in folder2
        DocumentModel proxy = remote.createProxy(folder2.getRef(),
                file.getRef(), version, true);
        assertTrue(proxy.isProxy());

        // copy proxy into folder3
        DocumentModel copy1 = remote.copyProxyAsDocument(proxy.getRef(),
                folder3.getRef(), null);
        assertFalse(copy1.isProxy());
        assertEquals(proxy.getName(), copy1.getName());
        assertEquals(proxy.getProperty("dublincore", "title"),
                copy1.getProperty("dublincore", "title"));

        // copy proxy using another name
        DocumentModel copy2 = remote.copyProxyAsDocument(proxy.getRef(),
                folder3.getRef(), "foo");
        assertFalse(copy2.isProxy());
        assertEquals("foo", copy2.getName());
        assertEquals(file.getProperty("dublincore", "title"),
                copy2.getProperty("dublincore", "title"));

        remote.cancel();
    }

    public void testCopyVersionable() throws Exception {
        DocumentModel note = new DocumentModelImpl("/", "note", "Note");
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        note = remote.createDocument(note);
        folder = remote.createDocument(folder);
        remote.save();

        assertTrue(remote.exists(new PathRef("note")));
        assertTrue(remote.exists(new PathRef("folder")));

        // no versions at first
        List<DocumentRef> versions = remote.getVersionsRefs(note.getRef());
        assertEquals(0, versions.size());

        // version the note
        note.setProperty("dublincore", "title", "blah");
        ScopedMap context = note.getContextData();
        context.putScopedValue(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        remote.saveDocument(note);
        remote.save();

        // check versions
        versions = remote.getVersionsRefs(note.getRef());
        assertEquals(1, versions.size());

        // copy
        DocumentModel copy = remote.copy(note.getRef(), folder.getRef(), null);

        // check no versions on copy
        versions = remote.getVersionsRefs(copy.getRef());
        assertEquals(0, versions.size());

        remote.cancel();
    }

    public void testCopyFolderOfVersionable() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        DocumentModel note = new DocumentModelImpl("/folder", "note", "Note");
        folder = remote.createDocument(folder);
        note = remote.createDocument(note);
        remote.save();

        assertTrue(remote.exists(new PathRef("/folder")));
        assertTrue(remote.exists(new PathRef("/folder/note")));

        // no versions at first
        List<DocumentRef> versions = remote.getVersionsRefs(note.getRef());
        assertEquals(0, versions.size());

        // version the note
        note.setProperty("dublincore", "title", "blah");
        ScopedMap context = note.getContextData();
        context.putScopedValue(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        remote.saveDocument(note);
        remote.save();

        // check versions
        versions = remote.getVersionsRefs(note.getRef());
        assertEquals(1, versions.size());

        // copy folder, use an all-digit name to test for xpath escaping
        DocumentModel copy = remote.copy(folder.getRef(), root.getRef(), "123");

        // check no versions on copied note
        DocumentModel note2 = remote.getChild(copy.getRef(), "note");
        versions = remote.getVersionsRefs(note2.getRef());
        assertEquals(0, versions.size());

        remote.cancel();
    }

    public void testMove() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");

        DocumentModel folder2 = new DocumentModelImpl(root.getPathAsString(),
                "folder2", "Folder");

        DocumentModel file = new DocumentModelImpl(folder1.getPathAsString(),
                "file", "File");

        folder1 = createChildDocument(folder1);
        folder2 = createChildDocument(folder2);
        file = createChildDocument(file);

        assertTrue(remote.exists(new PathRef("folder1/file")));
        assertFalse(remote.exists(new PathRef("folder2/file")));
        assertFalse(remote.exists(new PathRef("folder1/fileMove")));

        remote.move(file.getRef(), folder2.getRef(), null); // move using orig
        // name

        assertFalse(remote.exists(new PathRef("folder1/file")));
        assertTrue(remote.exists(new PathRef("folder2/file")));

        file = remote.getChild(folder2.getRef(), "file");
        remote.move(file.getRef(), folder1.getRef(), "fileMove");

        assertTrue(remote.exists(new PathRef("folder1/fileMove")));

        DocumentModel file2 = new DocumentModelImpl(folder2.getPathAsString(),
                "file2", "File");
        file2 = createChildDocument(file2);
        assertTrue(remote.exists(new PathRef("folder2/file2")));
        DocumentModel newFile2 = remote.move(file.getRef(), folder2.getRef(),
                "file2"); // collision
        String newName = newFile2.getName();
        assertFalse("file2".equals(newName));
        assertTrue(remote.exists(new PathRef("folder2/file2")));
        assertTrue(remote.exists(new PathRef("folder2/" + newName)));

        remote.cancel();
    }

    // TODO: fix this test
    public void XXXtestScalarList() throws Exception {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        String[] str = new String[] { "a", "b", "c" };
        childFile.setProperty("dublincore", "participants", str);
        remote.saveDocument(childFile);

        childFile = remote.getChild(root.getRef(), childFile.getName());

        str = (String[]) childFile.getProperty("dublincore", "participants");

        assertNotNull(str);
        List<String> list = Arrays.asList(str);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains("c"));

        // modify the array

        str = new String[] { "a", "b" };
        childFile.setProperty("dublincore", "participants", str);
        remote.saveDocument(childFile);

        str = (String[]) childFile.getProperty("dublincore", "participants");

        childFile = remote.getChild(root.getRef(), childFile.getName());
        str = (String[]) childFile.getProperty("dublincore", "participants");

        assertNotNull(str);
        list = Arrays.asList(str);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
    }

    public void testBlob() throws Exception {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

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

        remote.saveDocument(childFile);

        childFile = remote.getDocument(childFile.getRef());
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
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        DocumentModel doc = remote.getDocument(new IdRef(UID));
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
        remote.saveDocument(doc);

        remote.getDocument(doc.getRef());
        b2 = (Blob) doc.getProperty("file", "content");
        assertEquals("XXX", b2.getDigest());
        assertEquals("blob.txt", b2.getFilename());
        assertEquals(length, b2.getLength());
        assertEquals(encoding, b2.getEncoding());
        assertTrue(Arrays.equals(content, b2.getByteArray()));
        assertEquals(mimetype, b2.getMimeType());

    }

    public void xxx__testUploadBigBlob() throws Exception {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);

        remote.save();

        byte[] bytes = FileUtils.readBytes(Blob.class.getResourceAsStream("/blob.mp3"));
        Blob blob = new ByteArrayBlob(bytes, "audio/mpeg");

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        childFile.setProperty("file", "content", blob);

        remote.saveDocument(childFile);
        remote.save();
    }

    public void testRetrieveSamePropertyInAncestors() throws ClientException {
        DocumentModel root = getRootDocument();
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

        assertTrue(remote.exists(new PathRef("/folder1")));
        assertTrue(remote.exists(new PathRef("folder1/folder2")));
        assertTrue(remote.exists(new PathRef("folder1/folder2/file")));

        // need to save them before getting properties from schemas...
        remote.saveDocument(folder1);
        remote.saveDocument(folder2);
        remote.saveDocument(file);
        remote.save();

        final DocumentRef[] ancestorRefs = remote.getParentDocumentRefs(file.getRef());
        assertNotNull(ancestorRefs);
        assertEquals(3, ancestorRefs.length);
        assertEquals(folder2.getRef(), ancestorRefs[0]);
        assertEquals(folder1.getRef(), ancestorRefs[1]);
        assertEquals(root.getRef(), ancestorRefs[2]);

        final Object[] fieldValues = remote.getDataModelsField(ancestorRefs,
                "dublincore", "title");
        assertNotNull(fieldValues);
        assertEquals(3, fieldValues.length);
        assertEquals("folder #2", fieldValues[0]);
        assertEquals("folder #1", fieldValues[1]);

        final Object[] fieldValuesBis = remote.getDataModelsFieldUp(
                file.getRef(), "dublincore", "title");
        assertNotNull(fieldValuesBis);
        assertEquals(4, fieldValuesBis.length);
        assertEquals("file ##", fieldValuesBis[0]);
        assertEquals("folder #2", fieldValuesBis[1]);
        assertEquals("folder #1", fieldValuesBis[2]);
    }

    public void testLock() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel folder1 = new DocumentModelImpl(root.getPathAsString(),
                "folder1", "Folder");

        folder1 = createChildDocument(folder1);
        assertNull(folder1.getLock());
        assertFalse(folder1.isLocked());
        folder1.setLock("bstefanescu");
        assertEquals("bstefanescu", folder1.getLock());
        assertTrue(folder1.isLocked());

        folder1 = remote.getChild(root.getRef(), "folder1");
        assertEquals("bstefanescu", folder1.getLock());
        assertTrue(folder1.isLocked());

        folder1.unlock();
        assertNull(folder1.getLock());
        assertFalse(folder1.isLocked());
    }

    // TODO: fix and reenable.
    public void XXXtestDocumentAdapter() throws Exception {
        DocumentModel root = getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "file", "File");

        file = createChildDocument(file);

        AnnotatedDocument adoc = file.getAdapter(AnnotatedDocument.class);
        assertNotNull(adoc);
        adoc.putAnnotation("key1", "val1");
        adoc.putAnnotation("key2", "val2");
        assertEquals("val1", adoc.getAnnotation("key1"));
        assertEquals("val2", adoc.getAnnotation("key2"));

        adoc = file.getAdapter(AnnotatedDocument.class);
        assertEquals("val1", adoc.getAnnotation("key1"));
        assertEquals("val2", adoc.getAnnotation("key2"));
    }

    public void testGetSourceId() throws ClientException {
        DocumentModel root = getRootDocument();

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

        remote.save();
        remote.checkIn(childFile.getRef(), version);

        // Different source ids now.
        assertNotNull(childFile.getSourceId());
        assertEquals(sourceId, childFile.getSourceId());
        // TODO: look at this test.
        // assertFalse(childFile.getId().equals(childFile.getSourceId()));
    }

    public void testGetRepositoryName() throws ClientException {
        DocumentModel root = getRootDocument();

        String name2 = "file#" + generateUnique();
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = createChildDocument(childFile);
        assertNotNull(childFile.getRepositoryName());
        assertEquals("default", childFile.getRepositoryName());
    }

    // TODO: fix and reenable, is this a bug?
    public void XXXtestRetrieveProxies() throws ClientException {
        DocumentModel root = getRootDocument();

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
        remote.saveDocumentAsNewVersion(file);

        // Publishing
        remote.publishDocument(file, sectionA);
        // remote.publishDocument(file, sectionB);

        // Retrieving proxies
        DocumentModelList proxies = remote.getProxies(file.getRef(),
                sectionA.getRef());

        assertFalse(proxies.isEmpty());
        assertEquals(1, proxies.size());
        // assertEquals(2, proxies.size());
    }

    public void testCreateDocumentModel() throws ClientException {
        // first method: only the typename
        DocumentModel docModel = remote.createDocumentModel("File");
        assertEquals("File", docModel.getType());

        // bad type should fail with ClientException
        try {
            remote.createDocumentModel("NotAValidTypeName");
            fail();
        } catch (ClientException e) {
        }

        // same as previously with path info
        docModel = remote.createDocumentModel("/path/to/parent", "some-id",
                "File");
        assertEquals("File", docModel.getType());
        assertEquals("/path/to/parent/some-id", docModel.getPathAsString());

        // providing additional contextual data to feed a core event listener
        // with
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("Meteo", "Today is a beautiful day");
        docModel = remote.createDocumentModel("File", context);
        assertEquals("File", docModel.getType());
    }

    @SuppressWarnings("unchecked")
    public void testCopyContent() throws ClientException {
        DocumentModel root = remote.getRootDocument();
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
        doc = remote.createDocument(doc);
        remote.save();

        DocumentModel copy = new DocumentModelImpl(root.getPathAsString(),
                "copy", "File");
        copy.copyContent(doc);
        copy = remote.createDocument(copy);
        remote.save();

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
        DocumentModel root = getRootDocument();
        DocumentModel a_folder = new DocumentModelImpl(root.getPathAsString(),
                "a_folder", "Folder");
        a_folder.setProperty("dublincore", "title", "Z title for a_folder");
        DocumentModel b_folder = new DocumentModelImpl(root.getPathAsString(),
                "b_folder", "Folder");
        b_folder.setProperty("dublincore", "title", "B title for b_folder");
        DocumentModel c_folder = new DocumentModelImpl(root.getPathAsString(),
                "c_folder", "Folder");
        c_folder.setProperty("dublincore", "title", "C title for c_folder");

        DocumentModel a1_folder = new DocumentModelImpl(a_folder.getPathAsString(),
                "a1_folder", "Folder");
        a1_folder.setProperty("dublincore", "title", "ZZ title for a1_folder");
        DocumentModel a2_folder = new DocumentModelImpl(a_folder.getPathAsString(),
                "a2_folder", "Folder");
        a2_folder.setProperty("dublincore", "title", "AA title for a2_folder");

        DocumentModel b1_folder = new DocumentModelImpl(b_folder.getPathAsString(),
                "b1_folder", "Folder");
        b1_folder.setProperty("dublincore", "title", "A title for b1_folder");
        DocumentModel b2_folder = new DocumentModelImpl(b_folder.getPathAsString(),
                "b2_folder", "Folder");
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
        Collections.sort((ArrayList)tree, comp);

        assertEquals(b_folder, tree.get(0).getDocument());
        assertEquals(b1_folder, tree.get(1).getDocument());
        assertEquals(b2_folder, tree.get(2).getDocument());

        assertEquals(c_folder, tree.get(3).getDocument());

        assertEquals(a_folder, tree.get(4).getDocument());
        assertEquals(a2_folder, tree.get(5).getDocument());
        assertEquals(a1_folder, tree.get(6).getDocument());

        remote.cancel();
    }

}
