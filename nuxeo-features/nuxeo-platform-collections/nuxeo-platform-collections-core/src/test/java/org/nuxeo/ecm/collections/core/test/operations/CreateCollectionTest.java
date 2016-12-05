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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.collections.core.automation.CreateCollectionOperation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import junit.framework.Assert;

/**
 * Class testing the operation "Collection.CreateCollection".
 *
 * @since 5.9.4
 */
public class CreateCollectionTest extends CollectionOperationsTestCase {

    @Before
    public void setup() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        session.save();
    }

    @Test
    public void testCreateCollectionWithoutPath() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", COLLECTION_NAME);
        params.put("description", COLLECTION_DESCRIPTION);

        chain = new OperationChain("test-chain");
        chain.add(CreateCollectionOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertNotNull(doc);
        Assert.assertEquals(COLLECTION_NAME, doc.getTitle());
        Assert.assertEquals(COLLECTION_DESCRIPTION, doc.getPropertyValue("dc:description"));
    }

    @Test
    public void testCreateCollectionWithPath() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", COLLECTION_NAME);
        params.put("description", COLLECTION_DESCRIPTION);

        chain = new OperationChain("test-chain");
        chain.add(CreateCollectionOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(testWorkspace);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertNotNull(doc);
        Assert.assertEquals(COLLECTION_NAME, doc.getTitle());
        Assert.assertEquals(COLLECTION_DESCRIPTION, doc.getPropertyValue("dc:description"));

        String collectionPath = testWorkspace.getPathAsString() + "/" + COLLECTION_NAME;
        assertTrue(session.exists(new PathRef(collectionPath)));
    }

    @Test(expected=OperationException.class)
    public void testCreateCollectionOnWrongDocument() throws Exception {
        DocumentModel doc = session.createDocumentModel(testWorkspace.getPath().toString(), "test", "File");
        session.createDocument(doc);
        session.save();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", COLLECTION_NAME);
        params.put("description", COLLECTION_DESCRIPTION);

        chain = new OperationChain("test-chain");
        chain.add(CreateCollectionOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);

        try {
            service.run(ctx, chain);
            // Should fail before
            fail("Document is not a File");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }
}
