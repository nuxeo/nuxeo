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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.collections.MoveCollectionMemberOperation;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * Class testing the MoveCollectionMemberOpeartion operation.
 *
 * @since 8.4
 */
public class MoveCollectionMemberTest extends CollectionOperationsTestCase {

    private static final int NB_FILES = 5;

    private List<DocumentModel> listDocs;

    private DocumentModel collection;

    @Inject
    CollectionManager collectionManager;

    @Inject
    PageProviderService pps;

    protected List<DocumentModel> getCollectionMembersByQuery() {
        // Check order from query

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pps.getPageProvider(
                CollectionConstants.ORDERED_COLLECTION_CONTENT_PAGE_PROVIDER, null, null, null, props,
                new Object[] { collection.getId() });

        List<DocumentModel> members = pageProvider.getCurrentPage();
        return members;
    }

    protected void initialCheck(Collection collectionAdapter) {
        assertEquals(NB_FILES, collectionAdapter.size());

        assertEquals(listDocs.get(0).getId(), collectionAdapter.getCollectedDocumentIds().get(0));
        assertEquals(listDocs.get(NB_FILES - 1).getId(), collectionAdapter.getCollectedDocumentIds().get(NB_FILES - 1));
    }

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        // Create test documents
        listDocs = createTestFiles(session, NB_FILES);
        // Create a collection
        collection = collectionManager.createCollection(session, COLLECTION_NAME, COLLECTION_DESCRIPTION,
                testWorkspace.getPathAsString());
        collectionManager.addToCollection(collection, listDocs, session);
    }

    @Test
    public void testIllegalSwapMembers() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();

        chain = new OperationChain("test-chain");
        chain.add(MoveCollectionMemberOperation.ID);
        // missing param

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);

            try {
                service.run(ctx, chain);
            } catch (OperationException e) {
                // Expected, let's keep testing

                chain = new OperationChain("test-chain");
                chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(0)).set("member2",
                        listDocs.get(NB_FILES - 1));

                // Wrong input
                ctx.setInput(null);

                try {
                    service.run(ctx, chain);
                } catch (OperationException ex) {
                    // expected
                    return;
                }
                fail(MoveCollectionMemberOperation.ID + " should have failed because of missing input");
            }
        }

        fail(MoveCollectionMemberOperation.ID + " should have failed because of missing param");

    }

    @Test
    public void testMoveFirstAfterLastMember() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();

        initialCheck(collectionAdapter);

        chain = new OperationChain("test-chain");
        chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(0)).set("member2",
                listDocs.get(NB_FILES - 1));

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);

            boolean result = (boolean) service.run(ctx, chain);

            assertTrue(result);
        }

        collection = session.getDocument(collection.getRef());
        collectionAdapter = collection.getAdapter(Collection.class);

        assertEquals(NB_FILES, collectionAdapter.getCollectedDocumentIds().size());

        assertEquals(listDocs.get(0).getId(), collectionAdapter.getCollectedDocumentIds().get(NB_FILES - 1));
        assertEquals(listDocs.get(NB_FILES - 1).getId(), collectionAdapter.getCollectedDocumentIds().get(NB_FILES - 2));

        // Check by query
        List<DocumentModel> members = getCollectionMembersByQuery();

        assertEquals(NB_FILES, members.size());

        assertEquals(listDocs.get(0).getId(), members.get(NB_FILES - 1).getId());
        assertEquals(listDocs.get(NB_FILES - 1).getId(), members.get(NB_FILES - 2).getId());

    }

    @Test
    public void testMoveLastBeforeFirstMember() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();
        initialCheck(collectionAdapter);

        chain = new OperationChain("test-chain");
        chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(NB_FILES - 1));

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);

            boolean result = (boolean) service.run(ctx, chain);

            assertTrue(result);
        }

        collection = session.getDocument(collection.getRef());
        collectionAdapter = collection.getAdapter(Collection.class);

        assertEquals(NB_FILES, collectionAdapter.getCollectedDocumentIds().size());

        assertEquals(listDocs.get(0).getId(), collectionAdapter.getCollectedDocumentIds().get(1));
        assertEquals(listDocs.get(NB_FILES - 1).getId(), collectionAdapter.getCollectedDocumentIds().get(0));

        // Check by query
        List<DocumentModel> members = getCollectionMembersByQuery();

        assertEquals(NB_FILES, members.size());

        assertEquals(listDocs.get(0).getId(), members.get(1).getId());
        assertEquals(listDocs.get(NB_FILES - 1).getId(), members.get(0).getId());
    }

    @Test
    public void testMoveDowntInTheMiddleMember() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();

        initialCheck(collectionAdapter);

        chain = new OperationChain("test-chain");
        int index = NB_FILES / 2;
        chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(index)).set("member2",
                listDocs.get(index + 1));

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);

            boolean result = (boolean) service.run(ctx, chain);

            assertTrue(result);
        }

        collection = session.getDocument(collection.getRef());
        collectionAdapter = collection.getAdapter(Collection.class);

        assertEquals(NB_FILES, collectionAdapter.getCollectedDocumentIds().size());

        assertEquals(listDocs.get(index).getId(), collectionAdapter.getCollectedDocumentIds().get(index + 1));
        assertEquals(listDocs.get(index + 1).getId(), collectionAdapter.getCollectedDocumentIds().get(index));

        // Check by query
        List<DocumentModel> members = getCollectionMembersByQuery();

        assertEquals(NB_FILES, members.size());

        assertEquals(listDocs.get(index).getId(), members.get(index + 1).getId());
        assertEquals(listDocs.get(index + 1).getId(), members.get(index).getId());

    }

    /**
     * @since 8.4
     */
    @Test
    public void testMoveUpInTheMiddleMember() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();

        initialCheck(collectionAdapter);

        chain = new OperationChain("test-chain");
        int index = (NB_FILES / 2) + 1;
        chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(index)).set("member2",
                listDocs.get(index - 1));

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);

            boolean result = (boolean) service.run(ctx, chain);

            assertTrue(result);
        }

        collection = session.getDocument(collection.getRef());
        collectionAdapter = collection.getAdapter(Collection.class);

        assertEquals(NB_FILES, collectionAdapter.getCollectedDocumentIds().size());

        assertEquals(listDocs.get(index).getId(), collectionAdapter.getCollectedDocumentIds().get(index - 1));
        assertEquals(listDocs.get(index - 1).getId(), collectionAdapter.getCollectedDocumentIds().get(index));

        // Check by query
        List<DocumentModel> members = getCollectionMembersByQuery();

        assertEquals(NB_FILES, members.size());

        assertEquals(listDocs.get(index).getId(), members.get(index - 1).getId());
        assertEquals(listDocs.get(index - 1).getId(), members.get(index).getId());

    }

    @Test
    public void testWrongMoveMembers() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();
        initialCheck(collectionAdapter);

        DocumentModel notInCollection = session.createDocumentModel(testWorkspace.getPath().toString(), "toto", "File");
        notInCollection = session.createDocument(notInCollection);

        chain = new OperationChain("test-chain");
        chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(0)).set("member2", notInCollection);

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);

            boolean result = (boolean) service.run(ctx, chain);

            assertFalse(result);
        }

        collection = session.getDocument(collection.getRef());
        collectionAdapter = collection.getAdapter(Collection.class);

        // Check nothing changed
        for (int i = 0; i < NB_FILES; i++) {
            assertEquals(listDocs.get(i).getId(), collectionAdapter.getCollectedDocumentIds().get(i));
        }

    }

    // move member before itself should do nothing
    @Test
    public void testWrongMoveMembers2() throws OperationException {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.getCollectedDocumentIds();
        initialCheck(collectionAdapter);

        chain = new OperationChain("test-chain");
        int index = (NB_FILES / 2) + 1;
        chain.add(MoveCollectionMemberOperation.ID).set("member1", listDocs.get(index)).set("member2",
                listDocs.get(index)); // twice same index

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(collection);
            boolean result = (boolean) service.run(ctx, chain);
            assertFalse(result);
        }

        collection = session.getDocument(collection.getRef());
        collectionAdapter = collection.getAdapter(Collection.class);

        // Check nothing changed
        for (int i = 0; i < NB_FILES; i++) {
            assertEquals(listDocs.get(i).getId(), collectionAdapter.getCollectedDocumentIds().get(i));
        }
    }

}
