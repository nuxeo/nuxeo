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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TraceException;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.automation.AddToCollectionOperation;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

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
    public void setUp() throws ClientException {
        testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create test documents
        listDocs = createTestFiles(session, 5);
        // Create a collection
        collection = collectionManager.createCollection(session,
                COLLECTION_NAME, COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());
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

    @Test
    public void testOperationWithNonCollectableDocument() throws Exception {
        // Create a second collection for the test
        DocumentModel collection2 = collectionManager.createCollection(session,
                COLLECTION_NAME, COLLECTION_DESCRIPTION, testWorkspace.getPathAsString());

        chain = new OperationChain("test-chain");
        chain.add(AddToCollectionOperation.ID).set("collection", collection);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(collection2);

        try {
            service.run(ctx, chain);
            // Should fail before
            fail("File is not a proper file");
        } catch(TraceException e) {
            return;
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }
}
