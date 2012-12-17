/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Tests the {@link DefaultFileSystemItemFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml" })
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
public class TestDefaultFileSystemItemFactory {

    private static final String DEFAULT_FILE_SYSTEM_ID_PREFIX = "defaultFileSystemItemFactory/test/";

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    protected Principal principal;

    protected String rootDocFileSystemItemId;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folder;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

    protected FileSystemItemFactory defaultFileSystemItemFactory;

    @Before
    public void createTestDocs() throws Exception {

        principal = session.getPrincipal();

        // TODO
        DocumentModel rootDoc = session.getRootDocument();
        rootDocFileSystemItemId = DEFAULT_FILE_SYSTEM_ID_PREFIX
                + rootDoc.getId();

        // File
        file = session.createDocumentModel("/", "aFile", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        // Note
        note = session.createDocumentModel("/", "aNote", "Note");
        note.setPropertyValue("note:note", "Content of Bob's note.");
        note = session.createDocument(note);

        // Custom doc type with the "file" schema
        custom = session.createDocumentModel("/", "aCustomDoc", "Custom");
        blob = new StringBlob("Content of Bonnie's file.");
        blob.setFilename("Bonnie's file.odt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom = session.createDocument(custom);

        // Folder
        folder = session.createDocumentModel("/", "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder = session.createDocument(folder);

        // FolderishFile: doc type with the "file" schema and the "Folderish"
        // facet
        folderishFile = session.createDocumentModel("/", "aFolderishFile",
                "FolderishFile");
        folderishFile.setPropertyValue("dc:title", "Sarah's folderish file");
        folderishFile = session.createDocument(folderishFile);

        // Doc not adaptable as a FileSystemItem (not Folderish nor a
        // BlobHolder)
        notAFileSystemItem = session.createDocumentModel("/",
                "notAFileSystemItem", "NotSynchronizable");
        notAFileSystemItem = session.createDocument(notAFileSystemItem);

        session.save();

        // TODO
        defaultFileSystemItemFactory = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactories().get(
                1).getFactory();
        assertTrue(defaultFileSystemItemFactory instanceof DefaultFileSystemItemFactory);
    }

    @Test
    public void testGetFileSystemItem() throws Exception {

        // ------------------------------------------------------
        // Check downloadable FileSystemItems
        // ------------------------------------------------------
        // File
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItem(file);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Joe.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // Note
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + note.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());

        // Custom doc type with the "file" schema
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(custom);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + custom.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Bonnie's file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.odt", fileItemBlob.getFilename());
        assertEquals("Content of Bonnie's file.", fileItemBlob.getString());

        // File without a blob => not adaptable as a FileSystemItem
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(file);
        assertNull(fsItem);

        // ------------------------------------------------------
        // Check folderish FileSystemItems
        // ------------------------------------------------------
        // Folder
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        List<FileSystemItem> children = ((FolderItem) fsItem).getChildren();
        assertNotNull(children);
        assertEquals(0, children.size());

        // FolderishFile => adaptable as a FolderItem since the default
        // FileSystemItem factory gives precedence to the Folderish facet
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(folderishFile);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folderishFile.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Sarah's folderish file", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());

        // ------------------------------------------------------
        // Check not adaptable as a FileSystemItem
        // ------------------------------------------------------
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(notAFileSystemItem);
        assertNull(fsItem);

        // -------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, String parentId)
        // -------------------------------------------------------------
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note,
                "noteParentId");
        assertEquals("noteParentId", fsItem.getParentId());
        fsItem = defaultFileSystemItemFactory.getFileSystemItem(note, null);
        assertNull(fsItem.getParentId());
    }

    @Test
    public void testExists() throws Exception {

        // Bad id
        try {
            defaultFileSystemItemFactory.exists("badId", principal);
            fail("Should not be able to check existence for bad id.");
        } catch (ClientException e) {
            assertEquals(
                    "FileSystemItem id badId cannot be handled by factory named defaultFileSystemItemFactory. Should match the 'fileSystemItemFactoryName/repositoryName/docId' pattern.",
                    e.getMessage());
        }
        // Non existent doc id
        assertFalse(defaultFileSystemItemFactory.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + "nonExistentDocId", principal));
        // File
        assertTrue(defaultFileSystemItemFactory.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(), principal));
        // Note
        assertTrue(defaultFileSystemItemFactory.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + note.getId(), principal));
        // Not adaptable as a FileSystemItem
        assertFalse(defaultFileSystemItemFactory.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + notAFileSystemItem.getId(),
                principal));
    }

    @Test
    public void testGetFileSystemItemById() throws Exception {

        // Non existent doc id
        try {
            defaultFileSystemItemFactory.getFileSystemItemById(
                    DEFAULT_FILE_SYSTEM_ID_PREFIX + "nonExistentDocId",
                    principal);
            fail("No FileSystemItem should be found for non existant id.");
        } catch (ClientException e) {
            assertEquals("Failed to get document nonExistentDocId",
                    e.getMessage());
        }
        // File without a blob
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        session.save();
        FileSystemItem fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(), principal);
        assertNull(fsItem);
        // Note
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + note.getId(), principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + note.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());
        // Folder
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(), principal);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());
        // Not adaptable as a FileSystemItem
        fsItem = defaultFileSystemItemFactory.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + notAFileSystemItem.getId(),
                principal);
        assertNull(fsItem);
    }

    @Test
    public void testFileItem() throws Exception {

        // ------------------------------------------------------------
        // FileSystemItem#getCanRename and FileSystemItem#getCanDelete
        // ------------------------------------------------------------
        FileItem fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
        assertTrue(fileItem.getCanRename());
        assertTrue(fileItem.getCanDelete());

        // ------------------------------------------------------
        // FileItem#getDownloadURL
        // ------------------------------------------------------
        String baseURL = "http://myServer/nuxeo/";
        String downloadURL = fileItem.getDownloadURL(baseURL);
        assertEquals("http://myServer/nuxeo/nxbigfile/test/" + file.getId()
                + "/blobholder:0/Joe.odt", downloadURL);

        // ------------------------------------------------------
        // FileItem#setBlob
        // ------------------------------------------------------
        Blob fileItemBlob = fileItem.getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        Blob newBlob = new StringBlob("This is a new file.");
        newBlob.setFilename("New blob.txt");
        fileItem.setBlob(newBlob);
        ((DocumentBackedFileItem) fileItem).getSession().save();
        // Need to flush VCS cache to be aware of changes in the session used by
        // the file system item
        session.save();

        file = session.getDocument(file.getRef());
        Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
        assertEquals("New blob.txt", updatedBlob.getFilename());
        assertEquals("This is a new file.", updatedBlob.getString());
    }

    @Test
    public void testFolderItem() throws Exception {

        // ------------------------------------------------------------
        // FileSystemItem#getCanRename and FileSystemItem#getCanDelete
        // ------------------------------------------------------------
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        assertTrue(folderItem.getCanRename());
        assertTrue(folderItem.getCanDelete());

        // ------------------------------------------------------
        // FolderItem#canCreateChild
        // ------------------------------------------------------
        assertTrue(folderItem.getCanCreateChild());

        // ------------------------------------------------------
        // FolderItem#createFile and FolderItem#createFolder
        // ------------------------------------------------------
        folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        // Note
        Blob childBlob = new StringBlob("This is the first child file.");
        childBlob.setFilename("First child file.txt");
        folderItem.createFile(childBlob);
        // File
        childBlob = new StringBlob("This is the second child file.");
        childBlob.setFilename("Second child file.odt");
        childBlob.setMimeType("application/vnd.oasis.opendocument.text");
        folderItem.createFile(childBlob);
        // Folder
        folderItem.createFolder("Sub-folder");

        DocumentModelList children = session.query(String.format(
                "select * from Document where ecm:parentId = '%s' order by dc:created asc",
                folder.getId()));
        assertEquals(3, children.size());
        // Check Note
        DocumentModel note = children.get(0);
        assertEquals("Note", note.getType());
        assertEquals("First child file.txt", note.getTitle());
        childBlob = note.getAdapter(BlobHolder.class).getBlob();
        assertEquals("First child file.txt", childBlob.getFilename());
        assertEquals("This is the first child file.", childBlob.getString());
        // Check File
        DocumentModel file = children.get(1);
        assertEquals("File", file.getType());
        assertEquals("Second child file.odt", file.getTitle());
        childBlob = (Blob) file.getPropertyValue("file:content");
        assertEquals("Second child file.odt", childBlob.getFilename());
        assertEquals("This is the second child file.", childBlob.getString());
        // Check Folder
        DocumentModel subFolder = children.get(2);
        assertEquals("Folder", subFolder.getType());
        assertEquals("Sub-folder", subFolder.getTitle());

        // ------------------------------------------------------
        // FolderItem#getChildren
        // ------------------------------------------------------
        // Create another child adaptable as a FileSystemItem => should be
        // retrieved
        DocumentModel adaptableChild = session.createDocumentModel("/aFolder",
                "adaptableChild", "File");
        Blob adaptableChildBlob = new StringBlob("Content of another file.");
        adaptableChildBlob.setFilename("Another file.odt");
        adaptableChild.setPropertyValue("file:content",
                (Serializable) adaptableChildBlob);
        adaptableChild = session.createDocument(adaptableChild);
        // Create another child not adaptable as a FileSystemItem => should not
        // be retrieved
        session.createDocument(session.createDocumentModel("/aFolder",
                "notAdaptableChild", "NotSynchronizable"));
        session.save();

        List<FileSystemItem> folderChildren = folderItem.getChildren();
        assertEquals(4, folderChildren.size());
        // Check Note
        FileSystemItem fsItem = folderChildren.get(0);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + note.getId(),
                fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                fsItem.getParentId());
        assertEquals("First child file.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("First child file.txt", fileItemBlob.getFilename());
        assertEquals("This is the first child file.", fileItemBlob.getString());
        // Check File
        fsItem = folderChildren.get(1);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(),
                fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                fsItem.getParentId());
        assertEquals("Second child file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Second child file.odt", fileItemBlob.getFilename());
        assertEquals("This is the second child file.", fileItemBlob.getString());
        // Check sub-Folder
        fsItem = folderChildren.get(2);
        assertTrue(fsItem instanceof FolderItem);
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + subFolder.getId(),
                fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                fsItem.getParentId());
        assertEquals("Sub-folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        List<FileSystemItem> childFolderChildren = ((FolderItem) fsItem).getChildren();
        assertNotNull(childFolderChildren);
        assertEquals(0, childFolderChildren.size());
        // Check other File
        fsItem = folderChildren.get(3);
        assertTrue(fsItem instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + adaptableChild.getId(),
                fsItem.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                fsItem.getParentId());
        assertEquals("Another file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Another file.odt", fileItemBlob.getFilename());
        assertEquals("Content of another file.", fileItemBlob.getString());
    }

}
