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
 *     Thierry Martins
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentParent;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PopDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PushDocument;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

@RunWith(FeaturesRunner.class)
@Features({ DocumentUpdatePropertiesWithMultilineTest.InitFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
public class DocumentUpdatePropertiesWithMultilineTest {

    public static class InitFeature implements RunnerFeature {

        @Override
        public void initialize(FeaturesRunner runner) {
            Framework.addListener(new RuntimeServiceListener() {

                @Override
                public void handleEvent(RuntimeServiceEvent event) {
                    if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
                        return;
                    }
                    Framework.removeListener(this);
                    event.runtime.getProperties().setProperty(Properties.PROPERTIES_MULTILINE_ESCAPE, "true");
                }
            });
        }
    }

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

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

    /**
     * Test if a multiline description is correctly updated
     */
    @Test
    @Ignore
    public void testUpdateWithMultilineDescription() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("properties", new Properties("dc:title=MyDoc")).set(
                "name", "note");
        chain.add(PushDocument.ID);
        chain.add(GetDocumentParent.ID);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "parentdoc");
        chain.add(SaveDocument.ID);
        chain.add(PopDocument.ID);
        chain.add(UpdateDocument.ID).set("properties",
                new Properties("dc:title=MyDoc2\ndc:description=" + "mydesc\notherdesc".replace("\n", "\\\n")));
        chain.add(LockDocument.ID);
        chain.add(SaveDocument.ID);

        assertNull(src.getPropertyValue("dc:description"));
        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        assertEquals("mydesc\notherdesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc2", out.getPropertyValue("dc:title"));
        assertTrue(out.isLocked());
        assertEquals("parentdoc", session.getDocument(src.getRef()).getPropertyValue("dc:description"));
    }
}
