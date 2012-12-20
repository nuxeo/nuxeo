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
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the {@link FileSystemItem} related operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestFileSystemItemOperations {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected Session clientSession;

    protected ObjectMapper mapper;

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

        // Create 1 doc in each sync root
        doc1 = session.createDocumentModel("/folder1", "doc1", "File");
        org.nuxeo.ecm.core.api.Blob blob = new StringBlob(
                "The content of file 1.");
        blob.setFilename("First file.odt");
        doc1.setPropertyValue("file:content", (Serializable) blob);
        doc1 = session.createDocument(doc1);
        doc2 = session.createDocumentModel("/folder2", "doc2", "File");
        blob = new StringBlob("The content of file 2.");
        blob.setFilename("Second file.odt");
        doc2.setPropertyValue("file:content", (Serializable) blob);
        doc2 = session.createDocument(doc2);

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

        TransactionHelper.startTransaction();
        try {
            assertNotNull(topLevelChildren);
            assertEquals(2, topLevelChildren.size());

            DefaultSyncRootFolderItem child = topLevelChildren.get(0);
            assertEquals(
                    "defaultSyncRootFolderItemFactory/test/"
                            + syncRoot1.getId(), child.getId());
            assertTrue(child.getParentId().endsWith(
                    "DefaultTopLevelFolderItemFactory/"));
            assertEquals("folder1", child.getName());
            assertTrue(child.isFolder());
            assertEquals("Administrator", child.getCreator());
            assertFalse(child.getCanRename());
            assertTrue(child.getCanDelete());
            assertTrue(child.getCanCreateChild());

            child = topLevelChildren.get(1);
            assertEquals(
                    "defaultSyncRootFolderItemFactory/test/"
                            + syncRoot2.getId(), child.getId());
            assertEquals(
                    "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory/",
                    child.getParentId());
            assertEquals("folder2", child.getName());
            assertTrue(child.isFolder());
            assertEquals("Administrator", child.getCreator());
            assertFalse(child.getCanRename());
            assertTrue(child.getCanDelete());
            assertTrue(child.getCanCreateChild());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Test
    public void testFileSystemItemExists() throws Exception {

        // Non existing file system item
        Blob fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                "defaultSyncRootFolderItemFactory/test/badId").execute();
        assertNotNull(fileSystemItemExistsJSON);

        String fileSystemItemExists = mapper.readValue(
                fileSystemItemExistsJSON.getStream(), String.class);
        assertEquals("false", fileSystemItemExists);

        // Existing file system item
        fileSystemItemExistsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveFileSystemItemExists.ID).set("id",
                "defaultSyncRootFolderItemFactory/test/" + syncRoot1.getId()).execute();
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
                "defaultSyncRootFolderItemFactory/test/" + syncRoot1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        DefaultSyncRootFolderItem fileSystemItem = mapper.readValue(
                fileSystemItemJSON.getStream(), DefaultSyncRootFolderItem.class);
        assertNotNull(fileSystemItem);
        assertEquals(
                "defaultSyncRootFolderItemFactory/test/" + syncRoot1.getId(),
                fileSystemItem.getId());
        assertTrue(fileSystemItem.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertEquals("folder1", fileSystemItem.getName());
        assertTrue(fileSystemItem.isFolder());
        assertEquals("Administrator", fileSystemItem.getCreator());
        assertFalse(fileSystemItem.getCanRename());
        assertTrue(fileSystemItem.getCanDelete());
        assertTrue(fileSystemItem.getCanCreateChild());

        // Get doc in sync root
        fileSystemItemJSON = (Blob) clientSession.newRequest(
                NuxeoDriveGetFileSystemItem.ID).set("id",
                "defaultFileSystemItemFactory/test/" + doc1.getId()).execute();
        assertNotNull(fileSystemItemJSON);

        DocumentBackedFileItem fileSystemItem1 = mapper.readValue(
                fileSystemItemJSON.getStream(), DocumentBackedFileItem.class);
        assertNotNull(fileSystemItem1);
        assertEquals("defaultFileSystemItemFactory/test/" + doc1.getId(),
                fileSystemItem1.getId());
        assertEquals(
                "defaultSyncRootFolderItemFactory/test/" + syncRoot1.getId(),
                fileSystemItem1.getParentId());
        assertEquals("First file.odt", fileSystemItem1.getName());
        assertFalse(fileSystemItem1.isFolder());
        assertEquals("Administrator", fileSystemItem1.getCreator());
        assertTrue(fileSystemItem1.getCanRename());
        assertTrue(fileSystemItem1.getCanDelete());
        assertEquals("http://my-server/nuxeo/nxbigfile/test/" + doc1.getId()
                + "/blobholder:0/First%20file.odt",
                fileSystemItem1.getDownloadURL("http://my-server/nuxeo/"));
    }
}
