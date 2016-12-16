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
 *     <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
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
        testCollection.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        collections = collectionManager.getVisibleCollection(testFile, 2, session);
        assertEquals(1, collections.size());
        assertEquals(testCollection2.getId(), collections.get(0).getId());

        // Restore collection from the trash
        testCollection.followTransition(LifeCycleConstants.UNDELETE_TRANSITION);
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
