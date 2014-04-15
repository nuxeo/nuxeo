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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.collections.core.automation.CreateCollectionOperation;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * Class testing the operation "Collection.CreateCollection".
 *
 * @since 5.9.4
 */
public class CreateCollectionTest extends CollectionOperationsTestCase {

    private DocumentModel doc;

    @Before
    public void setup() throws ClientException {
        testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);

        doc = session.createDocumentModel(testWorkspace.getPath().toString(),
                "test", "File");
        session.createDocument(doc);
        session.save();
    }

    @Test
    public void testCreateCollectionWithoutPath() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", COLLECTION_NAME);
        params.put("description", COLLECTION_DESCRIPTION);
        params.put("path", null);

        chain = new OperationChain("test-chain");
        chain.add(CreateCollectionOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(session.getRootDocument());

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertNotNull(doc);
        Assert.assertEquals(COLLECTION_NAME, doc.getTitle());
        Assert.assertEquals(COLLECTION_DESCRIPTION,
                doc.getPropertyValue("dc:description"));
    }

    @Test
    public void testCreateCollectionWithPath() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", COLLECTION_NAME);
        params.put("description", COLLECTION_DESCRIPTION);
        params.put("path", testWorkspace.getPathAsString());

        chain = new OperationChain("test-chain");
        chain.add(CreateCollectionOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(session.getRootDocument());

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertNotNull(doc);
        Assert.assertEquals(COLLECTION_NAME, doc.getTitle());
        Assert.assertEquals(COLLECTION_DESCRIPTION, doc.getPropertyValue("dc:description"));

        String collectionPath = testWorkspace.getPathAsString() + "/" + COLLECTION_NAME;
        assertTrue(session.exists(new PathRef(collectionPath)));
    }
}
