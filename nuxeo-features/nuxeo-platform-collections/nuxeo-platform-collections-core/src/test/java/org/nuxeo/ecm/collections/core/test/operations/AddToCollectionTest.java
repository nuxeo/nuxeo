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
package org.nuxeo.ecm.collections.core.test.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.automation.AddToCollectionOperation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Class testing the operation "Collection.AddToCollection".
 *
 * @since 5.9.4
 */
public class AddToCollectionTest extends CollectionOperationsTestCase {

    private List<DocumentModel> listDocs;

    private DocumentModel collection;

    @Inject
    CollectionManager collectionManager;

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create test documents
        listDocs = createTestFiles(session, 5);
        // Create a collection
        collection = collectionManager.createCollection(session, COLLECTION_NAME, COLLECTION_DESCRIPTION,
                testWorkspace.getPathAsString());
    }

    @Test
    public void testOperationWithOneDocument() throws Exception {
        chain = new OperationChain("test-chain");
        chain.add(AddToCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(listDocs.get(0));

        DocumentModel resultDoc = (DocumentModel) service.run(ctx, chain);

        // Test the result of the operation
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        assertTrue(collectionAdapter.getCollectedDocumentIds().contains(listDocs.get(0).getId()));
        assertEquals(1, collectionAdapter.getCollectedDocumentIds().size());
        assertEquals(listDocs.get(0).getId(), resultDoc.getId());
    }

    @Test
    public void testOperationWithSeveralDocuments() throws Exception {
        chain = new OperationChain("test-chain");
        chain.add(AddToCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        DocumentModelList listDocModel = new DocumentModelListImpl(listDocs);
        ctx.setInput(listDocModel);

        DocumentModelList listDocResult = (DocumentModelList) service.run(ctx, chain);

        // Test the result of the operation
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        assertEquals(listDocs.size(), collectionAdapter.getCollectedDocumentIds().size());
        assertEquals(listDocs.size(), listDocResult.size());
        for (DocumentModel doc : listDocModel) {
            assertTrue(collectionAdapter.getCollectedDocumentIds().contains(doc.getId()));
            assertEquals(doc.getId(), listDocResult.get(listDocs.indexOf(doc)).getId());
        }
    }

    @Test(expected=OperationException.class)
    public void testOperationWithNonCollectableDocument() throws Exception {
        // Create a second collection for the test
        DocumentModel collection2 = collectionManager.createCollection(session, COLLECTION_NAME,
                COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());

        chain = new OperationChain("test-chain");
        chain.add(AddToCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(collection2);

        try {
            service.run(ctx, chain);
            // Should fail before
            fail("File is not a proper file");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }
}
