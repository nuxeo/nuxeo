/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestEventConfService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.LogEntryProvider;

/**
 * Test the log entries persistence
 *
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestLogEntryProvider extends PersistenceTestCase {

    protected static final Log log = LogFactory.getLog(TestLogEntryProvider.class);

    private LogEntryProvider providerUnderTest;

    @Override
    protected void handleAfterSetup(EntityManager entityManager) {
        providerUnderTest = LogEntryProvider.createProvider(entityManager);
    }

    protected Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        ExtendedInfo info = ExtendedInfoImpl.createExtendedInfo(Long.valueOf(1));
        infos.put("id", info);
        return infos;
    }

    protected String eventId() {
        return TestLogEntryProvider.class.getSimpleName();
    }

    protected String[] eventIds() {
        return new String[] { eventId() };
    }

    protected LogEntry doCreateEntry(String docId, String repositoryId) {
        LogEntry createdEntry = new LogEntryImpl();
        createdEntry.setEventId(eventId());
        createdEntry.setDocUUID(docId);
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath("/" + docId);
        createdEntry.setRepositoryId(repositoryId);
        createdEntry.setExtendedInfos(createExtendedInfos());
        return createdEntry;
    }

    protected LogEntry doCreateEntryAndPersist(String docId, String repositoryId) {
        LogEntry entry = doCreateEntry(docId, repositoryId);
        entityManager.persist(entry);
        return entry;
    }

    protected LogEntry doCreateEntryAndPersist(String docId) {
        return doCreateEntryAndPersist(docId, "test");
    }

    protected List<LogEntry> doEncapsulate(LogEntry entry) {
        List<LogEntry> entries = new ArrayList<>(1);
        entries.add(entry);
        return entries;
    }

    @Test
    public void testAddLogEntry() {
        LogEntry entry = doCreateEntry("id", "test");
        providerUnderTest.addLogEntry(entry);
        boolean hasId = entry.getId() != 0;
        assertTrue(hasId);
    }

    @Test
    public void testHavingKey() {
        LogEntry entry = doCreateEntryAndPersist("id");
        providerUnderTest.addLogEntry(entry);
        List<LogEntry> entries = providerUnderTest.nativeQueryLogs(
                "log.id = " + entry.getId() + " and log.extendedInfos['id'] is not null", 1, 10);
        assertEquals(1, entries.size());
        assertEquals(Long.valueOf(1L), entries.get(0).getExtendedInfos().get("id").getValue(Long.class));
    }

    @Test
    public void testByUUID() {
        LogEntry entry = doCreateEntryAndPersist("id");
        providerUnderTest.addLogEntry(entry);
        List<LogEntry> fetchedEntries = providerUnderTest.getLogEntriesFor("id");
        assertNotNull(fetchedEntries);
        int entriesCount = fetchedEntries.size();
        assertEquals(1, entriesCount);
        LogEntry fetchedEntry = fetchedEntries.get(0);
        assertNotNull(fetchedEntry);
        assertEquals("id", fetchedEntry.getDocUUID());
    }

    @Test
    public void testByUuidAndRepository() {
        LogEntry entry1 = doCreateEntryAndPersist("id", "repository1");
        providerUnderTest.addLogEntry(entry1);
        LogEntry entry2 = doCreateEntryAndPersist("id", "repository2");
        providerUnderTest.addLogEntry(entry2);

        List<LogEntry> fetchedEntries = providerUnderTest.getLogEntriesFor("id");
        assertNotNull(fetchedEntries);
        int entriesCount = fetchedEntries.size();
        assertEquals(2, entriesCount);
        List<LogEntry> fetchedEntries1 = providerUnderTest.getLogEntriesFor("id", "repository1");
        assertNotNull(fetchedEntries1);
        int entriesCount1 = fetchedEntries1.size();
        assertEquals(1, entriesCount1);
        List<LogEntry> fetchedEntries2 = providerUnderTest.getLogEntriesFor("id", "repository2");
        assertNotNull(fetchedEntries2);
        int entriesCount2 = fetchedEntries2.size();
        assertEquals(1, entriesCount2);

        LogEntry fetchedEntry1 = fetchedEntries1.get(0);
        assertNotNull(fetchedEntry1);
        assertEquals("id", fetchedEntry1.getDocUUID());
        assertEquals("repository1", fetchedEntry1.getRepositoryId());
        LogEntry fetchedEntry2 = fetchedEntries2.get(0);
        assertNotNull(fetchedEntry2);
        assertEquals("id", fetchedEntry2.getDocUUID());
        assertEquals("repository2", fetchedEntry2.getRepositoryId());
    }

    @Test
    public void testByFilter() throws Exception {
        LogEntry one = doCreateEntryAndPersist("id");
        Thread.sleep(1000);
        doCreateEntryAndPersist("id");

        Map<String, FilterMapEntry> filters = new HashMap<>();
        FilterMapEntry filterOne = new FilterMapEntry();
        filterOne.setQueryParameterName("yop");
        filterOne.setColumnName("eventDate");
        filterOne.setOperator("=");
        filterOne.setObject(one.getEventDate());
        filters.put("oups", filterOne);
        List<LogEntry> entries = providerUnderTest.getLogEntriesFor("id", filters, true);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals(one.getId(), entries.get(0).getId());
    }

    @Test
    public void testById() {
        LogEntry one = doCreateEntryAndPersist("id");
        LogEntry fetchedEntry = providerUnderTest.getLogEntryByID(one.getId());
        assertNotNull(fetchedEntry);
        assertEquals(one.getId(), fetchedEntry.getId());
    }

    @Test
    public void testByNativeQuery() {
        doCreateEntryAndPersist("one");
        doCreateEntryAndPersist("two");
        List<LogEntry> entries = providerUnderTest.nativeQueryLogs(
                "log.extendedInfos['id'] is not null order by log.eventDate desc", 2, 1);
        assertNotNull(entries);
        int entryCount = entries.size();
        assertEquals(1, entryCount);
    }

    @Test
    public void testDateRange() throws AuditQueryException {
        Date now = new Date();
        Date nowMinusOneHour = DateRangeParser.parseDateRangeQuery(now, "-1h");
        LogEntry one = doCreateEntryAndPersist("one");
        one.setEventDate(nowMinusOneHour);
        LogEntry two = doCreateEntryAndPersist("two");
        two.setEventDate(now);
        entityManager.flush();
        List<LogEntry> entries = providerUnderTest.queryLogs(eventIds(), "-40m");
        assertNotNull(entries);
        int entryCount = entries.size();
        assertEquals(1, entryCount);
    }

    @Test
    public void testLogsByPage() throws Exception {
        LogEntry one = doCreateEntryAndPersist("one");
        Thread.sleep(1000);
        Date limit = new Date();
        doCreateEntryAndPersist("two");
        LogEntry three = doCreateEntryAndPersist("three");
        one.setCategory("nuch");
        three.setCategory("nuch");
        String[] categories = { "nuch" };
        List<LogEntry> entries = providerUnderTest.queryLogsByPage(eventIds(), limit, categories, "/", 1, 1);
        assertNotNull(entries);
        int entryCount = entries.size();
        assertEquals(1, entryCount);
        assertEquals("three", entries.get(0).getDocUUID());
    }

    @Test
    public void testRemove() {
        doCreateEntryAndPersist("one");
        doCreateEntryAndPersist("two");
        doCreateEntryAndPersist("three");
        int count = providerUnderTest.removeEntries(eventId(), "/");
        assertEquals(3, count);
    }

    @Test
    public void testCountEventsById() {
        LogEntry one = doCreateEntryAndPersist("one");
        String eventId = one.getEventId();
        Long count = providerUnderTest.countEventsById(eventId);
        assertEquals(Long.valueOf(1), count);
    }

    @Test
    public void testQuery() {
        LogEntry one = doCreateEntryAndPersist("one");
        doCreateEntryAndPersist("two");
        LogEntry three = doCreateEntryAndPersist("three");
        one.setCategory("nuch");
        three.setCategory("nuch");
        List<?> entries = providerUnderTest.nativeQuery(
                "select log.eventId, count(*) from LogEntry log where log.eventId = 'TestLogEntryProvider' group by log.eventId",
                1, 1);
        assertNotNull(entries);
        int entryCount = entries.size();
        assertEquals(1, entryCount);
        Object[] entry = (Object[]) entries.get(0);
        String name = (String) entry[0];
        Long count = (Long) entry[1];
        assertEquals("TestLogEntryProvider", name);
        assertEquals(Long.valueOf(3L), count);
    }

    @Test
    @Ignore
    public void testEventIds() {
        String eventId = eventId();
        doCreateEntryAndPersist("one");
        doCreateEntryAndPersist("two");
        List<String> eventIds = providerUnderTest.findEventIds();
        assertEquals(1, eventIds.size());
        assertEquals(eventId, eventIds.get(0));
    }

}
