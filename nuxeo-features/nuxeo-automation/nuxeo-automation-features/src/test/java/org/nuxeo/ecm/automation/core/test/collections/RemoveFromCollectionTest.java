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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.collections.RemoveFromCollectionOperation;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Class testing the operation "Collection.RemoveFromCollection".
 *
 * @since 5.9.4
 */
public class RemoveFromCollectionTest extends CollectionOperationsTestCase {

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
        // Add documents to the collection
        collectionManager.addToCollection(collection, listDocs, session);
    }

    @Test
    public void testRemovalWithOneDocument() throws Exception {

        Collection collectionAdapter = collection.getAdapter(Collection.class);
        assertEquals(listDocs.size(), collectionAdapter.getCollectedDocumentIds().size());

        chain = new OperationChain("test-chain");
        chain.add(RemoveFromCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(listDocs.get(0));

        DocumentModel resultDoc = (DocumentModel) service.run(ctx, chain);

        // Test the result of the operation
        assertFalse(collectionAdapter.getCollectedDocumentIds().contains(listDocs.get(0).getId()));
        assertEquals(listDocs.size() - 1, collectionAdapter.getCollectedDocumentIds().size());
        assertEquals(listDocs.get(0).getId(), resultDoc.getId());
    }

    @Test
    public void testRemovalWithSeveralDocuments() throws Exception {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        assertEquals(listDocs.size(), collectionAdapter.getCollectedDocumentIds().size());

        chain = new OperationChain("test-chain");
        chain.add(RemoveFromCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        DocumentModelList listDocModel = new DocumentModelListImpl(listDocs);
        ctx.setInput(listDocModel);

        DocumentModelList listDocResult = (DocumentModelList) service.run(ctx, chain);

        // Test the result of the operation
        assertEquals(0, collectionAdapter.getCollectedDocumentIds().size());
        assertEquals(listDocs.size(), listDocResult.size());
        for (DocumentModel doc : listDocModel) {
            assertFalse(collectionAdapter.getCollectedDocumentIds().contains(doc.getId()));
        }
    }

    @Test
    public void testRemoveDocumentNotInCollection() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel doc = session.createDocumentModel(testWorkspace.getPath().toString(), "test", "File");
        session.createDocument(doc);
        session.save();

        chain = new OperationChain("test-chain");
        chain.add(RemoveFromCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);

        try {
            service.run(ctx, chain);
        } catch (Exception e) {
            // Behavior expected
            return;
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        fail("Document not in collection");
    }
}
