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
 *     <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @since 6.0
 */
public class VisibleCollectionTest extends CollectionTestCase {

    protected static final String COLLECTION_NAME_2 = "testCollection2";

    @Test
    public void testGetVisibleCollection() throws Exception {
        // Create a test doc and add it to 2 collections
        DocumentModel testFile = session.createDocumentModel("/", TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME, COLLECTION_DESCRIPTION, testFile, session);
        collectionManager.addToNewCollection(COLLECTION_NAME_2, COLLECTION_DESCRIPTION, testFile, session);

        // Check visible collections limited to 1
        List<DocumentModel> collections = collectionManager.getVisibleCollection(testFile, 1, session);
        assertEquals(1, collections.size());
        DocumentModel testCollection = session.getDocument(new PathRef(COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME));
        assertEquals(testCollection.getId(), collections.get(0).getId());

        // Check visible collections limited to 2
        collections = collectionManager.getVisibleCollection(testFile, 2, session);
        assertEquals(2, collections.size());
        DocumentModel testCollection2 = session.getDocument(
                new PathRef(COLLECTION_FOLDER_PATH + "/" + COLLECTION_NAME_2));
        assertEquals(testCollection.getId(), collections.get(0).getId());
        assertEquals(testCollection2.getId(), collections.get(1).getId());

        // Send one collection to the trash
        trashService.trashDocument(testCollection);
        collections = collectionManager.getVisibleCollection(testFile, 2, session);
        assertEquals(1, collections.size());
        assertEquals(testCollection2.getId(), collections.get(0).getId());

        // Restore collection from the trash
        trashService.untrashDocument(testCollection);
        collections = collectionManager.getVisibleCollection(testFile, 2, session);
        assertEquals(2, collections.size());
        assertEquals(testCollection.getId(), collections.get(0).getId());
        assertEquals(testCollection2.getId(), collections.get(1).getId());

        // Delete one collection permanently
        session.removeDocument(testCollection.getRef());
        collections = collectionManager.getVisibleCollection(testFile, 1, session);
        assertEquals(1, collections.size());
        assertEquals(testCollection2.getId(), collections.get(0).getId());
    }

}
