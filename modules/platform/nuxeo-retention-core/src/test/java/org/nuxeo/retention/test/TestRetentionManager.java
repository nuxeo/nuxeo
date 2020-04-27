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

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;

/**
 * @since 11.1
 */
public class TestRetentionManager extends RetentionTestCase {

    @Test
    public void testRuleOnlyFile() {
        DocumentModel workspace = session.createDocumentModel("/", "workspace", "Workspace");
        workspace = session.createDocument(workspace);
        workspace = session.saveDocument(workspace);
        try {
            service.attachRule(workspace, createManualImmediateRuleMillis(100), session);
            fail("Should not accept workspace document");
        } catch (NuxeoException e) {
            assertEquals("Rule does not accept this document type", e.getMessage());
        }
    }

    @Test
    public void test1DayManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, 0, 0, 1, 0, null, null), 1000);
    }

    @Test
    public void test1MonthManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, 0, 1, 0, 0, null, null), 1000);
    }

    @Test
    public void test1YearManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, 1, 0, 0, 0, null, null), 1000);
    }

    @Test
    public void testManualImmediateRuleWithActions() throws InterruptedException {
        RetentionRule testRule = createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, 100,
                null, Arrays.asList("Document.Trash"));

        file = service.attachRule(file, testRule, session);
        assertTrue(session.isRecord(file.getRef()));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);

        file = session.getDocument(file.getRef());

        // it has no retention anymore and trashed
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(file.isTrashed());
    }

    @Ignore(value = "NXP-29002")
    @Test
    public void testManualImmediateRule() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertFalse(file.isLocked());

        awaitRetentionExpiration(1000);

        file = session.getDocument(file.getRef());

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testManualDocumentMovedToFolderRule() throws InterruptedException {

        RetentionRule testRule = createManualEventBasedRuleMillis(DocumentEventTypes.DOCUMENT_MOVED,
                "document.getPathAsString().startsWith('/testFolder')", 1000);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        Record record = file.getAdapter(Record.class);
        assertTrue(record.isRetentionIndeterminate());

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        record = file.getAdapter(Record.class);
        assertTrue(record.isRetentionIndeterminate());

        DocumentModel folder = session.createDocumentModel("/", "testFolder", "Folder");
        folder = session.createDocument(folder);
        folder = session.saveDocument(folder);

        file = session.move(file.getRef(), folder.getRef(), null);

        awaitRetentionExpiration(500);

        record = file.getAdapter(Record.class);
        assertFalse(record.isRetentionIndeterminate());
        assertFalse(record.isRetentionExpired());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    public void testManualMetadataBasedRule() throws InterruptedException {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 1000);
        Calendar halfSecond = Calendar.getInstance();
        halfSecond.add(Calendar.MILLISECOND, 500);
        file.setPropertyValue("dc:expired", halfSecond);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);

        Record record = file.getAdapter(Record.class);
        assertFalse(record.isRetentionIndeterminate());
        assertFalse(record.isRetentionExpired());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    public void testManualPastMetadataBasedRule() {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 500);
        Calendar minusOneSecond = Calendar.getInstance();
        minusOneSecond.add(Calendar.MILLISECOND, -1000);
        file.setPropertyValue("dc:expired", minusOneSecond);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testManualNullMetadataBasedRule() {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 500);
        Calendar minusOneSecond = Calendar.getInstance();
        minusOneSecond.add(Calendar.MILLISECOND, -1000);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testRetainUntilDateSaved() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100);
        file = service.attachRule(file, testRule, session);
        Calendar original = file.getRetainUntil();
        awaitRetentionExpiration(1000);
        Record record = session.getDocument(file.getRef()).getAdapter(Record.class);
        Calendar saved = record.getSavedRetainUntil();
        assertNotNull(saved);
        assertEquals(original.getTimeInMillis(), saved.getTimeInMillis());
    }

}
