/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.CollectionSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.CollectionSyncRootFolderItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link CollectionSyncRootFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
public class TestCollectionSyncRootFolderItemFactory {

    private static final Logger log = LogManager.getLogger(TestCollectionSyncRootFolderItemFactory.class);

    private static final String DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX = "defaultFileSystemItemFactory#test#";

    private static final String COLLECTION_SYNC_ROOT_ITEM_ID_PREFIX = "collectionSyncRootFolderItemFactory#test#";

    @Inject
    protected CoreSession session;

    @Inject
    protected CollectionManager collectionManager;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Test
    public void testFactory() throws Exception {

        FileSystemItemFactory collectionSyncRootFolderItemFactory = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory(
                "collectionSyncRootFolderItemFactory");
        DocumentModel collection = collectionManager.createCollection(session, "testCollection", "Test collection.",
                "/");
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("dc:title", "doc1");
        doc1.setPropertyValue("file:content", new StringBlob("Content of file 1."));
        doc1 = session.createDocument(doc1);
        collectionManager.addToCollection(collection, doc1, session);
        assertTrue(collectionManager.isInCollection(collection, doc1, session));
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2.setPropertyValue("dc:title", "doc2");
        doc2.setPropertyValue("file:content", new StringBlob("Content of file 2."));
        doc2 = session.createDocument(doc2);
        collectionManager.addToCollection(collection, doc2, session);
        assertTrue(collectionManager.isInCollection(collection, doc2, session));

        log.trace("Check document that is not a Collection");
        assertFalse(collectionSyncRootFolderItemFactory.isFileSystemItem(session.getRootDocument()));
        log.trace("Check Collection not registered as a sync root");
        assertFalse(collectionSyncRootFolderItemFactory.isFileSystemItem(collection));
        log.trace("Check Collection registered as a sync root");
        nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), collection, session);
        assertTrue(collectionSyncRootFolderItemFactory.isFileSystemItem(collection));

        log.trace("Adapt test collection as a FileSystemItem");
        FileSystemItem fsItem = collectionSyncRootFolderItemFactory.getFileSystemItem(collection);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof CollectionSyncRootFolderItem);

        log.trace("Check children");
        FolderItem collectionFSItem = (FolderItem) fsItem;
        List<FileSystemItem> collectionChildren = collectionFSItem.getChildren();
        assertEquals(2, collectionChildren.size());
        FileSystemItem child1 = collectionChildren.get(0);
        assertTrue(child1 instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + doc1.getId(), child1.getId());
        assertEquals(COLLECTION_SYNC_ROOT_ITEM_ID_PREFIX + collection.getId(), child1.getParentId());
        assertEquals("doc1", child1.getName());
        FileSystemItem child2 = collectionChildren.get(1);
        assertTrue(child2 instanceof FileItem);
        assertEquals(DEFAULT_FILE_SYSTEM_ITEM_ID_PREFIX + doc2.getId(), child2.getId());
        assertEquals(COLLECTION_SYNC_ROOT_ITEM_ID_PREFIX + collection.getId(), child2.getParentId());
        assertEquals("doc2", child2.getName());

        log.trace("Check FolderItem#getCanScrollDescendants");
        assertFalse(collectionFSItem.getCanScrollDescendants());

        log.trace("Check descendants");
        try {
            collectionFSItem.scrollDescendants(null, 10, 1000);
            fail("Should not be able to scroll through the descendants of a CollectionSyncRootFolderItem.");
        } catch (UnsupportedOperationException e) {
            assertEquals(
                    "Cannot scroll through the descendants of a collection sync root folder item, please call getChildren() instead.",
                    e.getMessage());
        }

        log.trace("Check FolderItem#getCanCreateChild");
        assertFalse(collectionFSItem.getCanCreateChild());

        log.trace("Check FolderItem#createFile");
        try {
            collectionFSItem.createFile(new StringBlob("Child file content."));
            fail("Should not be able to create a file in a CollectionSyncRootFolderItem.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot create a file in a collection synchronization root.", e.getMessage());
        }

        log.trace("Check FolderItem#createFolder");
        try {
            collectionFSItem.createFolder("Child folder", false);
            fail("Should not be able to create a folder in a CollectionSyncRootFolderItem.");
        } catch (UnsupportedOperationException e) {
            assertEquals("Cannot create a folder in a collection synchronization root.", e.getMessage());
        }

        log.trace("Test AbstractDocumentBackedFileSystemItem#delete");
        child1.delete();
        doc1 = session.getDocument(doc1.getRef());
        assertFalse(doc1.isTrashed());
        assertFalse(collectionManager.isInCollection(collection, doc1, session));
    }

}
