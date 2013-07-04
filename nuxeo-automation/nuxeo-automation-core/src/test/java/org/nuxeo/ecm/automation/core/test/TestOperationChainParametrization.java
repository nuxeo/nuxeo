/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     vpasquier
 */
package org.nuxeo.ecm.automation.core.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationTypeImpl;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import java.util.ArrayList;

import com.google.inject.Inject;

/**
 * Test to cover all new features related to new chain concept
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-parametrization-chain.xml")
public class TestOperationChainParametrization {

    protected DocumentModel src;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        session.removeChildren(session.getRootDocument().getRef());
    }

    /**
     * Check if using old chain concept is working
     */
    @Test
    public void testSimpleChain() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("mychain");
        chain.add(FetchContextDocument.ID);
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o2").set("message", "Hello 2!");
        chain.add("o1").set("message", "Hello 3!");
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        Assert.assertNotNull(doc);
    }

    /**
     * Check the new concept
     */
    @Test
    public void testParametizedChain() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        // Operation Listing including the chain
        List<OperationType> operationTypeList = new LinkedList<OperationType>();
        OperationType chain = new ChainTypeImpl("mychain");
        List<OperationType> operations = new ArrayList<OperationType>();
        operations.add(new OperationTypeImpl("o1"));
        operations.add(new OperationTypeImpl(FetchContextDocument.ID));
        chain.addOperations(operations);
        // Setting parameters of the chain/operation
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("message", "Hello i'm a chain!");
        // No need to put a message parameter, fallback is going to be done
        DocumentModel doc = (DocumentModel) service.run(ctx, chain,
                params);
        Assert.assertNotNull(doc);

    }
}
