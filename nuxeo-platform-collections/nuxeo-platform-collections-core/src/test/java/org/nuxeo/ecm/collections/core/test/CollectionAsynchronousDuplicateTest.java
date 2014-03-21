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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @since 5.9.3
 */
public class CollectionAsynchronousDuplicateTest extends CollectionTestCase {

    @Test
    public void testCopyCollectionWithManyItems()
            throws ClientException, InterruptedException {
        updateCollectionMemberOnCollectionDuplicated(createTestFiles(session,
                MAX_CARDINALITY));
    }

    @Test
    public void testCopyCollectionWithOneItem() throws ClientException,
            InterruptedException {
        updateCollectionMemberOnCollectionDuplicated(createTestFiles(session, 1));
    }

    protected void updateCollectionMemberOnCollectionDuplicated(
            final List<DocumentModel> docs) throws ClientException,
            InterruptedException {
        List<DocumentModel> files = docs;

        collectionManager.addToNewCollection(COLLECTION_NAME,
                COLLECTION_DESCRIPTION, files, session);

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/"
                + COLLECTION_NAME;

        final DocumentRef newCollectionRef = new PathRef(
                newlyCreatedCollectionPath);

        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

        final String newCollectionId = newlyCreatedCollection.getId();

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        for (DocumentModel file : files) {

            file = session.getDocument(file.getRef());

            assertTrue(collectionAdapter.getCollectedDocumentIds().contains(
                    file.getId()));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollectionIds().contains(
                    newCollectionId));
        }

        session.copy(newlyCreatedCollection.getRef(), new PathRef(
                COLLECTION_FOLDER_PATH), COLLECTION_NAME + "_BIS");

        awaitCollectionWorks();

        final String newlyCreatedCollectionPathBis = COLLECTION_FOLDER_PATH
                + "/" + COLLECTION_NAME + "_BIS";

        final DocumentRef newCollectionRefBis = new PathRef(
                newlyCreatedCollectionPathBis);

        DocumentModel newlyCreatedCollectionBis = session.getDocument(newCollectionRefBis);

        final String newCollectionIdBis = newlyCreatedCollectionBis.getId();

        Collection collectionAdapterBis = newlyCreatedCollectionBis.getAdapter(Collection.class);

        for (DocumentModel file : files) {

            assertTrue(collectionAdapterBis.getCollectedDocumentIds().contains(
                    file.getId()));

            CollectionMember collectionMemberAdapter = session.getDocument(
                    file.getRef()).getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollectionIds().contains(
                    newCollectionIdBis));
        }
    }

}
