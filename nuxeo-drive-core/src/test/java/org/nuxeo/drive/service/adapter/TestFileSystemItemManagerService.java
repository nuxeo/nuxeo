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
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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
 * Tests the {@link FileSystemItemManager}.
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
public class TestFileSystemItemManagerService {

    private static final String DEFAULT_FILE_SYSTEM_ID_PREFIX = "defaultFileSystemItemFactory/test/";

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemManager fileSystemItemManagerService;

    protected DocumentModel folder;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

    protected DocumentModel subFolder;

    @Before
    public void createTestDocs() throws ClientException {

        // Folder
        folder = session.createDocumentModel("/", "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder = session.createDocument(folder);

        // File
        file = session.createDocumentModel("/aFolder", "aFile", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        // Note
        note = session.createDocumentModel("/aFolder", "aNote", "Note");
        note.setPropertyValue("note:note", "Content of Bob's note.");
        note = session.createDocument(note);

        // Custom doc type with the "file" schema
        custom = session.createDocumentModel("/aFolder", "aCustomDoc", "Custom");
        blob = new StringBlob("Content of Bonnie's file.");
        blob.setFilename("Bonnie's file.odt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom = session.createDocument(custom);

        // FolderishFile: doc type with the "file" schema and the "Folderish"
        // facet
        folderishFile = session.createDocumentModel("/aFolder",
                "aFolderishFile", "FolderishFile");
        folderishFile.setPropertyValue("dc:title", "Sarah's folderish file");
        folderishFile = session.createDocument(folderishFile);

        // Doc not adaptable as a FileSystemItem (not Folderish nor a
        // BlobHolder)
        notAFileSystemItem = session.createDocumentModel("/aFolder",
                "notAFileSystemItem", "NotSynchronizable");
        notAFileSystemItem = session.createDocument(notAFileSystemItem);

        // Sub folder
        subFolder = session.createDocumentModel("/aFolder", "aSubFolder",
                "Folder");
        subFolder.setPropertyValue("dc:title", "Tony's sub folder");
        subFolder = session.createDocument(subFolder);

        session.save();
    }

    @Test
    public void testReadOperations() throws Exception {

        // ------------------------------------------------------
        // Check #exists
        // ------------------------------------------------------
        // Non existent doc id
        assertFalse(fileSystemItemManagerService.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + "nonExistentId", session));
        // File
        assertTrue(fileSystemItemManagerService.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(), session));
        // Not adaptable as a FileSystemItem
        assertFalse(fileSystemItemManagerService.exists(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + notAFileSystemItem.getId(),
                session));

        // ------------------------------------------------------
        // Check #getFileSystemItemById
        // ------------------------------------------------------
        // Folder
        FileSystemItem fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(), session);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        List<FileSystemItem> children = ((FolderItem) fsItem).getChildren();
        assertNotNull(children);
        assertEquals(5, children.size());

        // File
        fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(), session);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("Joe.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // FolderishFile
        fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + folderishFile.getId(), session);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("Sarah's folderish file", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());

        // Not adaptable as a FileSystemItem
        fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + notAFileSystemItem.getId(),
                session);
        assertNull(fsItem);

        // Sub folder
        fsItem = fileSystemItemManagerService.getFileSystemItemById(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + subFolder.getId(), session);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("Tony's sub folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertTrue(((FolderItem) fsItem).getChildren().isEmpty());

        // ------------------------------------------------------
        // Check #getChildren
        // ------------------------------------------------------
        children = fileSystemItemManagerService.getChildren(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(), session);
        assertNotNull(children);
        assertEquals(5, children.size());
        FileSystemItem child = children.get(0);
        assertEquals("Joe.odt", child.getName());
        assertFalse(child.isFolder());
        child = children.get(1);
        assertEquals("aNote.txt", child.getName());
        assertFalse(child.isFolder());
        child = children.get(2);
        assertEquals("Bonnie's file.odt", child.getName());
        assertFalse(child.isFolder());
        child = children.get(3);
        assertEquals("Sarah's folderish file", child.getName());
        assertTrue(child.isFolder());
        child = children.get(4);
        assertEquals("Tony's sub folder", child.getName());
        assertTrue(child.isFolder());

        children = fileSystemItemManagerService.getChildren(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + subFolder.getId(), session);
        assertTrue(children.isEmpty());
    }

    @Test
    public void testWriteOperations() throws Exception {

        // ------------------------------------------------------
        // Check #createFolder
        // ------------------------------------------------------
        // Not allowed sub-type exception
        try {
            fileSystemItemManagerService.createFolder(
                    DEFAULT_FILE_SYSTEM_ID_PREFIX
                            + session.getRootDocument().getId(),
                    "A new folder", session);
            fail("Folder creation should fail because the Folder type is not allowed as a sub-type of Root.");
        } catch (ClientException e) {
            assertEquals(
                    "Cannot create folder named 'A new folder' as a child of doc /. Probably because of the allowed sub-types for this doc type, please check the allowed sub-types for the Root doc type.",
                    e.getMessage());
        }
        // Folder creation
        FolderItem newFolderItem = fileSystemItemManagerService.createFolder(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(), "A new folder",
                session);
        assertNotNull(newFolderItem);
        assertEquals("A new folder", newFolderItem.getName());
        DocumentModelList folderChildren = session.query(String.format(
                "select * from Document where ecm:parentId = '%s' order by dc:created desc",
                folder.getId()));
        DocumentModel newFolder = folderChildren.get(0);
        assertTrue(newFolder.isFolder());
        assertEquals("A new folder", newFolder.getTitle());

        // Parent folder children check
        assertEquals(
                6,
                fileSystemItemManagerService.getChildren(
                        DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(), session).size());

        // ------------------------------------------------------
        // Check #createFile
        // ------------------------------------------------------
        // File creation
        String newFolderItemId = newFolderItem.getDocument().getId();
        Blob blob = new StringBlob("Content of a new file.");
        blob.setFilename("New file.odt");
        blob.setMimeType("application/vnd.oasis.opendocument.text");
        FileItem fileItem = fileSystemItemManagerService.createFile(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + newFolderItemId, blob, session);
        assertNotNull(fileItem);
        assertEquals("New file.odt", fileItem.getName());
        folderChildren = session.query(String.format(
                "select * from Document where ecm:parentId = '%s'",
                newFolder.getId()));
        assertEquals(1, folderChildren.size());
        DocumentModel newFile = folderChildren.get(0);
        assertEquals("File", newFile.getType());
        assertEquals("New file.odt", newFile.getTitle());
        assertEquals("/aFolder/A new folder/New file.odt",
                newFile.getPathAsString());
        Blob newFileBlob = (Blob) newFile.getPropertyValue("file:content");
        assertEquals("New file.odt", newFileBlob.getFilename());
        assertEquals("Content of a new file.", newFileBlob.getString());

        // Parent folder children check
        assertEquals(
                1,
                fileSystemItemManagerService.getChildren(
                        DEFAULT_FILE_SYSTEM_ID_PREFIX + newFolderItemId,
                        session).size());

        // ------------------------------------------------------
        // Check #updateFile
        // ------------------------------------------------------
        String fileItemId = fileItem.getDocument().getId();
        blob = new StringBlob("Modified content of an existing file.");
        fileItem = fileSystemItemManagerService.updateFile(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + fileItemId, blob, session);
        assertNotNull(fileItem);
        assertEquals(fileItemId, fileItem.getDocument().getId());
        assertEquals("New file.odt", fileItem.getName());
        folderChildren = session.query(String.format(
                "select * from Document where ecm:parentId = '%s'",
                newFolder.getId()));
        assertEquals(1, folderChildren.size());
        DocumentModel updatedFile = folderChildren.get(0);
        assertEquals("File", updatedFile.getType());
        assertEquals("New file.odt", updatedFile.getTitle());
        assertEquals("/aFolder/A new folder/New file.odt",
                updatedFile.getPathAsString());
        Blob updatedFileBlob = (Blob) updatedFile.getPropertyValue("file:content");
        assertEquals("New file.odt", updatedFileBlob.getFilename());
        assertEquals("Modified content of an existing file.",
                updatedFileBlob.getString());

        // ------------------------------------------------------
        // Check #delete
        // ------------------------------------------------------
        // File deletion
        fileSystemItemManagerService.delete(DEFAULT_FILE_SYSTEM_ID_PREFIX
                + updatedFile.getId(), session);

        // Parent folder children check
        assertTrue(fileSystemItemManagerService.getChildren(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + newFolderItemId, session).isEmpty());

        // Parent folder trash check
        assertEquals(
                1,
                session.query(
                        String.format(
                                "select * from Document where ecm:parentId = '%s' and ecm:currentLifeCycleState = 'deleted'",
                                newFolder.getId())).size());

        // ------------------------------------------------------
        // Check #rename
        // ------------------------------------------------------
        // Folder rename
        FileSystemItem fsItem = fileSystemItemManagerService.rename(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + folder.getId(),
                "Jack's folder has a new name", session);
        assertEquals("Jack's folder has a new name", fsItem.getName());
        folder = session.getDocument(folder.getRef());
        assertEquals("Jack's folder has a new name", folder.getTitle());

        // File rename with title != filename
        // => should rename filename but not title
        assertEquals("aFile", file.getTitle());
        assertEquals("Joe.odt",
                ((Blob) file.getPropertyValue("file:content")).getFilename());
        fsItem = fileSystemItemManagerService.rename(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + file.getId(),
                "File new name.odt", session);
        assertEquals("File new name.odt", fsItem.getName());
        file = session.getDocument(file.getRef());
        assertEquals("aFile", file.getTitle());
        assertEquals("File new name.odt",
                ((Blob) file.getPropertyValue("file:content")).getFilename());

        // File rename with title == filename
        // => should rename filename and title
        blob = new StringBlob("File for a doc with title == filename.");
        blob.setFilename("Title-filename equality.odt");
        blob.setMimeType("application/vnd.oasis.opendocument.text");
        fileItem = fileSystemItemManagerService.createFile(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + newFolderItemId, blob, session);
        newFile = fileItem.getDocument();
        assertEquals("Title-filename equality.odt", newFile.getTitle());
        assertEquals("Title-filename equality.odt",
                ((Blob) newFile.getPropertyValue("file:content")).getFilename());
        fsItem = fileSystemItemManagerService.rename(
                DEFAULT_FILE_SYSTEM_ID_PREFIX + newFile.getId(),
                "Renamed title-filename equality.odt", session);
        assertEquals("Renamed title-filename equality.odt", fsItem.getName());
        newFile = session.getDocument(newFile.getRef());
        assertEquals("Renamed title-filename equality.odt", newFile.getTitle());
        assertEquals("Renamed title-filename equality.odt",
                ((Blob) newFile.getPropertyValue("file:content")).getFilename());
    }

}
