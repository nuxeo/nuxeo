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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.TransactionalFeature;
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
public class CollectionAddRemoveTest {

    private static final String TEST_FILE_NAME = "testFile";

    private static final String COLLECTION_NAME = "testCollection";

    private static final String COLLECTION_DESCRIPTION = "dummy";

    private static final String COLLECTION_FOLDER_PATH = "/default-domain/UserWorkspaces/Administrator/"
            + CollectionConstants.DEFAULT_COLLECTIONS_NAME;

    @Inject
    CoreSession session;

    @Inject
    CollectionManager collectionManager;

    @Test
    public void testAddOneDocToNewCollectionAndRemove() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(
                testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME,
                COLLECTION_DESCRIPTION, testFile, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/"
                + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

        assertEquals(COLLECTION_NAME, newlyCreatedCollection.getTitle());

        assertEquals(COLLECTION_DESCRIPTION,
                newlyCreatedCollection.getProperty("dc:description").getValue());

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        assertTrue(collectionAdapter.getCollectedDocuments().contains(testFile));

        CollectionMember collectionMemberAdapter = testFile.getAdapter(CollectionMember.class);

        assertTrue(collectionMemberAdapter.getCollections().contains(
                newlyCreatedCollection));

        collectionManager.removeFromCollection(newlyCreatedCollection,
                testFile, session);

        assertFalse(collectionAdapter.getCollectedDocuments().contains(testFile));
        assertFalse(collectionMemberAdapter.getCollections().contains(
                newlyCreatedCollection));
    }

    @Test
    public void testAddManyDocsToNewCollectionAndRemove()
            throws ClientException {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);

        List<DocumentModel> files = createTestFiles(
                testWorkspace.getPath().toString(), 3);

        collectionManager.addToNewCollection(COLLECTION_NAME,
                COLLECTION_DESCRIPTION, files, session);

        assertTrue(session.exists(new PathRef(COLLECTION_FOLDER_PATH)));

        final String newlyCreatedCollectionPath = COLLECTION_FOLDER_PATH + "/"
                + COLLECTION_NAME;

        DocumentRef newCollectionRef = new PathRef(newlyCreatedCollectionPath);
        assertTrue(session.exists(newCollectionRef));

        DocumentModel newlyCreatedCollection = session.getDocument(newCollectionRef);

        assertEquals(COLLECTION_NAME, newlyCreatedCollection.getTitle());

        assertEquals(COLLECTION_DESCRIPTION,
                newlyCreatedCollection.getProperty("dc:description").getValue());

        for (DocumentModel file : files) {
            Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

            assertTrue(collectionAdapter.getCollectedDocuments().contains(file));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertTrue(collectionMemberAdapter.getCollections().contains(
                    newlyCreatedCollection));
        }

        collectionManager.removeAllFromCollection(newlyCreatedCollection,
                files, session);

        for (DocumentModel file : files) {
            Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

            assertFalse(collectionAdapter.getCollectedDocuments().contains(file));

            CollectionMember collectionMemberAdapter = file.getAdapter(CollectionMember.class);

            assertFalse(collectionMemberAdapter.getCollections().contains(
                    newlyCreatedCollection));
        }

    }

    /**
     * Tests that we cannot add a document of type Collection to a document of
     * Collection.
     */
    @Test
    public void testCanAddToNotCollection() throws ClientException {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testCollection1 = session.createDocumentModel(
                testWorkspace.getPathAsString(), COLLECTION_NAME, "Collection");
        testCollection1 = session.createDocument(testCollection1);

        DocumentModel testCollection2 = session.createDocumentModel(
                testWorkspace.getPathAsString(), TEST_FILE_NAME + 2, "Collection");
        testCollection2 = session.createDocument(testCollection2);

        try {
            collectionManager.addToCollection(testCollection1, testCollection2,
                    session);
        } catch (IllegalArgumentException e) {
            // Expeted behaviour
            return;
        }
        fail("File is not a Collection");
    }

    /**
     * Tests that we cannot add a document to a document which is not a document
     * of type Collection.
     */
    @Test
    public void testCanAddCollectionNotCollection() throws ClientException {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(
                testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME,
                COLLECTION_DESCRIPTION, testFile, session);

        DocumentModel testFile2 = session.createDocumentModel(
                testWorkspace.getPathAsString(), TEST_FILE_NAME + 2, "File");
        testFile2 = session.createDocument(testFile2);
        collectionManager.addToNewCollection(COLLECTION_NAME,
                COLLECTION_DESCRIPTION, testFile2, session);

        try {
            collectionManager.addToCollection(testFile, testFile2, session);
        } catch (IllegalArgumentException e) {
            // Expeted behaviour
            return;
        }
        fail("File is not a Collection");
    }

    private List<DocumentModel> createTestFiles(String parentPath, int nbFile)
            throws ClientException {
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        for (int i = 1; i <= nbFile; i++) {
            DocumentModel testFile = session.createDocumentModel(parentPath,
                    TEST_FILE_NAME + i, "File");
            testFile = session.createDocument(testFile);
            result.add(testFile);
        }
        return result;
    }

}
