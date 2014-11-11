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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.collections.core.test.operations;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.automation.GetCollectionsOperation;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.google.inject.Inject;

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
    public void setUp() throws ClientException {
        testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create a list of collections
        listCollections = new ArrayList<>();
        DocumentModel collection1 = collectionManager.createCollection(session,
                COLLECTION_NAME + 1, COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection1);
        DocumentModel collection2 = collectionManager.createCollection(session,
                COLLECTION_NAME + 2, COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection2);
        DocumentModel collection3 = collectionManager.createCollection(session,
                COLLECTION_NAME + 3, COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
        listCollections.add(collection3);
        DocumentModel collection4 = collectionManager.createCollection(session,
                COLLECTION_NAME + 4, COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
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
