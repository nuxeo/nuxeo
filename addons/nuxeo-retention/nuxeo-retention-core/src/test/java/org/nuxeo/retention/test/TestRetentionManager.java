/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this doc except in compliance with the License.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Calendar;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;

/**
 * @since 11.1
 */
public class TestRetentionManager extends RetentionTestCase {

    public static Log log = LogFactory.getLog(TestRetentionManager.class);

    @Inject
    protected AutomationService automationService;

    @Inject
    protected BulkService bulkService;

    @Test
    public void testRuleOnlyFile() throws InterruptedException {
        DocumentModel workspace = session.createDocumentModel("/", "workspace", "Workspace");
        workspace = session.createDocument(workspace);
        workspace = session.saveDocument(workspace);
        try {
            service.attachRule(workspace, createManualImmediateRuleMillis(100L), session);
            fail("Should not accept workspace document");
        } catch (NuxeoException e) {
            // expected
        }
    }

    @Test
    public void test1DayManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, 0L, 0L, 1L, 0L, null, null), 1_000);
    }

    @Test
    public void test1MonthManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, 0L, 1L, 0L, 0L, null, null), 1_000);
    }

    @Test
    public void test1YearManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, 1L, 0L, 0L, 0L, null, null), 1_000);
    }

    @Test
    public void testManualImmediateRuleWithActions() throws InterruptedException {
        RetentionRule testRule = createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, 100L,
                null, Arrays.asList("Document.Trash"));

        file = service.attachRule(file, testRule, session);
        assertTrue(session.isRecord(file.getRef()));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000L);

        file = session.getDocument(file.getRef());

        // it has no retention anymore and trashed
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(file.isTrashed());
    }

    @Test
    public void testManualImmediateRule() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100L);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertFalse(file.isLocked());

        awaitRetentionExpiration(1000L);

        file = session.getDocument(file.getRef());

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testManualDocumentMovedToFolderRule() throws InterruptedException {

        RetentionRule testRule = createManualEventBasedRuleMillis(DocumentEventTypes.DOCUMENT_MOVED,
                "document.getPathAsString().startsWith('/testFolder')", 1000L);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        Record record = file.getAdapter(Record.class);
        assertTrue(record.isRetainUntilInderterminate());

        awaitRetentionExpiration(500L);

        file = session.getDocument(file.getRef());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        record = file.getAdapter(Record.class);
        assertTrue(record.isRetainUntilInderterminate());

        DocumentModel folder = session.createDocumentModel("/", "testFolder", "Folder");
        folder = session.createDocument(folder);
        folder = session.saveDocument(folder);

        file = session.move(file.getRef(), folder.getRef(), null);

        awaitRetentionExpiration(500L);

        record = file.getAdapter(Record.class);
        assertFalse(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(500L);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    public void testManualMetadataBasedRule() throws InterruptedException {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 1000L);
        Calendar haldSecond = Calendar.getInstance();
        haldSecond.add(Calendar.MILLISECOND, 500);
        file.setPropertyValue("dc:expired", haldSecond);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000L);

        Record record = file.getAdapter(Record.class);
        assertFalse(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000L);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    public void testManualPastMetadataBasedRule() throws InterruptedException {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 500L);
        Calendar haldSecond = Calendar.getInstance();
        haldSecond.add(Calendar.MILLISECOND, -1000);
        file.setPropertyValue("dc:expired", haldSecond);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testRetainUntilDateSaved() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100L);
        file = service.attachRule(file, testRule, session);
        Calendar original = file.getRetainUntil();
        awaitRetentionExpiration(1000L);
        Record record = session.getDocument(file.getRef()).getAdapter(Record.class);
        Calendar saved = record.getSavedRetainUntil();
        assertNotNull(saved);
        assertEquals(original.getTimeInMillis(), saved.getTimeInMillis());
    }

}
