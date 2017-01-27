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
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.scripting.MvelTemplate;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class DocumentUpdateOperationTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    OperationChain chain;

    @Before
    public void initChain() throws Exception {
        chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("type", "File");
        params.put("name", "file");
        chain.add(CreateDocument.ID).from(params);

        params = new HashMap<String, Object>();
        params.put(
                "properties",
                new MvelTemplate(
                        "dc:title=Test\ndc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}"));
        params.put("save", "true");
        chain.add(UpdateDocument.ID).from(params);
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void shouldUpdateProperties() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(session.getRootDocument());

            DocumentModel doc = (DocumentModel) service.run(ctx, chain);
            assertNotNull(doc);
            assertEquals("Test", doc.getTitle());
            assertNotNull(doc.getPropertyValue("dc:issued"));
        }
    }
}
