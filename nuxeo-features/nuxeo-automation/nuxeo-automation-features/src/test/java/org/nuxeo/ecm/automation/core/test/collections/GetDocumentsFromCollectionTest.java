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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.collections.GetDocumentsFromCollectionOperation;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Class testing the operation "Collection.GetDocumentFromCollection".
 *
 * @since 5.9.4
 */
public class GetDocumentsFromCollectionTest extends CollectionOperationsTestCase {

    @Inject
    CollectionManager collectionManager;

    private DocumentModel collection;

    private List<DocumentModel> listDocuments;

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create a new collection
        collection = collectionManager.createCollection(session, COLLECTION_NAME, COLLECTION_DESCRIPTION,
                testWorkspace.getPathAsString());
        // Create a list of test documents
        listDocuments = createTestFiles(session, 5);
        // Add them in the collection
        collectionManager.addToCollection(collection, listDocuments, session);
        session.save();
    }

    @Test
    public void testGetDocumentsFromCollection() throws Exception {
        chain = new OperationChain("test-chain");
        chain.add(GetDocumentsFromCollectionOperation.ID);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(collection);
        PaginableDocumentModelListImpl documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);
        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(listDocuments.size(), documentsList.size());

        // Remove a document from the collection and check the result of the operation
        collectionManager.removeFromCollection(collection, listDocuments.get(0), session);
        session.save();
        listDocuments.remove(0);
        chain = new OperationChain("test-chain-2");
        chain.add(GetDocumentsFromCollectionOperation.ID);

        ctx = new OperationContext(session);
        ctx.setInput(collection);
        documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);
        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(listDocuments.size(), documentsList.size());

        // Remove all documents from the collection and check the result of the operation
        collectionManager.removeAllFromCollection(collection, listDocuments, session);
        session.save();
        chain = new OperationChain("test-chain-3");
        chain.add(GetDocumentsFromCollectionOperation.ID);

        ctx = new OperationContext(session);
        ctx.setInput(collection);
        documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);

        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(0, documentsList.size());
    }
}
