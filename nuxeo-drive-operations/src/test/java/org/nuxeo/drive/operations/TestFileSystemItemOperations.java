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
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.StringBlob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the {@link FileSystemItem} related operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.drive.core", "org.nuxeo.drive.operations" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestFileSystemItemOperations {

    private static final String SYNC_ROOT_FOLDER_ITEM_ID_PREFIX = "defaultSyncRootFolderItemFactory/test/";

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory/test/";

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel file1;

    protected DocumentModel file2;

    protected DocumentModel file3;

    protected DocumentModel file4;

    protected DocumentModel subFolder1;

    protected Session clientSession;

    protected ObjectMapper mapper;

    /**
     * Initializes the test hierarchy.
     *
     * <pre>
     * topLevel
     *   |-- folder1 (syncRoot1)
     *   |     |-- file1
     *   |     |-- subFolder1
     *   |           |-- file3
     *   |           |-- file4
     *   |-- folder2 (syncRoot2)
     *   |     |-- file2
     * </pre>
     */
    @Before
    public void init() throws Exception {

        // Create 2 sync roots
        syncRoot1 = session.createDocument(session.createDocumentModel("/",
                "folder1", "Folder"));
        syncRoot2 = session.createDocument(session.createDocumentModel("/",
                "folder2", "Folder"));

        // Register sync roots
        nuxeoDriveManager.registerSynchronizationRoot("Administrator",
                syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator",
                syncRoot2, session);

        // Create 1 file in each sync root
        file1 = session.createDocumentModel("/folder1", "file1", "File");
        org.nuxeo.ecm.core.api.Blob blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 1.");
        blob.setFilename("First file.odt");
        file1.setPropertyValue("file:content", (Serializable) blob);
        file1 = session.createDocument(file1);
        file2 = session.createDocumentModel("/folder2", "file2", "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 2.");
        blob.setFilename("Second file.odt");
        file2.setPropertyValue("file:content", (Serializable) blob);
        file2 = session.createDocument(file2);

        // Create a sub-folder in sync root 1
        subFolder1 = session.createDocument(session.createDocumentModel(
                "/folder1", "subFolder1", "Folder"));

        // Create 2 files in sub-folder
        file3 = session.createDocumentModel("/folder1/subFolder1", "file3",
                "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 3.");
        blob.setFilename("Third file.odt");
        file3.setPropertyValue("file:content", (Serializable) blob);
        file3 = session.createDocument(file3);
        file4 = session.createDocumentModel("/folder1/subFolder1", "file4",
                "File");
        blob = new org.nuxeo.ecm.core.api.impl.blob.StringBlob(
                "The content of file 4.");
        blob.setFilename("Fourth file.odt");
        file4.setPropertyValue("file:content", (Serializable) blob);
        file4 = session.createDocument(file4);

        session.save();

        // Get an Automation client session
        clientSession = automationClient.getSession("Administrator",
                "Administrator");
        mapper = new ObjectMapper();
    }

    @Test
    public void testGetTopLevelChildren() throws Exception {

        Blob topLevelChildrenJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetTopLevelChildren.ID).execute();
        assertNotNull(topLevelChildrenJSON);

        List<DefaultSyncRootFolderItem> topLevelChildren = mapper.readValue(
                topLevelChildrenJSON.getStream(),
                new TypeReference<List<DefaultSyncRootFolderItem>>() {
                });
        assertNotNull(topLevelChildren);
        assertEquals(2, topLevelChildren.size());

        DefaultSyncRootFolderItem child = topLevelChildren.get(0);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                child.getId());
        assertTrue(child.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertEquals("folder1", child.getName());
        assertTrue(child.isFolder());
        assertEquals("Administrator", child.getCreator());
        assertFalse(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertTrue(child.getCanCreateChild());

        child = topLevelChildren.get(1);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId(),
                child.getId());
        assertTrue(child.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertEquals("folder2", child.getName());
        assertTrue(child.isFolder());
        assertEquals("Administrator", child.getCreator());
        assertFalse(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertTrue(child.getCanCreateChild());
    }

    @Test
    public void testFileSystemItemExists() throws Exception {

        // Non existing file system item
        Blob fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + "badId").execute();
        assertNotNull(fileSystemItemExistsJSON);

        String fileSystemItemExists = mapper.readValue(
                fileSystemItemExistsJSON.getStream(), String.class);
        assertEquals("false", fileSystemItemExists);

        // Existing file system item
        fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).execute();
        assertNotNull(fileSystemItemExistsJSON);

        fileSystemItemExists = mapper.readValue(
                fileSystemItemExistsJSON.getStream(), String.class);
        assertEquals("true", fileSystemItemExists);

    }

    @Test
    public void testGetFileSystemItem() throws Exception {

        // Get sync root
        Blob fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        DefaultSyncRootFolderItem fileSystemItem = mapper.readValue(
                fileSystemItemJSON.getStream(), DefaultSyncRootFolderItem.class);
        assertNotNull(fileSystemItem);
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                fileSystemItem.getId());
        assertTrue(fileSystemItem.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertEquals("folder1", fileSystemItem.getName());
        assertTrue(fileSystemItem.isFolder());
        assertEquals("Administrator", fileSystemItem.getCreator());
        assertFalse(fileSystemItem.getCanRename());
        assertTrue(fileSystemItem.getCanDelete());
        assertTrue(fileSystemItem.getCanCreateChild());

        // Get file in sync root
        fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        DocumentBackedFileItem fileSystemItem1 = mapper.readValue(
                fileSystemItemJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(fileSystemItem1);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file1.getId(),
                fileSystemItem1.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot1.getId(),
                fileSystemItem1.getParentId());
        assertEquals("First file.odt", fileSystemItem1.getName());
        assertFalse(fileSystemItem1.isFolder());
        assertEquals("Administrator", fileSystemItem1.getCreator());
        assertTrue(fileSystemItem1.getCanRename());
        assertTrue(fileSystemItem1.getCanDelete());
        assertEquals("http://my-server/nuxeo/nxbigfile/test/" + file1.getId()
                + "/blobholder:0/First%20file.odt",
                fileSystemItem1.getDownloadURL("http://my-server/nuxeo/"));
    }

    @Test
    public void testGetChildren() throws Exception {

        // Get children of sub-folder of sync root 1
        Blob childrenJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetChildren.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId()).execute();
        assertNotNull(childrenJSON);

        List<DocumentBackedFileItem> children = mapper.readValue(
                childrenJSON.getStream(),
                new TypeReference<List<DocumentBackedFileItem>>() {
                });
        assertNotNull(children);
        assertEquals(2, children.size());

        DocumentBackedFileItem child = children.get(0);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file3.getId(),
                child.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId(),
                child.getParentId());
        assertEquals("Third file.odt", child.getName());
        assertFalse(child.isFolder());
        assertEquals("Administrator", child.getCreator());
        assertTrue(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertEquals("http://my-server/nuxeo/nxbigfile/test/" + file3.getId()
                + "/blobholder:0/Third%20file.odt",
                child.getDownloadURL("http://my-server/nuxeo/"));

        child = children.get(1);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + file4.getId(),
                child.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId(),
                child.getParentId());
        assertEquals("Fourth file.odt", child.getName());
        assertFalse(child.isFolder());
        assertEquals("Administrator", child.getCreator());
        assertTrue(child.getCanRename());
        assertTrue(child.getCanDelete());
        assertEquals("http://my-server/nuxeo/nxbigfile/test/" + file4.getId()
                + "/blobholder:0/Fourth%20file.odt",
                child.getDownloadURL("http://my-server/nuxeo/"));
    }

    @Test
    public void testCreateFolder() throws Exception {

        Blob newFolderJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCreateFolder.ID).set("id",
                SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId()).set(
                "name", "newFolder").execute();
        assertNotNull(newFolderJSON);

        DocumentBackedFolderItem newFolder = mapper.readValue(
                newFolderJSON.getStream(), DocumentBackedFolderItem.class);
        assertNotNull(newFolder);

        session.save();

        DocumentModel newFolderDoc = session.getDocument(new PathRef(
                "/folder2/newFolder"));
        assertEquals("Folder", newFolderDoc.getType());
        assertEquals("newFolder", newFolderDoc.getTitle());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFolderDoc.getId(),
                newFolder.getId());
        assertEquals(SYNC_ROOT_FOLDER_ITEM_ID_PREFIX + syncRoot2.getId(),
                newFolder.getParentId());
        assertEquals("newFolder", newFolder.getName());
        assertTrue(newFolder.isFolder());
        assertEquals("Administrator", newFolder.getCreator());
        assertTrue(newFolder.getCanRename());
        assertTrue(newFolder.getCanDelete());
        assertTrue(newFolder.getCanCreateChild());
    }

    @Test
    public void testCreateFile() throws Exception {

        StringBlob blob = new StringBlob("This is the content of a new file.");
        blob.setFileName("New file.odt");
        Blob newFileJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCreateFile.ID).set("id",
                DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId()).setInput(
                blob).execute();
        assertNotNull(newFileJSON);

        DocumentBackedFileItem newFile = mapper.readValue(
                newFileJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(newFile);

        session.save();

        DocumentModel newFileDoc = session.getDocument(new PathRef(
                "/folder1/subFolder1/New file.odt"));
        assertEquals("File", newFileDoc.getType());
        assertEquals("New file.odt", newFileDoc.getTitle());
        org.nuxeo.ecm.core.api.Blob newFileBlob = (org.nuxeo.ecm.core.api.Blob) newFileDoc.getPropertyValue("file:content");
        assertNotNull(newFileBlob);
        assertEquals("New file.odt", newFileBlob.getFilename());
        assertEquals("This is the content of a new file.",
                newFileBlob.getString());

        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + newFileDoc.getId(),
                newFile.getId());
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + subFolder1.getId(),
                newFile.getParentId());
        assertEquals("New file.odt", newFile.getName());
        assertFalse(newFile.isFolder());
        assertEquals("Administrator", newFile.getCreator());
        assertTrue(newFile.getCanRename());
        assertTrue(newFile.getCanDelete());
        assertEquals(
                "http://my-server/nuxeo/nxbigfile/test/" + newFileDoc.getId()
                        + "/blobholder:0/New%20file.odt",
                newFile.getDownloadURL("http://my-server/nuxeo/"));
    }
}
