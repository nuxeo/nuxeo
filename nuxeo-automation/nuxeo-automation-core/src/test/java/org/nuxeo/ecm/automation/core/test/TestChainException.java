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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
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
public class TestChainException {

    protected DocumentModel src;

    protected DocumentModel doc;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    TracerFactory factory;

    @Before
    public void initRepo() throws Exception {
        // Document with Source as title
        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        // Document with Document as title
        doc = session.createDocumentModel("/", "doc", "Folder");
        doc.setPropertyValue("dc:title", "Document");
        doc = session.createDocument(doc);
        session.save();
        src = session.getDocument(src.getRef());
        doc = session.getDocument(doc.getRef());
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        session.removeChildren(session.getRootDocument().getRef());
    }

    @Test
    public void testChainExceptionContribution() throws Exception {
        ChainException chainException = service.getChainException("contributedchain");
        assertNotNull(chainException);
        assertEquals(3, chainException.getCatchChainExceptions().size());
        assertEquals("chainExceptionA",
                chainException.getCatchChainExceptions().get(0).getChainId());
    }

    @Test
    public void testAutomationFilterContribution() throws Exception {
        AutomationFilter automationFilter = service.getAutomationFilter("filterA");
        assertNotNull(automationFilter);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        assertEquals(
                Scripting.newTemplate("@{Document['dc:title']=='Source'}").eval(
                        ctx), automationFilter.getValue().eval(ctx));
    }

    @Test
    public void testAutomationChainException() throws Exception {
        // Activate trace mode to verify if exception chain has been run
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }

        // verify for a simple catch chain if it has been run
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        service.run(ctx, "anothercontributedchain");
        assertNotNull(factory.getTrace("chainExceptionA"));

        ctx = new OperationContext(session);
        ctx.setInput(src);
        assertTrue(service.run(ctx, "contributedchain") instanceof DocumentModel);
        // Verify that result is documentmodel from operation3 of
        // chainExceptionA
        // Verify if chainExceptionA has been run after contributedchain failure
        assertNotNull(factory.getTrace("chainExceptionA"));

        ctx = new OperationContext(session);
        ctx.setInput(doc);
        // Verify that result is documentref from operation2 of chainExceptionB
        assertTrue(service.run(ctx, "contributedchain") instanceof DocumentRef);
        // Verify if chainExceptionB has been run after contributedchain failure
        assertNotNull(factory.getTrace("chainExceptionB"));
    }
}
