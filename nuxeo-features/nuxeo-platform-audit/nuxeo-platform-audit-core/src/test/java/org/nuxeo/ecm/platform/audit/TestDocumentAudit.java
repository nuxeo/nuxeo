/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2021.34
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestDocumentAudit {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected DocumentHistoryReader history;

    protected DocumentModel doc;

    @Before
    public void setup() {
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "TestDoc");
        doc = session.createDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();
        List<LogEntry> entries = history.getDocumentHistory(doc, 0, 20);
        assertEquals(1, entries.size());
        assertEquals("documentCreated", entries.get(0).getEventId());
    }

    @Test
    public void testDoNotAuditDocumentUpdatedIfSetSameValue() {
        doc.setPropertyValue("dc:title", doc.getPropertyValue("dc:title"));
        session.saveDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();

        List<LogEntry> entries = history.getDocumentHistory(doc, 0, 20);
        assertEquals(1, entries.size());
        assertEquals(DOCUMENT_CREATED, entries.get(0).getEventId());
    }

    @Test
    public void testDoAuditDocumentUpdatedIfSetUnsetValue() {
        String oldTitle = (String) doc.getPropertyValue("dc:title");
        doc.setPropertyValue("dc:title", "newValue");
        session.saveDocument(doc);
        doc.setPropertyValue("dc:title", oldTitle);
        session.saveDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();

        List<LogEntry> entries = history.getDocumentHistory(doc, 0, 20);
        assertEquals(3, entries.size());
        assertTrue(entries.stream().anyMatch(e -> DOCUMENT_CREATED.equals(e.getEventId())));
        assertEquals(2, entries.stream().filter(e -> DOCUMENT_UPDATED.equals(e.getEventId())).count());
    }

    @Test
    public void testDoNotAuditDocumentUpdatedIfNotDirty() {
        session.saveDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();

        List<LogEntry> entries = history.getDocumentHistory(doc, 0, 20);
        assertEquals(1, entries.size());
        assertEquals(DOCUMENT_CREATED, entries.get(0).getEventId());
    }

    @Test
    public void testForceDoAuditDocumentUpdatedIfNotDirty() {
        doc.putContextData(CoreSession.DISABLE_AUDIT_LOGGER, false);
        session.saveDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();

        List<LogEntry> entries = history.getDocumentHistory(doc, 0, 20);
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> DOCUMENT_UPDATED.equals(e.getEventId())));
        assertTrue(entries.stream().anyMatch(e -> DOCUMENT_CREATED.equals(e.getEventId())));
    }

    @Test
    public void testForceDoNotAuditDocumentUpdatedIfDirty() {
        doc.putContextData(CoreSession.DISABLE_AUDIT_LOGGER, true);
        doc.setPropertyValue("dc:title", "foo");
        session.saveDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();

        List<LogEntry> entries = history.getDocumentHistory(doc, 0, 20);
        assertEquals(1, entries.size());
        assertTrue(entries.stream().anyMatch(e -> DOCUMENT_CREATED.equals(e.getEventId())));
    }

}
