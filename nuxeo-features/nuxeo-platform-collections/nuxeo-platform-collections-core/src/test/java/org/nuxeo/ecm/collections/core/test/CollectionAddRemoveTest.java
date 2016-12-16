/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @since 5.9.3
 */
public class CollectionAddRemoveTest extends CollectionTestCase {

    @Test
    public void testAddOneDocToNewCollectionAndRemove() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testFile, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);
        final String newlyCreatedCollectionId = newlyCreatedCollection.getId();

        assertEquals(COLLECTION_NAME, newlyCreatedCollection.getTitle());

        assertEquals(COLLECTION_DESCRIPTION, newlyCreatedCollection.getProperty("dc:description").getValue());

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        assertTrue(collectionAdapter.getCollectedDocumentIds().contains(testFile.getId()));

        testFile = session.getDocument(testFile.getRef());

        CollectionMember collectionMemberAdapter = testFile.getAdapter(CollectionMember.class);

        assertTrue(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));

        collectionManager.removeFromCollection(newlyCreatedCollection, testFile, session);

        assertFalse(collectionAdapter.getCollectedDocumentIds().contains(testFile.getId()));
        assertFalse(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));
    }

    @Test
    public void testAddManyDocsToNewCollectionAndRemove() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);

        List<DocumentModel> files = createTestFiles(session, 3);

        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, files, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);
        final String newlyCreatedCollectionId = newlyCreatedCollection.getId();

        assertEquals(COLLECTION_NAME, newlyCreatedCollection.getTitle());

        assertEquals(COLLECTION_DESCRIPTION, newlyCreatedCollection.getProperty("dc:description").getValue());

        for (DocumentModel file : files) {
            file = session.getDocument(file.getRef());

            Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

            assertTrue(collectionAdapter.getCollectedDocumentIds().contains(file.getId()));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));
        }

        collectionManager.removeAllFromCollection(newlyCreatedCollection, files, session);

        for (DocumentModel file : files) {
            Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

            assertFalse(collectionAdapter.getCollectedDocumentIds().contains(file.getId()));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertFalse(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));
        }

    }

    /**
     * Tests that we cannot add a document of type Collection to a document of Collection.
     */
    @Test
    public void testCanAddToNotCollection() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testCollection1 = session.createDocumentModel(testWorkspace.getPathAsString(), COLLECTION_NAME,
                "Collection");
        testCollection1 = session.createDocument(testCollection1);

        DocumentModel testCollection2 = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME + 2,
                "Collection");
        testCollection2 = session.createDocument(testCollection2);

        try {
            collectionManager.addToCollection(testCollection1, testCollection2, session);
        } catch (IllegalArgumentException e) {
            // Expeted behaviour
            return;
        }
        fail("File is not a Collection");
    }

    /**
     * Tests that we cannot add a document to a document which is not a document of type Collection.
     */
    @Test
    public void testCanAddCollectionNotCollection() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testFile, session);

        DocumentModel testFile2 = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME + 2,
                "File");
        testFile2 = session.createDocument(testFile2);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testFile2, session);

        try {
            collectionManager.addToCollection(testFile, testFile2, session);
        } catch (IllegalArgumentException e) {
            // Expeted behaviour
            return;
        }
        fail("File is not a Collection");
    }

    /**
     * Check that a copied document does not belong to the collections of the original documents.
     *
     * @since 7.3
     */
    @Test
    public void testCopiedCollectionMember() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testFile, session);
        testFile = session.getDocument(testFile.getRef());
        DocumentModel copiedTestFile = session.copy(testFile.getRef(), testFile.getParentRef(),
                TEST_FILE_NAME + "_BIS");

        copiedTestFile = session.getDocument(copiedTestFile.getRef());
        assertFalse(collectionManager.isCollected(copiedTestFile));

        // Let's add to another collection and see it still does not belong to the original one.
        collectionManager.addToNewCollection(COLLECTION_NAME + "_BIS", COLLECTION_DESCRIPTION + "_BIS", copiedTestFile,
                session);

        copiedTestFile = session.getDocument(copiedTestFile.getRef());

        final String collectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;
        DocumentRef collectionPathRef = new PathRef(collectionPath);
        assertTrue(session.exists(collectionPathRef));
        final String collectionPathBis = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME + "_BIS";
        DocumentRef collectionPathRefBis = new PathRef(collectionPathBis);
        assertTrue(session.exists(collectionPathRefBis));

        final DocumentModel collection = session.getDocument(collectionPathRef);
        final DocumentModel collectionBis = session.getDocument(collectionPathRefBis);

        assertFalse(copiedTestFile.getAdapter(CollectionMember.class).getCollectionIds().contains(collection.getId()));
        assertTrue(
                copiedTestFile.getAdapter(CollectionMember.class).getCollectionIds().contains(collectionBis.getId()));
    }

}
