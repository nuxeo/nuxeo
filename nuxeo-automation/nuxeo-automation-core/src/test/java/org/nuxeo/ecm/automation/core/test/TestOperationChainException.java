/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl;
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

import com.google.inject.Inject;

/**
 * @since 5.7.3 Test for exception chain.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-exception-chain.xml")
public class TestOperationChainException {

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

    @Test
    public void testChainExceptionContribution() throws Exception {
        OperationChain chain = service.getOperationChain("contributedchain");
        assertNotNull(chain.getExceptionChain());
        assertEquals(3, chain.getExceptionChain().getCatchChains().size());
        assertEquals("exceptionA",
                chain.getExceptionChain().getCatchChains().get(0).getChainId());
    }

    // TODO
    public void testChainException() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("notRegisteredChain");
        chain.add(FetchContextDocument.ID);
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o2").set("message", "Hello 2!");
        chain.add("oChainCtx").set("message",
                "expr:@{ChainParameters['messageChain']}");
        // Setting parameters of the chain
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("messageChain", "Hello i'm a chain!");
        chain.addChainParameters(params);
        // Checking if chain parameter is taken into account
        DocumentModel doc = (DocumentModel) ((OperationServiceImpl) service).run(
                ctx, chain);
        Assert.assertNotNull(doc);
    }
}
