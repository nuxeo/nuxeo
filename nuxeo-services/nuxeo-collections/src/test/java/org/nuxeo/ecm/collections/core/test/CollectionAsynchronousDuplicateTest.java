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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @since 5.9.3
 */
public class CollectionAsynchronousDuplicateTest extends CollectionTestCase {

    private static final String TEST_COLLECTION_IN_FOLDER = "testCollectionInFolder";

    private static final String TEST_FOLDER = "testFolder";

    /**
     * Test the copy of a folder containing a Collection with many members that are descendants of the given folder.
     *
     * @throws InterruptedException
     * @since 8.4
     */
    @Test
    public void testCopyFolderContainingACollectionWithManyMembers() throws InterruptedException {
        testCopyFolderContainingACollection(MAX_CARDINALITY);
    }

    /**
     * Test the copy of a folder containing a Collection with a single members that are descendants of the given folder.
     *
     * @throws InterruptedException
     * @since 8.4
     */
    @Test
    public void testCopyFolderContainingACollectionWithOneMember() throws InterruptedException {
        testCopyFolderContainingACollection(1);
    }

    /**
     * @since 8.4
     */
    protected void testCopyFolderContainingACollection(int nbMembers) throws InterruptedException {

        List<DocumentModel> files = new ArrayList<DocumentModel>();
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel folder = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FOLDER, "Folder");
        folder = session.createDocument(folder);
        DocumentModel memberFolder = session.createDocumentModel(folder.getPathAsString(), "MemberFolder", "Folder");
        memberFolder = session.createDocument(memberFolder);
        for (int i = 1; i <= nbMembers; i++) {
            DocumentModel testFile = session.createDocumentModel(memberFolder.getPath().toString(), TEST_FILE_NAME + i,
                    "File");
            testFile = session.createDocument(testFile);
            files.add(testFile);
        }

        DocumentModel collectionInFolder = session.createDocumentModel(folder.getPathAsString(),
                TEST_COLLECTION_IN_FOLDER, CollectionConstants.COLLECTION_TYPE);
        collectionInFolder = session.createDocument(collectionInFolder);
        session.save();
        collectionManager.addToCollection(collectionInFolder, files, session);

        DocumentModel copiedFolder = session.copy(folder.getRef(), testWorkspace.getRef(), TEST_FOLDER + "_BIS");
        awaitCollectionWorks();

        String copiedCollectionPath = copiedFolder.getPathAsString() + "/" + TEST_COLLECTION_IN_FOLDER;
        PathRef copiedCollectionPathRef = new PathRef(copiedCollectionPath);
        DocumentModel copiedCollection = session.getDocument(copiedCollectionPathRef);
        final String copiedCollectionId = copiedCollection.getId();
        Collection copiedCollectionAdapter = copiedCollection.getAdapter(Collection.class);
        for (DocumentModel file : files) {
            assertTrue(copiedCollectionAdapter.getCollectedDocumentIds().contains(file.getId()));
            CollectionMember collectionMemberAdapter = session.getDocument(file.getRef())
                                                              .getAdapter(CollectionMember.class);
            assertTrue(collectionMemberAdapter.getCollectionIds().contains(copiedCollectionId));
        }

        String copiedMemberFolderPath = copiedFolder.getPathAsString() + "/MemberFolder";
        PathRef copiedMemberFolderPathRef = new PathRef(copiedMemberFolderPath);
        DocumentModelIterator it = session.getChildrenIterator(copiedMemberFolderPathRef);
        int size = 0;
        for (DocumentModel copiedMember : it) {
            assertFalse(collectionManager.isCollected(copiedMember));
            size++;
        }
        assertEquals(nbMembers, size);
    }

    @Test
    public void testCopyCollectionWithManyItems() throws InterruptedException {
        updateCollectionMemberOnCollectionDuplicated(createTestFiles(session, MAX_CARDINALITY));
    }

    @Test
    public void testCopyCollectionWithOneItem() throws InterruptedException {
        updateCollectionMemberOnCollectionDuplicated(createTestFiles(session, 1));
    }

    protected void updateCollectionMemberOnCollectionDuplicated(final List<DocumentModel> docs)
            throws InterruptedException {
        List<DocumentModel> files = docs;

        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, files, session);

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;

        final DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);

        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

        final String newCollectionId = newlyCreatedCollection.getId();

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        for (DocumentModel file : files) {

            file = session.getDocument(file.getRef());

            assertTrue(collectionAdapter.getCollectedDocumentIds().contains(file.getId()));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollectionIds().contains(newCollectionId));
        }

        DocumentRef newCollectionRefBis = null;
        session.copy(newlyCreatedCollection.getRef(), new PathRef(COLLECTION_FOLDER_PATH), COLLECTION_NAME + "_BIS");

        awaitCollectionWorks();

        final String newlyCreatedCollectionPathBis = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME + "_BIS";
        newCollectionRefBis = new PathRef(newlyCreatedCollectionPathBis);

        DocumentModel newlyCreatedCollectionBis = session.getDocument(newCollectionRefBis);

        final String newCollectionIdBis = newlyCreatedCollectionBis.getId();

        Collection collectionAdapterBis = newlyCreatedCollectionBis.getAdapter(Collection.class);

        for (DocumentModel file : files) {

            assertTrue(collectionAdapterBis.getCollectedDocumentIds().contains(file.getId()));

            CollectionMember collectionMemberAdapter = session.getDocument(file.getRef())
                                                              .getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollectionIds().contains(newCollectionIdBis));
        }
    }

}
