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
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link DefaultTopLevelFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.ecm.platform.query.api" })
public class TestDefaultTopLevelFolderItemFactory {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected DocumentModel syncRoot1;

    protected DocumentModel syncRoot2;

    protected DocumentModel syncRoot1Child;

    protected TopLevelFolderItemFactory defaultTopLevelFolderItemFactory;

    @Before
    public void createTestDocs() throws Exception {

        // Create and register 2 synchronization roots for Administrator
        syncRoot1 = session.createDocument(session.createDocumentModel("/",
                "syncRoot1", "Folder"));
        syncRoot2 = session.createDocument(session.createDocumentModel("/",
                "syncRoot2", "Folder"));
        nuxeoDriveManager.registerSynchronizationRoot("Administrator",
                syncRoot1, session);
        nuxeoDriveManager.registerSynchronizationRoot("Administrator",
                syncRoot2, session);

        // Add a child file to syncRoot1
        syncRoot1Child = session.createDocumentModel("/syncRoot1",
                "syncRoot1Child", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        syncRoot1Child.setPropertyValue("file:content", (Serializable) blob);
        syncRoot1Child = session.createDocument(syncRoot1Child);

        session.save();

        // Get default top level folder item factory
        defaultTopLevelFolderItemFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertTrue(defaultTopLevelFolderItemFactory instanceof DefaultTopLevelFolderItemFactory);
    }

    @Test
    public void testFactory() throws Exception {

        // -------------------------------------------------------------
        // Check TopLevelFolderItemFactory#getTopLevelFolderItem(String
        // userName)
        // -------------------------------------------------------------
        FolderItem topLevelFolderItem = defaultTopLevelFolderItemFactory.getTopLevelFolderItem("Administrator");
        assertNotNull(topLevelFolderItem);
        assertTrue(topLevelFolderItem instanceof DefaultTopLevelFolderItem);
        assertTrue(topLevelFolderItem.getId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertNull(topLevelFolderItem.getParentId());
        assertEquals("Nuxeo Drive", topLevelFolderItem.getName());
        assertTrue(topLevelFolderItem.isFolder());
        assertEquals("system", topLevelFolderItem.getCreator());
        assertFalse(topLevelFolderItem.getCanRename());
        try {
            topLevelFolderItem.rename("newName");
            fail("Should not be able to rename the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot rename a system folder item.", e.getMessage());
        }
        assertFalse(topLevelFolderItem.getCanDelete());
        try {
            topLevelFolderItem.delete();
            fail("Should not be able to delete the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot delete a system folder item.", e.getMessage());
        }
        List<FileSystemItem> children = topLevelFolderItem.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        assertFalse(topLevelFolderItem.getCanCreateChild());
        try {
            topLevelFolderItem.createFile(new StringBlob("Child file content."));
            fail("Should not be able to create a file in the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot create a file in a system folder item.",
                    e.getMessage());
        }
        try {
            topLevelFolderItem.createFolder("subFolder");
            fail("Should not be able to create a folder in the default top level folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot create a folder in a system folder item.",
                    e.getMessage());
        }

        // ---------------------------------------------------------------------
        // Check TopLevelFolderItemFactory#getSyncRootParentFolderItemId(String
        // userName)
        // ---------------------------------------------------------------------
        String syncRootParentFolderId = defaultTopLevelFolderItemFactory.getSyncRootParentFolderItemId("Administrator");
        assertEquals(topLevelFolderItem.getId(), syncRootParentFolderId);
    }

    /**
     * Tests the default top level folder item children, ie. the synchronization
     * root folders.
     */
    @Test
    public void testTopLevelFolderItemChildren() throws ClientException {

        FolderItem topLevelFolderItem = defaultTopLevelFolderItemFactory.getTopLevelFolderItem("Administrator");
        List<FileSystemItem> children = topLevelFolderItem.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());

        FileSystemItem childFsItem = children.get(0);
        assertTrue(childFsItem instanceof DefaultSyncRootFolderItem);
        assertEquals(
                "defaultSyncRootFolderItemFactory/test/" + syncRoot1.getId(),
                childFsItem.getId());
        assertTrue(childFsItem.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertEquals("syncRoot1", childFsItem.getName());
        assertTrue(childFsItem.isFolder());
        assertEquals("Administrator", childFsItem.getCreator());
        assertFalse(childFsItem.getCanRename());
        try {
            childFsItem.rename("newName");
            fail("Should not be able to rename a synchronization root folder item.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot rename a synchronization root folder item.",
                    e.getMessage());
        }
        assertTrue(childFsItem.getCanDelete());
        childFsItem.delete();
        assertFalse(nuxeoDriveManager.getSynchronizationRootReferences(session).contains(
                new PathRef("/syncRoot1")));
        assertTrue(childFsItem instanceof FolderItem);
        FolderItem childFolderItem = (FolderItem) childFsItem;
        List<FileSystemItem> childFsItemChildren = childFolderItem.getChildren();
        assertNotNull(childFsItemChildren);
        assertEquals(1, childFsItemChildren.size());
        assertTrue(childFolderItem.getCanCreateChild());

        childFsItem = children.get(1);
        assertTrue(childFsItem instanceof DefaultSyncRootFolderItem);
        assertEquals(
                "defaultSyncRootFolderItemFactory/test/" + syncRoot2.getId(),
                childFsItem.getId());
        assertTrue(childFsItem.getParentId().endsWith(
                "DefaultTopLevelFolderItemFactory/"));
        assertEquals("syncRoot2", childFsItem.getName());
    }

}
