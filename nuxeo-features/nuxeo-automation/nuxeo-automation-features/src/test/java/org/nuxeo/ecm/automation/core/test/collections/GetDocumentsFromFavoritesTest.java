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
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.collections.FetchFavorites;
import org.nuxeo.ecm.automation.core.operations.collections.GetDocumentsFromFavoritesOperation;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Class testing the operation "Collection.GetDocumentFromFavorites".
 *
 * @since 6.0
 */
public class GetDocumentsFromFavoritesTest extends CollectionOperationsTestCase {

    @Inject
    FavoritesManager favoritesManager;

    private List<DocumentModel> listDocuments;

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create a list of test documents
        listDocuments = createTestFiles(session, 5);
        // Add them in the favorites
        for (DocumentModel doc : listDocuments) {
            favoritesManager.addToFavorites(doc, session);
        }
        session.save();
    }

    @Test
    public void testGetDocumentsFromCollection() throws Exception {
        chain = new OperationChain("test-chain");
        chain.add(GetDocumentsFromFavoritesOperation.ID);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(listDocuments.get(0));
        PaginableDocumentModelListImpl documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);

        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(listDocuments.size(), documentsList.size());

        // Remove a document from the favorites and check the result of the operation
        favoritesManager.removeFromFavorites(listDocuments.get(0), session);
        listDocuments.remove(0);
        chain = new OperationChain("test-chain-2");
        chain.add(GetDocumentsFromFavoritesOperation.ID);
        session.save();

        ctx = new OperationContext(session);
        ctx.setInput(listDocuments.get(0));
        documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);
        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(listDocuments.size(), documentsList.size());

        // Remove all documents from the favorites and check the result of the operation
        for (DocumentModel doc : listDocuments) {
            favoritesManager.removeFromFavorites(doc, session);
        }
        chain = new OperationChain("test-chain-3");
        chain.add(GetDocumentsFromFavoritesOperation.ID);
        session.save();

        ctx = new OperationContext(session);
        ctx.setInput(listDocuments.get(0));
        documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);
        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(0, documentsList.size());
    }

    @Test
    public void canFetchFavorites() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModel favoritesRoot = (DocumentModel) service.run(ctx, FetchFavorites.ID);
        assertNotNull(favoritesRoot);
        assertTrue(favoritesRoot.hasFacet("Collection"));
    }
}
