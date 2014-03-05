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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.collections.core.worker.DuplicateCollectionMemberWork;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.9.3
 */@TransactionalConfig(autoStart = false)
public class CollectionAsynchronousDuplicateTest extends CollectionTestCase {

    @Inject
    WorkManager workManager;

    @Test
    public void testUpdateCollectionMemberOnCollectionDuplicated()
            throws ClientException, InterruptedException {

        List<DocumentModel> files = null;

        TransactionHelper.startTransaction();
        try {

            files = createTestFiles(MAX_CARDINALITY);

            collectionManager.addToNewCollection(COLLECTION_NAME,
                    COLLECTION_DESCRIPTION, files, session);

            final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH
                    + "/" + COLLECTION_NAME;

            final DocumentRef newCollectionRef = new PathRef(
                    newlyCreatedCollectionPath);

            assertTrue(session.exists(newCollectionRef));

            DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

            final String newCollectionId = newlyCreatedCollection.getId();

            Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

            for (DocumentModel file : files) {

                assertTrue(collectionAdapter.getCollectedDocuments().contains(
                        file));

                CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

                assertTrue(collectionMemberAdapter.getCollectionIds().contains(
                        newCollectionId));
            }

            session.copy(newlyCreatedCollection.getRef(), new PathRef(
                    COLLECTION_FOLDER_PATH), COLLECTION_NAME + "_BIS");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        workManager.awaitCompletion(DuplicateCollectionMemberWork.CATEGORY,
                WORK_TIME_OUT_MS, TimeUnit.MILLISECONDS);

        assertEquals(0, workManager.getQueueSize(DuplicateCollectionMemberWork.CATEGORY, null));

        TransactionHelper.startTransaction();

        try {
            assertEquals(0, workManager.getQueueSize(
                    DuplicateCollectionMemberWork.CATEGORY, null));

            final String newlyCreatedCollectionPathBis = COLLECTION_FOLDER_PATH
                    + "/" + COLLECTION_NAME + "_BIS";

            final DocumentRef newCollectionRefBis = new PathRef(
                    newlyCreatedCollectionPathBis);

            DocumentModel newlyCreatedCollectionBis = session.getDocument(newCollectionRefBis);

            final String newCollectionIdBis = newlyCreatedCollectionBis.getId();

            Collection collectionAdapterBis = newlyCreatedCollectionBis.getAdapter(Collection.class);

            for (DocumentModel file : files) {

                assertTrue(collectionAdapterBis.getCollectedDocuments().contains(
                        file));

                CollectionMember collectionMemberAdapter =  session.getDocument(
                        file.getRef()).getAdapter(CollectionMember.class);

                assertTrue(collectionMemberAdapter.getCollectionIds().contains(
                        newCollectionIdBis));
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

}
