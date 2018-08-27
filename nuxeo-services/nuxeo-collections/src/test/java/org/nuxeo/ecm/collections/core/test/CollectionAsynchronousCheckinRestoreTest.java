/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Test versioning on Collections.
 *
 * @since 7.3
 */
public class CollectionAsynchronousCheckinRestoreTest extends CollectionTestCase {

    protected void testCheckingAndResotreCollection(final List<DocumentModel> docs) throws InterruptedException {
        List<DocumentModel> firstMembers = docs.subList(0, docs.size() / 2);
        List<DocumentModel> secondMembers = docs.subList(docs.size() / 2, docs.size());

        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, firstMembers, session);

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME;

        final DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);

        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        final String newCollectionId = newlyCreatedCollection.getId();

        for (DocumentModel file : firstMembers) {

            file = session.getDocument(file.getRef());

            assertTrue(collectionAdapter.getCollectedDocumentIds().contains(file.getId()));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollectionIds().contains(newCollectionId));
        }

        // Checkin collection
        DocumentRef checkoutCollectionRef = null;
        checkoutCollectionRef = newlyCreatedCollection.checkIn(VersioningOption.MAJOR, "a new version");
        awaitCollectionWorks();

        // Check that version's members are the same
        DocumentModel checkoutCollection = session.getDocument(checkoutCollectionRef);
        final String checkoutCollectionId = checkoutCollection.getId();
        Collection checkoutCollectionAdapater = checkoutCollection.getAdapter(Collection.class);
        for (DocumentModel file : firstMembers) {
            assertTrue(checkoutCollectionAdapater.getCollectedDocumentIds().contains(file.getId()));
            CollectionMember collectionMemberAdapter = session.getDocument(file.getRef()).getAdapter(
                    CollectionMember.class);
            assertTrue(collectionMemberAdapter.getCollectionIds().contains(checkoutCollectionId));
        }
        firstMembers = refresh(session, firstMembers);
        secondMembers = refresh(session, secondMembers);

        // Add new members to working copy of collection
        collectionManager.addToCollection(newlyCreatedCollection, secondMembers, session);
        // Remove old members from working copy of collection
        collectionManager.removeAllFromCollection(newlyCreatedCollection, firstMembers, session);

        // Restore collection
        session.restoreToVersion(newCollectionRef, checkoutCollectionRef, true, true);
        session.save();
        awaitCollectionWorks();
        newlyCreatedCollection = session.getDocument(newCollectionRef);

        // Check members are correctly restored
        collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);
        for (DocumentModel file : firstMembers) {
            assertTrue(collectionAdapter.getCollectedDocumentIds().contains(file.getId()));
            CollectionMember collectionMemberAdapter = session.getDocument(file.getRef()).getAdapter(
                    CollectionMember.class);
            assertTrue(collectionMemberAdapter.getCollectionIds().contains(newCollectionId));
        }
        for (DocumentModel file : secondMembers) {
            assertFalse(collectionAdapter.getCollectedDocumentIds().contains(file.getId()));
            CollectionMember collectionMemberAdapter = session.getDocument(file.getRef()).getAdapter(
                    CollectionMember.class);
            assertFalse(collectionMemberAdapter.getCollectionIds().contains(newCollectionId));
        }
    }

    protected List<DocumentModel> refresh(CoreSession session, List<DocumentModel> docs) {
        List<DocumentModel> result = new ArrayList<DocumentModel>(docs.size());
        for (DocumentModel doc : docs) {
            result.add(session.getDocument(doc.getRef()));
        }
        return result;
    }

    @Test
    public void testCheckinAndRestoreCollectionWithManyItems() throws InterruptedException {
        testCheckingAndResotreCollection(createTestFiles(session, MAX_CARDINALITY * 2));
    }

}
