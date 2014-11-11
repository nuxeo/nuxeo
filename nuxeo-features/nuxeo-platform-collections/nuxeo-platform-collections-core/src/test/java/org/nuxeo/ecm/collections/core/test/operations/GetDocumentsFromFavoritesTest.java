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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.collections.core.automation.GetDocumentsFromFavoritesOperation;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.google.inject.Inject;

/**
 * Class testing the operation "Collection.GetDocumentFromFavorites".
 *
 * @since 6.0
 */
public class GetDocumentsFromFavoritesTest extends
        CollectionOperationsTestCase {

    @Inject
    FavoritesManager favoritesManager;

    private List<DocumentModel> listDocuments;

    @Before
    public void setUp() throws ClientException {
        testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create a list of test documents
        listDocuments = createTestFiles(session, 5);
        // Add them in the favorites
        for (DocumentModel doc : listDocuments) {
            favoritesManager.addToFavorites(doc, session);
        }
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

        ctx = new OperationContext(session);
        ctx.setInput(listDocuments.get(0));
        documentsList = (PaginableDocumentModelListImpl) service.run(ctx, chain);
        // Check the result of the operation
        assertNotNull(documentsList);
        assertEquals(0, documentsList.size());
    }
}
