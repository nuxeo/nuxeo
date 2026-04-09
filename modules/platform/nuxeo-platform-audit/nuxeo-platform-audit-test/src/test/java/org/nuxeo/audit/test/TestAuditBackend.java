/*
 * (C) Copyright 2018-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.audit.test;

import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_CATEGORY;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_PATH;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EXTENDED;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.service.AuditBackend.Capability.STARTS_WITH_PARTIAL_MATCH;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.eq;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.in;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.IgnoreIfAuditBackendDoesNotHaveExtendedInfoSearchCapability;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Audit tests that are common across all storage backends.
 * 
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditBackend {

    public static final String ID_FOR_AUDIT_STORAGE_TESTS = "ID_FOR_AUDIT_STORAGE_TESTS";

    public static final int NUM_OF_EVENTS = 56;

    @Inject
    protected AuditBackend backend;

    @Inject
    protected AuditFeature auditFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    @SuppressWarnings("removal")
    @ConditionalIgnore(condition = IgnoreIfNotAuditSequence.class)
    public void testInsertLogs() {
        // first test illegal argument cases
        // no id
        assertThrows(IllegalArgumentException.class, () -> backend.insertLogs(
                List.of(LogEntry.builder("eventIdForTests", new Date()).logDate(new Date()).build())));
        // no log date
        assertThrows(IllegalArgumentException.class,
                () -> backend.insertLogs(List.of(LogEntry.builder("eventIdForTests", new Date()).id(1_000L).build())));

        // second test normal insertion
        backend.insertLogs(
                List.of(LogEntry.builder("eventIdForTests", new Date()).id(1_000L).logDate(new Date()).build()));
        assertNotNull(backend.getLogEntryByID(1_000L));

        // third test same entry can not be inserted twice
        assertThrows(ConcurrentUpdateException.class, () -> backend.insertLogs(
                List.of(LogEntry.builder("eventIdForTests", new Date()).id(1_000L).logDate(new Date()).build())));
    }

    @Test
    @SuppressWarnings("removal")
    @ConditionalIgnore(condition = IgnoreIfNotAuditSequence.class)
    public void testInsertLogsWithSomeExistingLogEntries() {
        var queryBuilder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "eventIdForTests"));
        // insert two logs entries
        var originalEntries = List.of( //
                LogEntry.builder("eventIdForTests", new Date()).id(1_000L).logDate(new Date()).build(), //
                LogEntry.builder("eventIdForTests", new Date()).id(1_001L).logDate(new Date()).build() //
        );
        backend.insertLogs(originalEntries);
        transactionalFeature.nextTransaction();
        assertEquals(2, backend.queryLogs(queryBuilder).size());

        // create two more entries and try to insert all of them
        var newEntries = List.of( //
                LogEntry.builder("eventIdForTests", new Date()).id(1_002L).logDate(new Date()).build(), //
                LogEntry.builder("eventIdForTests", new Date()).id(1_003L).logDate(new Date()).build() //
        );
        var e = assertThrows(ConcurrentUpdateException.class,
                () -> backend.insertLogs(union(originalEntries, newEntries)));
        assertEquals("Concurrent update", e.getOriginalMessage());
        // assert duplicate entries number
        assertEquals(2, e.getInfos().size());
        // assert new entries have been inserted
        transactionalFeature.nextTransaction();
        assertEquals(4, backend.queryLogs(queryBuilder).size());
    }

    @Test
    public void shouldSupportMultiCriteriaQueries() {
        auditFeature.generateLogEntries("mydoc", "evt", "cat", 9);

        // simple Query
        List<LogEntry> res = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1", "evt2")));
        assertNotNull(res);
        assertEquals(2, res.size());

        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1")));
        assertEquals(1, res.size());

        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt")));
        assertEquals(0, res.size());

        // multi Query
        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1", "evt2"))
                                                       .and(Predicates.in(LOG_CATEGORY, "cat1")));
        assertEquals(1, res.size());

        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1", "evt2"))
                                                       .and(Predicates.in(LOG_CATEGORY, "cat1", "cat0")));
        assertEquals(2, res.size());

        // test page size
        res = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_PATH, "/mydoc")).offset(0).limit(5));
        assertEquals(5, res.size());

        res = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_PATH, "/mydoc")).offset(5).limit(5));
        assertEquals(4, res.size());

    }

    @Test
    public void testGetLatestLogId() {
        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id1 = backend.getLatestLogId("test", "documentModified0");
        assertTrue("id: " + id1, id1 > 0);

        auditFeature.generateLogEntries("mydoc", "documentCreated", "cat", 1);
        long id2 = backend.getLatestLogId("test", "documentModified0", "documentCreated0");
        assertTrue("id2: " + id2, id2 > 0);
        assertTrue(id2 > id1);

        long id = backend.getLatestLogId("test", "documentModified0");
        assertEquals(id1, id);
        id = backend.getLatestLogId("test", "unknown");
        assertEquals(0, id);
    }

    @Test
    @SuppressWarnings("removal")
    @Deprecated(since = "2025.0", forRemoval = true)
    public void testGetLogEntriesAfter() {
        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id1 = backend.getLatestLogId("test", "documentModified0");

        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id2 = backend.getLatestLogId("test", "documentModified0");
        assertTrue(id2 > id1);

        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id3 = backend.getLatestLogId("test", "documentModified0");
        assertTrue(id3 > id2);

        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id4 = backend.getLatestLogId("test", "documentModified0");
        assertTrue(id4 > id3);

        List<LogEntry> entries = backend.getLogEntriesAfter(id1, 5, "test", "documentCreated0", "documentModified0");
        assertEquals(4, entries.size());
        assertEquals(id1, entries.get(0).getId());

        entries = backend.getLogEntriesAfter(id2, 2, "test", "documentCreated0", "documentModified0");
        assertEquals(2, entries.size());
        assertEquals(id2, entries.get(0).getId());
        assertEquals(id3, entries.get(1).getId());
    }

    @Test
    public void testStartsWith() {
        auditFeature.generateLogEntries(NUM_OF_EVENTS,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date())
                             .docPath(i % 2 == 0 ? "/is/even" : "/is/odd")
                             .build());
        QueryBuilder builder = new AuditQueryBuilder().predicate(
                Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS));
        List<LogEntry> logs = backend.queryLogs(builder);
        assertEquals("Incorrect log entries: " + logs, NUM_OF_EVENTS, logs.size());

        assertStartsWithCount(NUM_OF_EVENTS, "/");
        assertStartsWithCount(NUM_OF_EVENTS, "/is");
        assertStartsWithCount(NUM_OF_EVENTS, "/is/");
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/even");
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/odd");

        // A partial match is supported by the sql and mongodb implementations, but not ES
        if (backend.hasCapability(STARTS_WITH_PARTIAL_MATCH)) {
            assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/eve");
            assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/od");
        } else {
            assertStartsWithCount(0, "/is/eve");
            assertStartsWithCount(0, "/is/od");
        }
    }

    /**
     * Asserts the number of events that match the startsWith parameter
     */
    public void assertStartsWithCount(int eventsCount, String startsWith) {
        List<LogEntry> logs = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                       .and(Predicates.startsWith(LOG_DOC_PATH, startsWith)));
        assertEquals(eventsCount, logs.size());
    }

    @Test
    public void testIn() {
        auditFeature.generateLogEntries(NUM_OF_EVENTS,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date())
                             .docPath(i % 2 == 0 ? "/is/even" : "/is/odd")
                             .build());
        var query = new AuditQueryBuilder().predicate(
                Predicates.in(LOG_EVENT_ID, List.of(ID_FOR_AUDIT_STORAGE_TESTS, "no-such-event-id")));
        List<LogEntry> list = backend.queryLogs(query);
        assertEquals(NUM_OF_EVENTS, list.size());
    }

    @Test
    public void testQueryWithDate() {
        // check that a query with a date doesn't raise any error
        QueryBuilder dateBuilder = new AuditQueryBuilder();
        dateBuilder.predicate(Predicates.gte(LOG_EVENT_DATE, Calendar.getInstance()));
        List<LogEntry> logs = backend.queryLogs(dateBuilder);
        assertTrue(logs.isEmpty());

        // test LT predicate too
        dateBuilder = new AuditQueryBuilder();
        dateBuilder.predicate(Predicates.lt(LOG_EVENT_DATE, Calendar.getInstance()));
        backend.queryLogs(dateBuilder);
    }

    @Test
    public void testReferenceUsedTwice() {
        // eventId = "something AND eventId != 'other'
        // won't match if parameters are incorrectly mapped to the same name
        auditFeature.generateLogEntries(NUM_OF_EVENTS,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date())
                             .docPath(i % 2 == 0 ? "/is/even" : "/is/odd")
                             .build());
        QueryBuilder query = new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                                    .and(Predicates.noteq(LOG_EVENT_ID, "no-such-event-id"));
        List<LogEntry> logs = backend.queryLogs(query);
        assertEquals(NUM_OF_EVENTS, logs.size());
    }

    @Test
    @ConditionalIgnore(condition = IgnoreIfAuditBackendDoesNotHaveExtendedInfoSearchCapability.class)
    public void testQueryExtendedInfo() {
        auditFeature.generateLogEntries(1, i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date()).build());
        auditFeature.generateLogEntries(5,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date()).extended("number", i).build());

        String extendedNumberField = LOG_EXTENDED + "/number";

        // retrieve log entries without extended info
        List<LogEntry> logs = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                       .and(Predicates.isnull(extendedNumberField)));
        assertEquals(1, logs.size());

        // retrieve log entries with extended infos
        logs = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                       .and(Predicates.isnotnull(extendedNumberField)));
        assertEquals(5, logs.size());

        // retrieve log entries with various conditions
        logs = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                       .and(Predicates.isnotnull(extendedNumberField))
                                       .and(Predicates.eq(extendedNumberField, 0)));
        assertEquals(1, logs.size());

        logs = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                       .and(Predicates.isnotnull(extendedNumberField))
                                       .and(Predicates.gt(extendedNumberField, 2)));
        assertEquals(2, logs.size());
    }

    // NXP-30511
    @Test
    public void testSupportNullExtendedInfos() {
        auditFeature.generateLogEntries(1,
                i -> LogEntry.builder("documentModified", new Date())
                             .category("cat")
                             .docUUID("testSupportNullExtendedInfos")
                             .repositoryId("test")
                             .extended("idx", i)
                             .extended("nullValue", null)
                             .build());

        var logEntries = backend.queryLogs(
                new AuditQueryBuilder().predicate(eq(LOG_DOC_UUID, "testSupportNullExtendedInfos"))
                                       .and(eq(LOG_EVENT_ID, "documentModified")));
        assertEquals(1, logEntries.size());
        var queriedLogEntry = logEntries.getFirst();
        var extended = queriedLogEntry.getExtended();
        assertTrue("ExtendedInfo should exist", extended.containsKey("nullValue"));
        assertNull("ExtendedInfo value should be null", extended.get("nullValue"));
    }

    @Test
    public void testScrollIds() {
        auditFeature.generateLogEntries(10,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date()).comment("log n°" + i).build());

        ScrollResult<String> scrollResult;
        List<LogEntry> entries;

        // retrieve all logs - 2 batches
        scrollResult = backend.scrollLogIds(new AuditQueryBuilder(), 5, Duration.ofMinutes(1));
        assertTrue(scrollResult.hasResults());
        assertFalse(isBlank(scrollResult.getScrollId()));
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(in(LOG_ID, scrollResult.getResults(Long::valueOf))));
        assertEquals(List.of("log n°0", "log n°1", "log n°2", "log n°3", "log n°4"),
                entries.stream().map(LogEntry::getComment).toList());

        scrollResult = backend.scrollLogIds(scrollResult.getScrollId());
        assertTrue(scrollResult.hasResults());
        assertFalse(isBlank(scrollResult.getScrollId()));
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(in(LOG_ID, scrollResult.getResults(Long::valueOf))));
        assertEquals(List.of("log n°5", "log n°6", "log n°7", "log n°8", "log n°9"),
                entries.stream().map(LogEntry::getComment).toList());

        // backup scrollId because last call could lead to a null or 'empty' scrollId
        var fScrollId = scrollResult.getScrollId();

        scrollResult = backend.scrollLogIds(scrollResult.getScrollId());
        assertFalse(scrollResult.hasResults());
        assertFalse(isBlank(scrollResult.getScrollId()));

        assertThrows(NuxeoException.class, () -> backend.scrollLogIds(fScrollId));
    }
}
