/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class PublishDocumentTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    protected DocumentModel section;

    protected DocumentModel fileToPublish;

    @Before
    public void initRepo() throws Exception {
        section = session.createDocumentModel("/", "Section", "Section");
        section.setPropertyValue("dc:title", "Section");
        section = session.createDocument(section);

        fileToPublish = session.createDocumentModel("/", "FileToPublish", "File");
        fileToPublish.setPropertyValue("dc:title", "File");
        fileToPublish = session.createDocument(fileToPublish);
        session.save();
    }

    @Test
    public void testPublishDocument() throws OperationException {
        try (OperationContext ctx = new OperationContext(session);
                CapturingEventListener listener = new CapturingEventListener(DocumentEventTypes.DOCUMENT_PUBLISHED)) {
            ctx.setInput(fileToPublish);
            DocumentModel publishedDoc = (DocumentModel) service.run(ctx, PublishDocument.ID,
                    Collections.singletonMap("target", section));

            assertEquals(section.getId(), session.getDocument(publishedDoc.getParentRef()).getId());
            assertEquals(1, session.getChildren(section.getRef()).size());
            List<Event> events = listener.getCapturedEvents();
            assertEquals(2, events.size());
            assertEquals(fileToPublish, ((DocumentEventContext) events.get(0).getContext()).getSourceDocument());
            assertEquals(publishedDoc, ((DocumentEventContext) events.get(1).getContext()).getSourceDocument());
        }
    }

}
