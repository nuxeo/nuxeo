/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.services.CreateRelation;
import org.nuxeo.ecm.automation.core.operations.services.GetRelations;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features", "org.nuxeo.ecm.relations.api",
        "org.nuxeo.ecm.relations", "org.nuxeo.ecm.relations.jena" })
@LocalDeploy("org.nuxeo.ecm.automation.core:test-relation-jena-contrib.xml")
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
    public void testCreateAndReadRelation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("createRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateRelation.ID).set("predicate", conformsTo).set("object",
                dst.getId());
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals(doc, src);

        ctx = new OperationContext(session);
        ctx.setInput(src);
        chain = new OperationChain("getRelation");
        chain.add(FetchContextDocument.ID);
        chain.add(GetRelations.ID).set("predicate", conformsTo);
        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);

        assertEquals(1, docs.size());
        assertEquals(dst, docs.get(0));
    }

}
