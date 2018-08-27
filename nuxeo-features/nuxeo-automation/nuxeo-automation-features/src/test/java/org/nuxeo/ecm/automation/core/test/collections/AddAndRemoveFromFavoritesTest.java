/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ecm.automation.core.test.collections;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.collections.AddToFavoritesOperation;
import org.nuxeo.ecm.automation.core.operations.collections.RemoveFromFavoritesOperation;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Class testing the operation "Document.AddToFavorites" and "Document.RemoveFromFavorites".
 *
 * @since 8.1
 */
public class AddAndRemoveFromFavoritesTest extends CollectionOperationsTestCase {

    private List<DocumentModel> listDocs;

    @Inject
    FavoritesManager favoritesManager;

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create test documents
        listDocs = createTestFiles(session, 5);
    }

    @Test
    public void testAddThenRemoveFromFavrotiesOperation() throws Exception {
        chain = new OperationChain("test-chain");
        chain.add(AddToFavoritesOperation.ID);

        OperationContext ctx = new OperationContext(session);
        DocumentModelList listDocModel = new DocumentModelListImpl(listDocs);
        ctx.setInput(listDocModel);

        DocumentModelList listDocResult = (DocumentModelList) service.run(ctx, chain);

        // Test the result of the add operation
        DocumentModel favorites = favoritesManager.getFavorites(listDocResult.get(0), session);
        Collection favoritesAdapter = favorites.getAdapter(Collection.class);
        assertEquals(listDocs.size(), favoritesAdapter.getCollectedDocumentIds().size());
        assertEquals(listDocs.size(), listDocResult.size());
        for (DocumentModel doc : listDocModel) {
            assertTrue(favoritesAdapter.getCollectedDocumentIds().contains(doc.getId()));
            assertEquals(doc.getId(), listDocResult.get(listDocs.indexOf(doc)).getId());
        }

        // Remove documents from favorites
        chain = new OperationChain("test-chain");
        chain.add(RemoveFromFavoritesOperation.ID);

        ctx = new OperationContext(session);
        listDocModel = new DocumentModelListImpl(listDocResult);
        ctx.setInput(listDocModel);

        listDocResult = (DocumentModelList) service.run(ctx, chain);

        // Test the result of the remove operation
        favorites = favoritesManager.getFavorites(listDocResult.get(0), session);
        favoritesAdapter = favorites.getAdapter(Collection.class);
        assertEquals(0, favoritesAdapter.getCollectedDocumentIds().size());
        assertEquals(listDocs.size(), listDocResult.size());
    }

}
