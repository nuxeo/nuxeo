/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.3 Test for exception chain.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-exception-chain.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = TestChainException.Populate.class)
public class TestChainException {

    protected DocumentModel src;

    protected DocumentModel doc;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    TracerFactory factory;

    public static class Populate implements RepositoryInit {

        @Override
        public void populate(CoreSession session) {
            DocumentModel src = session.createDocumentModel("/", "src", "Folder");
            src.setPropertyValue("dc:title", "Source");
            src = session.createDocument(src);
            // Document with Document as title
            DocumentModel doc = session.createDocumentModel("/", "doc", "Folder");
            doc.setPropertyValue("dc:title", "Document");
            doc = session.createDocument(doc);
        }
    }

    @Before
    public void fetchDocuments() throws Exception {
        src = session.getDocument(new PathRef("/src"));
        doc = session.getDocument(new PathRef("/doc"));
    }

    @Test
    public void testChainExceptionContribution() throws Exception {
        ChainException chainException = service.getChainException("contributedchain");
        assertNotNull(chainException);
        assertEquals(3, chainException.getCatchChainExceptions().size());
        assertEquals("chainExceptionA", chainException.getCatchChainExceptions().get(0).getChainId());
    }

    @Test
    public void testAutomationFilterContribution() throws Exception {
        AutomationFilter automationFilter = service.getAutomationFilter("filterA");
        assertNotNull(automationFilter);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        assertEquals(Scripting.newTemplate("@{Document['dc:title']=='Source'}").eval(ctx),
                automationFilter.getValue().eval(ctx));
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
