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
import org.nuxeo.ecm.core.api.VersioningOption;

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
     * Tests that we can add a collection with invalid names
     */
    @Test
    public void testAddCollectionWithInvalidName() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection("not/valid", COLLECTION_DESCRIPTION, testFile, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));
        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + "not-valid";

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

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
        DocumentModel copiedTestFile = session.copy(testFile.getRef(), testFile.getParentRef(),
                TEST_FILE_NAME + "_BIS");

        copiedTestFile = session.getDocument(copiedTestFile.getRef());
        assertFalse(collectionManager.isCollected(copiedTestFile));

        // Let's add to another collection and see it still does not belong to the original one.
        collectionManager.addToNewCollection(COLLECTION_NAME + "_BIS", COLLECTION_DESCRIPTION + "_BIS", copiedTestFile,
                session);

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

    /**
     * @since 8.4
     */
    @Test
    public void testAddVersionToNewCollectionAndRemove() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        DocumentRef refVersion = testFile.checkIn(VersioningOption.MAJOR, "blbabla");
        DocumentModel version = session.getDocument(refVersion);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, version, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);
        final String newlyCreatedCollectionId = newlyCreatedCollection.getId();

        assertEquals(COLLECTION_NAME, newlyCreatedCollection.getTitle());

        assertEquals(COLLECTION_DESCRIPTION, newlyCreatedCollection.getProperty("dc:description").getValue());

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        assertTrue(collectionAdapter.getCollectedDocumentIds().contains(version.getId()));

        version = session.getDocument(version.getRef());

        CollectionMember collectionMemberAdapter = version.getAdapter(CollectionMember.class);

        assertTrue(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));

        collectionManager.removeFromCollection(newlyCreatedCollection, version, session);

        assertFalse(collectionAdapter.getCollectedDocumentIds().contains(testFile.getId()));
        assertFalse(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));
    }

    /**
     * @since 8.4
     */
    @Test
    public void testAddVProxyToNewCollectionAndRemove() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);

        PathRef sectionsRootRef = new PathRef("/default-domain/sections");
        assertTrue(session.exists(sectionsRootRef));
        DocumentModel sectionDoc = session.getDocument(sectionsRootRef);
        sectionDoc = session.createDocumentModel("Section");
        sectionDoc.setPathInfo(sectionDoc.getPathAsString(), "section1");
        sectionDoc = session.createDocument(sectionDoc);

        DocumentModel proxy = session.publishDocument(testFile, sectionDoc);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, proxy, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);
        final String newlyCreatedCollectionId = newlyCreatedCollection.getId();

        assertEquals(COLLECTION_NAME, newlyCreatedCollection.getTitle());

        assertEquals(COLLECTION_DESCRIPTION, newlyCreatedCollection.getProperty("dc:description").getValue());

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        assertTrue(collectionAdapter.getCollectedDocumentIds().contains(proxy.getId()));

        proxy = session.getDocument(proxy.getRef());

        CollectionMember collectionMemberAdapter = proxy.getAdapter(CollectionMember.class);

        assertTrue(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));

        collectionManager.removeFromCollection(newlyCreatedCollection, proxy, session);

        assertFalse(collectionAdapter.getCollectedDocumentIds().contains(testFile.getId()));
        assertFalse(collectionMemberAdapter.getCollectionIds().contains(newlyCreatedCollectionId));
    }

    /*
     * NXP-22085
     */
    @Test
    public void testAddOneDocToNewCollectionAndRemoveDontTriggerAutomaticVersioning() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // use note as they are versioned after each update
        DocumentModel testNote = session.createDocumentModel(testWorkspace.getPathAsString(), "Note 1", "Note");
        testNote = session.createDocument(testNote);

        List<DocumentModel> versions = session.getVersions(testNote.getRef());
        // there should be one version created after document creation
        assertEquals(1, versions.size());

        // check out the note as collection manager disable auto checkout
        testNote.checkOut();

        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testNote, session);

        versions = session.getVersions(testNote.getRef());
        // there should be one version created after document creation
        assertEquals(1, versions.size());

        DocumentModel newlyCreatedCollection = session.getDocument(
                new PathRef(COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME));

        testNote = session.getDocument(testNote.getRef());

        collectionManager.removeFromCollection(newlyCreatedCollection, testNote, session);

        versions = session.getVersions(testNote.getRef());
        // there should be one version created after document creation
        assertEquals(1, versions.size());

        // a real edition should version the note
        testNote.setPropertyValue("note:note", "new content");
        session.saveDocument(testNote);

        versions = session.getVersions(testNote.getRef());
        // there should be two versions
        // - created after document creation
        // - created after document edition
        assertEquals(2, versions.size());
    }

    /*
     * NXP-22085
     */
    @Test
    public void testCopyDocumentInACollectionDontCreateAVersionOnClean() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // use note as they are versioned after each update
        DocumentModel testNote = session.createDocumentModel(testWorkspace.getPathAsString(), "Note 1", "Note");
        testNote = session.createDocument(testNote);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testNote, session);
        DocumentModel copiedTestNote = session.copy(testNote.getRef(), testNote.getParentRef(),
                TEST_FILE_NAME + "_BIS");

        copiedTestNote = session.getDocument(copiedTestNote.getRef());

        List<DocumentModel> versions = session.getVersions(copiedTestNote.getRef());
        // there should be 0 version as copy doesn't trigger versioning service
        assertEquals(0, versions.size());
    }

}
