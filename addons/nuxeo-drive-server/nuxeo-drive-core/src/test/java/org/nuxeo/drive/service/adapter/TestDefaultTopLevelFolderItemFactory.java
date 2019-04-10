/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link DefaultTopLevelFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
public class TestDefaultTopLevelFolderItemFactory {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected TopLevelFolderItemFactory defaultTopLevelFolderItemFactory;

    @Before
    public void createTestDocs() throws Exception {

        // Create and register 2 synchronization roots for Administrator
        syncRoot1 = session.createDocument(session.createDocumentModel("/", "syncRoot1", "Folder"));
        syncRoot2 = session.createDocument(session.createDocumentModel("/", "syncRoot2", "Folder"));
        NuxeoPrincipal administrator = session.getPrincipal();
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot(administrator, syncRoot2, session);

        // Add a child file to syncRoot1
        DocumentModel syncRoot1Child = session.createDocumentModel("/syncRoot1", "syncRoot1Child", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        syncRoot1Child.setPropertyValue("file:content", (Serializable) blob);
        syncRoot1Child = session.createDocument(syncRoot1Child);

        // Flush the session so that the other session instances from the
        // FileSystemManager service.
        session.save();

        // Get default top level folder item factory
        defaultTopLevelFolderItemFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertTrue(defaultTopLevelFolderItemFactory instanceof DefaultTopLevelFolderItemFactory);
        assertEquals("Nuxeo Drive", defaultTopLevelFolderItemFactory.getFolderName());
    }

    @Test
    public void testFactory() throws Exception {

        // -------------------------------------------------------------
        // Check TopLevelFolderItemFactory#getTopLevelFolderItem(String
        // NuxeoPrincipal)
        // -------------------------------------------------------------
        FolderItem topLevelFolderItem = defaultTopLevelFolderItemFactory.getTopLevelFolderItem(session.getPrincipal());
        assertNotNull(topLevelFolderItem);
        assertTrue(topLevelFolderItem instanceof DefaultTopLevelFolderItem);
        assertTrue(topLevelFolderItem.getId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertTrue(topLevelFolderItem.getPath().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertTrue(topLevelFolderItem.getPath().startsWith("/"));
        assertNull(topLevelFolderItem.getParentId());
        assertEquals("Nuxeo Drive", topLevelFolderItem.getName());
        assertTrue(topLevelFolderItem.isFolder());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, topLevelFolderItem.getCreator());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, topLevelFolderItem.getLastContributor());
        assertFalse(topLevelFolderItem.getCanRename());
        try {
            topLevelFolderItem.rename("newName");
            fail("Should not be able to rename the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot rename a virtual folder item.", e.getMessage());
        }
        assertFalse(topLevelFolderItem.getCanDelete());
        try {
            topLevelFolderItem.delete();
            fail("Should not be able to delete the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot delete a virtual folder item.", e.getMessage());
        }
        assertFalse(topLevelFolderItem.canMove(null));
        try {
            topLevelFolderItem.move(null);
            fail("Should not be able to move the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot move a virtual folder item.", e.getMessage());
        }
        List<FileSystemItem> children = topLevelFolderItem.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        assertFalse(topLevelFolderItem.getCanScrollDescendants());
        try {
            topLevelFolderItem.scrollDescendants(null, 10, 1000);
            fail("Should not be able to scroll through the descendants of the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals(
                    "Cannot scroll through the descendants of a virtual folder item, please call getChildren() instead.",
                    e.getMessage());
        }
        assertFalse(topLevelFolderItem.getCanCreateChild());

        for (FileSystemItem child : children) {
            assertEquals(topLevelFolderItem.getPath() + '/' + child.getId(), child.getPath());
        }
        try {
            topLevelFolderItem.createFile(new StringBlob("Child file content."), false);
            fail("Should not be able to create a file in the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot create a file in a virtual folder item.", e.getMessage());
        }
        try {
            topLevelFolderItem.createFolder("subFolder", false);
            fail("Should not be able to create a folder in the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot create a folder in a virtual folder item.", e.getMessage());
        }

        // -------------------------------------------------------------
        // Check VirtualFolderItemFactory#getVirtualFolderItem(NuxeoPrincipal
        // userName)
        // -------------------------------------------------------------
        assertEquals(topLevelFolderItem, defaultTopLevelFolderItemFactory.getVirtualFolderItem(session.getPrincipal()));
    }

    /**
     * Tests the default top level folder item children, ie. the synchronization root folders.
     */
    @Test
    public void testTopLevelFolderItemChildren() {

        FolderItem topLevelFolderItem = defaultTopLevelFolderItemFactory.getTopLevelFolderItem(session.getPrincipal());
        List<FileSystemItem> children = topLevelFolderItem.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());

        FileSystemItem firstRootAsFsItem = children.get(0);
        assertTrue(firstRootAsFsItem instanceof DefaultSyncRootFolderItem);
        assertEquals("defaultSyncRootFolderItemFactory#test#" + syncRoot1.getId(), firstRootAsFsItem.getId());
        assertTrue(firstRootAsFsItem.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals("syncRoot1", firstRootAsFsItem.getName());
        assertTrue(firstRootAsFsItem.isFolder());
        assertEquals("Administrator", firstRootAsFsItem.getCreator());
        assertEquals("Administrator", firstRootAsFsItem.getLastContributor());
        assertTrue(firstRootAsFsItem.getCanRename());
        firstRootAsFsItem.rename("newName");
        assertEquals("newName", firstRootAsFsItem.getName());
        assertTrue(firstRootAsFsItem instanceof FolderItem);
        FolderItem firstRootAsFolderItem = (FolderItem) firstRootAsFsItem;
        List<FileSystemItem> childFsItemChildren = firstRootAsFolderItem.getChildren();
        assertNotNull(childFsItemChildren);
        assertEquals(1, childFsItemChildren.size());
        assertTrue(firstRootAsFolderItem.getCanCreateChild());

        FileSystemItem secondRootAsFsItem = children.get(1);
        assertTrue(secondRootAsFsItem instanceof DefaultSyncRootFolderItem);
        assertEquals("defaultSyncRootFolderItemFactory#test#" + syncRoot2.getId(), secondRootAsFsItem.getId());
        assertTrue(secondRootAsFsItem.getParentId().endsWith("DefaultTopLevelFolderItemFactory#"));
        assertEquals("syncRoot2", secondRootAsFsItem.getName());

        // Let's delete a Sync Root FS Item: this should result in a root
        // unregistration
        assertTrue(firstRootAsFsItem.getCanDelete());
        firstRootAsFsItem.delete();
        assertFalse(nuxeoDriveManager.getSynchronizationRootReferences(session).contains(new IdRef(syncRoot1.getId())));
        assertFalse(firstRootAsFsItem.canMove(null));
        try {
            firstRootAsFsItem.move(null);
            fail("Should not be able to move a synchronization root folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot move a synchronization root folder item.", e.getMessage());
        }
    }

    @Test
    public void testFileSystemItemFactory() {

        // #getName()
        assertEquals("org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory",
                defaultTopLevelFolderItemFactory.getName());
        // #setName(String name)
        defaultTopLevelFolderItemFactory.setName("testName");
        assertEquals("testName", defaultTopLevelFolderItemFactory.getName());
        defaultTopLevelFolderItemFactory.setName("org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory");
        // #isFileSystemItem(DocumentModel doc)
        DocumentModel fakeDoc = DocumentModelFactory.createDocumentModel("File");
        assertFalse(defaultTopLevelFolderItemFactory.isFileSystemItem(fakeDoc));
        // #getFileSystemItem(DocumentModel doc)
        assertNull(defaultTopLevelFolderItemFactory.getFileSystemItem(fakeDoc));
        // #canHandleFileSystemItemId(String id)
        assertTrue(defaultTopLevelFolderItemFactory.canHandleFileSystemItemId(
                "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#"));
        assertFalse(defaultTopLevelFolderItemFactory.canHandleFileSystemItemId(
                "org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory#"));
        // #exists(String id, NuxeoPrincipal principal)
        assertTrue(defaultTopLevelFolderItemFactory.exists(
                "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#", session.getPrincipal()));
        try {
            defaultTopLevelFolderItemFactory.exists("testId", session.getPrincipal());
            fail("Should be unsupported.");
        } catch (UnsupportedOperationException e) {
            assertEquals(
                    "Cannot check if a file system item exists for an id that cannot be handled from factory org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory.",
                    e.getMessage());
        }
        // #getFileSystemItemById(String id, NuxeoPrincipal principal)
        FileSystemItem topLevelFolderItem = defaultTopLevelFolderItemFactory.getFileSystemItemById(
                "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#", session.getPrincipal());
        assertNotNull(topLevelFolderItem);
        assertTrue(topLevelFolderItem instanceof DefaultTopLevelFolderItem);
        assertNull(topLevelFolderItem.getParentId());
        assertEquals("Nuxeo Drive", topLevelFolderItem.getName());
        try {
            defaultTopLevelFolderItemFactory.getFileSystemItemById("testId", session.getPrincipal());
            fail("Should be unsupported.");
        } catch (UnsupportedOperationException e) {
            assertEquals(
                    "Cannot get the file system item for an id that cannot be handled from factory org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory.",
                    e.getMessage());
        }
        // #getFileSystemItemById(String id, String parentId, NuxeoPrincipal
        // principal)
        topLevelFolderItem = defaultTopLevelFolderItemFactory.getFileSystemItemById(
                "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#", null, session.getPrincipal());
        assertTrue(topLevelFolderItem instanceof DefaultTopLevelFolderItem);
    }
}
