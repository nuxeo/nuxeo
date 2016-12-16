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

import java.util.ArrayList;
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

        List<DocumentRef> toBePurged = new ArrayList<DocumentRef>();
        toBePurged.add(newCollectionRef);
        trashService.purgeDocuments(session, toBePurged);

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

        List<DocumentRef> toBePurged = new ArrayList<DocumentRef>();
        toBePurged.add(new PathRef(testFile.getPath().toString()));
        trashService.purgeDocuments(session, toBePurged);

        awaitCollectionWorks();

        for (int i = 1; i <= nbCollection; i++) {
            DocumentModel collectionModel = session.getDocument(
                    new PathRef(COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME + i));
            Collection collection = collectionModel.getAdapter(Collection.class);
            assertFalse(collection.getCollectedDocumentIds().contains(testFileId));
        }

    }

}
