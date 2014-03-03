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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.collections.core",
        "org.nuxeo.ecm.platform.userworkspace.types" })
public class CollectionAsymchronousUpdateTest extends CollectionTestCase {

    @Inject
    TrashService trashService;

    @Inject
    EventService eventService;

    @Test
    public void testUpdateCollectionMemberOnCollectionRemoved() throws ClientException {
        List<DocumentModel> files = createTestFiles(60);

        collectionManager.addToNewCollection(COLLECTION_NAME,
                COLLECTION_DESCRIPTION, files, session);

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/"
                + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        for (DocumentModel file : files) {

            assertTrue(collectionAdapter.getCollectedDocuments().contains(file));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollections().contains(
                    newlyCreatedCollection));
        }

        List<DocumentRef> toBePurged = new ArrayList<DocumentRef>();
        toBePurged.add(newCollectionRef);
        trashService.purgeDocuments(session, toBePurged);

        eventService.waitForAsyncCompletion();

        for (DocumentModel file : files) {
            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertFalse(collectionMemberAdapter.getCollections().contains(
                    newlyCreatedCollection));
        }
    }

    @Test
    public void testUpdateCollectionOnCollectionMemberRemoved() {
        // TODO
    }

    @Test
    public void testUpdateCollectionMemberOnCollectionDuplicated() {
        // TODO
    }

}
