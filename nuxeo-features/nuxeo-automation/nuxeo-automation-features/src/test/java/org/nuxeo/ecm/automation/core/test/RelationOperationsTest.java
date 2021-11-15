/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.services.CreateRelation;
import org.nuxeo.ecm.automation.core.operations.services.DeleteRelation;
import org.nuxeo.ecm.automation.core.operations.services.GetRelations;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.automation.core:test-relation-jena-contrib.xml")
public class RelationOperationsTest {

    protected static final String conformsTo = "http://purl.org/dc/terms/ConformsTo";

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    EventHandlerRegistry reg;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());
    }

    // ------ Tests comes here --------

    @Test
    public void testRelationOutboundOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("createRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", true);
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("outgoing", true);
        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);

        assertEquals(1, docs.size());
        assertEquals(dst, docs.get(0));

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelationWithGraphName");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("graphName", null).set("outgoing", true);
        DocumentModelList docs2 = (DocumentModelList) service.run(ctx, chain);

        assertEquals(docs, docs2);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("deleteRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(DeleteRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", true);
        DocumentModel doc2 = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc2, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo);
        DocumentModelList docs3 = (DocumentModelList) service.run(ctx, chain);

        assertTrue(docs3.isEmpty());
    }

    @Test
    public void testRelationIncomingOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("createRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", false);
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("outgoing", false);
        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);

        assertEquals(1, docs.size());
        assertEquals(dst, docs.get(0));

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelationWithGraphName");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("graphName", null).set("outgoing", false);
        DocumentModelList docs2 = (DocumentModelList) service.run(ctx, chain);

        assertEquals(docs, docs2);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("deleteRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(DeleteRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", false);
        DocumentModel doc2 = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc2, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo);
        DocumentModelList docs3 = (DocumentModelList) service.run(ctx, chain);

        assertTrue(docs3.isEmpty());
    }

    @Test
    public void testRelationMismatchOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("createRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", true);
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("outgoing", true);
        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);

        assertEquals(1, docs.size());
        assertEquals(dst, docs.get(0));

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelationWithGraphName");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("graphName", null).set("outgoing", true);
        DocumentModelList docs2 = (DocumentModelList) service.run(ctx, chain);

        assertEquals(docs, docs2);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("deleteRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(DeleteRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", false);
        DocumentModel doc2 = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc2, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("outgoing", true);
        DocumentModelList docs3 = (DocumentModelList) service.run(ctx, chain);

        assertEquals(1, docs3.size());
        assertEquals(dst, docs3.get(0));

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("deleteRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(DeleteRelation.ID).set("predicate", conformsTo).set("object", dst.getId()).set("outgoing", true);
        DocumentModel doc3 = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc3, src);

        ctx.clear();
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo).set("outgoing", true);
        DocumentModelList docs4 = (DocumentModelList) service.run(ctx, chain);

        assertTrue(docs4.isEmpty());
    }
}
