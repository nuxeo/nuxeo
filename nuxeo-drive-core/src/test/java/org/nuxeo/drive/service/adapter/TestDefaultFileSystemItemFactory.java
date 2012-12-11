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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
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
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml" })
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
public class TestDefaultFileSystemItemFactory {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folder;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

    @Before
    public void createTestDocs() throws ClientException {

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
    }

    @Test
    public void testGetFileSystemItem() throws Exception {

        // ------------------------------------------------------
        // Check downloadable FileSystemItems
        // ------------------------------------------------------
        // File
        FileSystemItem fsItem = file.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("test/" + file.getId(), fsItem.getId());
        assertEquals("Joe.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // Note
        fsItem = note.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("test/" + note.getId(), fsItem.getId());
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());

        // Custom doc type with the "file" schema
        fsItem = custom.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("test/" + custom.getId(), fsItem.getId());
        assertEquals("Bonnie's file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.odt", fileItemBlob.getFilename());
        assertEquals("Content of Bonnie's file.", fileItemBlob.getString());

        // File without a blob => not adaptable as a FileSystemItem
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        fsItem = file.getAdapter(FileSystemItem.class);
        assertNull(fsItem);

        // ------------------------------------------------------
        // Check folderish FileSystemItems
        // ------------------------------------------------------
        // Folder
        fsItem = folder.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("test/" + folder.getId(), fsItem.getId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());
        List<FileSystemItem> children = ((FolderItem) fsItem).getChildren();
        assertNotNull(children);
        assertEquals(0, children.size());

        // FolderishFile => adaptable as a FolderItem since the default
        // FileSystemItem factory gives precedence to the Folderish facet
        fsItem = folderishFile.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("test/" + folderishFile.getId(), fsItem.getId());
        assertEquals("Sarah's folderish file", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Administrator", fsItem.getCreator());

        // ------------------------------------------------------
        // Check not adaptable as a FileSystemItem
        // ------------------------------------------------------
        fsItem = notAFileSystemItem.getAdapter(FileSystemItem.class);
        assertNull(fsItem);
    }

    @Test
    public void testExists() throws Exception {

        // Get default factory
        FileSystemItemFactory defaultFactory = getDefaultFileSystemItemFactory();
        assertEquals("defaultFileSystemItemFactory", defaultFactory.getName());
        assertTrue(defaultFactory.getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));

        // Bad id
        try {
            defaultFactory.exists("badId", session);
            fail("Should not be able to check existence for bad id.");
        } catch (ClientException e) {
            assertEquals(
                    "FileSystemItem id badId cannot be handled by factory named defaultFileSystemItemFactory. Should match the 'fileSystemItemFactoryName/repositoryName/docId' pattern.",
                    e.getMessage());
        }
        // Non existent doc id
        assertFalse(defaultFactory.exists(
                "defaultFileSystemItemFactory/test/nonExistentDocId", session));
        // File
        assertTrue(defaultFactory.exists("defaultFileSystemItemFactory/test/"
                + file.getId(), session));
        // Note
        assertTrue(defaultFactory.exists("defaultFileSystemItemFactory/test/"
                + note.getId(), session));
        // Not adaptable as a FileSystemItem
        assertFalse(defaultFactory.exists("defaultFileSystemItemFactory/test/"
                + notAFileSystemItem.getId(), session));
    }

    @Test
    public void testGetFileSystemItemById() throws Exception {

        // Get default factory
        FileSystemItemFactory defaultFactory = getDefaultFileSystemItemFactory();

        // Non existent doc id
        try {
            defaultFactory.getFileSystemItemById(
                    "defaultFileSystemItemFactory/test/nonExistentDocId",
                    session);
            fail("No FileSystemItem should be found for non existant id.");
        } catch (ClientException e) {
            assertEquals("Failed to get document nonExistentDocId",
                    e.getMessage());
        }
        // File without a blob
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        FileSystemItem fsItem = defaultFactory.getFileSystemItemById(
                "defaultFileSystemItemFactory/test/" + file.getId(), session);
        assertNull(fsItem);
        // Note
        fsItem = defaultFactory.getFileSystemItemById(
                "defaultFileSystemItemFactory/test/" + note.getId(), session);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());
        // Folder
        fsItem = defaultFactory.getFileSystemItemById(
                "defaultFileSystemItemFactory/test/" + folder.getId(), session);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());
        // Not adaptable as a FileSystemItem
        fsItem = defaultFactory.getFileSystemItemById(
                "defaultFileSystemItemFactory/test/"
                        + notAFileSystemItem.getId(), session);
        assertNull(fsItem);
    }

    @Test
    public void testFileItem() throws Exception {

        // ------------------------------------------------------
        // FileItem#getDownloadURL
        // ------------------------------------------------------
        String baseURL = "http://myServer/nuxeo/";
        FileItem fileItem = (FileItem) file.getAdapter(FileSystemItem.class);
        String downloadURL = fileItem.getDownloadURL(baseURL);
        assertEquals("http://myServer/nuxeo/nxbigfile/test/" + file.getId()
                + "/blobholder:0/Joe.odt", downloadURL);

        // ------------------------------------------------------
        // FileItem#setBlob
        // ------------------------------------------------------
        fileItem = (FileItem) file.getAdapter(FileSystemItem.class);
        Blob fileItemBlob = fileItem.getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        Blob newBlob = new StringBlob("This is a new file.");
        newBlob.setFilename("New blob.txt");
        fileItem.setBlob(newBlob);

        file = session.getDocument(file.getRef());
        Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
        assertEquals("New blob.txt", updatedBlob.getFilename());
        assertEquals("This is a new file.", updatedBlob.getString());
    }

    @Test
    public void testFolderItem() throws Exception {

        // ------------------------------------------------------
        // FolderItem#createFile and FolderItem#createFolder
        // ------------------------------------------------------
        FolderItem folderItem = (FolderItem) folder.getAdapter(FileSystemItem.class);
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
        folderItem.createFolder("Child folder");

        DocumentModelList children = session.query(String.format(
                "select * from Document where ecm:parentId = '%s' order by dc:created asc",
                folder.getId()));
        assertEquals(3, children.size());
        // Check Note
        DocumentModel child = children.get(0);
        assertEquals("Note", child.getType());
        assertEquals("First child file.txt", child.getTitle());
        childBlob = child.getAdapter(BlobHolder.class).getBlob();
        assertEquals("First child file.txt", childBlob.getFilename());
        assertEquals("This is the first child file.", childBlob.getString());
        // Check File
        child = children.get(1);
        assertEquals("File", child.getType());
        assertEquals("Second child file.odt", child.getTitle());
        childBlob = (Blob) child.getPropertyValue("file:content");
        assertEquals("Second child file.odt", childBlob.getFilename());
        assertEquals("This is the second child file.", childBlob.getString());
        // Check Folder
        child = children.get(2);
        assertEquals("Folder", child.getType());
        assertEquals("Child folder", child.getTitle());

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
        session.createDocument(adaptableChild);
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
        assertEquals("First child file.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("First child file.txt", fileItemBlob.getFilename());
        assertEquals("This is the first child file.", fileItemBlob.getString());
        // Check File
        fsItem = folderChildren.get(1);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("Second child file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Second child file.odt", fileItemBlob.getFilename());
        assertEquals("This is the second child file.", fileItemBlob.getString());
        // Check Folder
        fsItem = folderChildren.get(2);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("Child folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        List<FileSystemItem> childFolderChildren = ((FolderItem) fsItem).getChildren();
        assertNotNull(childFolderChildren);
        assertEquals(0, childFolderChildren.size());
        // Check other File
        fsItem = folderChildren.get(3);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("Another file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Another file.odt", fileItemBlob.getFilename());
        assertEquals("Content of another file.", fileItemBlob.getString());
    }

    protected FileSystemItemFactory getDefaultFileSystemItemFactory() {
        return ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFactories().get(
                0).getFactory();
    }
}
