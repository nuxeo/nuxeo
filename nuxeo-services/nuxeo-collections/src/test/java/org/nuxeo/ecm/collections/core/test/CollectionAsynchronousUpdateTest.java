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
public class CollectionAsynchronousUpdateTest extends CollectionTestCase {

    @Test
    public void testUpdateCollectionMemberOnCollectionRemoved() throws InterruptedException {

        List<DocumentModel> files = createTestFiles(session, MAX_CARDINALITY);

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

        session.removeDocument(newCollectionRef);

        awaitCollectionWorks();

        for (DocumentModel file : files) {
            CollectionMember collectionMemberAdapter = session.getDocument(file.getRef())
                                                              .getAdapter(CollectionMember.class);

            assertFalse(collectionMemberAdapter.getCollectionIds().contains(newCollectionId));
        }
    }

    @Test
    public void testUpdateCollectionOnCollectionMemberRemoved() throws InterruptedException {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);

        final String testFileId = testFile.getId();

        int nbCollection = MAX_CARDINALITY;
        for (int i = 1; i <= nbCollection; i++) {
            collectionManager.addToNewCollection(COLLECTION_NAME + i, COLLECTION_DESCRIPTION, testFile, session);
        }

        CollectionMember collectionMember = testFile.getAdapter(CollectionMember.class);

        assertEquals(nbCollection, collectionMember.getCollectionIds().size());

        session.removeDocument(testFile.getRef());

        awaitCollectionWorks();

        for (int i = 1; i <= nbCollection; i++) {
            DocumentModel collectionModel = session.getDocument(
                    new PathRef(COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME + i));
            Collection collection = collectionModel.getAdapter(Collection.class);
            assertFalse(collection.getCollectedDocumentIds().contains(testFileId));
        }

    }

}
