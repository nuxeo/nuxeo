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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.automation.core.test.collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.collections.GetCollectionsOperation;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Class testing the operation "Collection.GetCollections".
 *
 * @since 5.9.4
 */
public class GetCollectionsTest extends CollectionOperationsTestCase {

    @Inject
    CollectionManager collectionManager;

    List<DocumentModel> listCollections;

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create a list of collections
        listCollections = new ArrayList<>();
        DocumentModel collection1 = collectionManager.createCollection(session, COLLECTION_NAME + 1,
                COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection1);
        DocumentModel collection2 = collectionManager.createCollection(session, COLLECTION_NAME + 2,
                COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection2);
        DocumentModel collection3 = collectionManager.createCollection(session, COLLECTION_NAME + 3,
                COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection3);
        DocumentModel collection4 = collectionManager.createCollection(session, COLLECTION_NAME + 4,
                COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection4);

        session.save();
    }

    @Test
    public void testGetCollections() throws Exception {
        // Search the exact name of a collection
        chain = new OperationChain("test-chain");
        chain.add(GetCollectionsOperation.ID).set("searchTerm", COLLECTION_NAME + 1);

        OperationContext ctx = new OperationContext(session);
        PaginableDocumentModelListImpl collectionsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);

        assertNotNull(collectionsList);
        assertEquals(1, collectionsList.size());
        DocumentModel collection = collectionsList.get(0);
        assertEquals(COLLECTION_NAME + 1, collection.getName());
        assertEquals(COLLECTION_DESCRIPTION, collection.getPropertyValue("dc:description"));

        // Search several collections
        chain = new OperationChain("test-chain-2");
        chain.add(GetCollectionsOperation.ID).set("searchTerm", COLLECTION_NAME + "%");
        collectionsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);

        assertNotNull(collectionsList);
        assertEquals(listCollections.size(), collectionsList.size());

        // Search a non-existing collection
        chain = new OperationChain("test-chain-3");
        chain.add(GetCollectionsOperation.ID).set("searchTerm", "NonExistenting");
        collectionsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);

        assertNotNull(collectionsList);
        assertEquals(0, collectionsList.size());
    }

}
