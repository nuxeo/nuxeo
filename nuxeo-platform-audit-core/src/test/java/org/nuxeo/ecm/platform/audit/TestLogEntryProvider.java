/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.service.LogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.PersistenceProvider;

/**
 * Test the log entries persistence
 * 
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestLogEntryProvider extends TestCase {

    protected PersistenceProvider persistenceProvider;

    protected EntityManager entityManager;

    private LogEntryProvider providerUnderTest;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        persistenceProvider = new PersistenceProvider(new TestHibernateConfiguration());

        persistenceProvider.openPersistenceUnit();

        entityManager = persistenceProvider.acquireEntityManagerWithActiveTransaction();

        providerUnderTest = LogEntryProvider.createProvider(entityManager);
    }

    public void tearDown() {
        persistenceProvider.releaseEntityManagerWithRollback(entityManager);
    }

    protected Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<String, ExtendedInfo>();
        ExtendedInfo info = ExtendedInfo.createExtendedInfo(new Long(1));
        infos.put("id", info);
        return infos;
    }

    protected String eventId() {
        return TestLogEntryProvider.class.getSimpleName();
    }

    protected String[] eventIds() {
        return new String[] { eventId() };
    }

    protected LogEntry doCreateEntry(String docId) {
        LogEntry createdEntry = new LogEntry();
        createdEntry.setEventId(eventId());
        createdEntry.setDocUUID(docId);
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath("/" + docId);
        createdEntry.setExtendedInfos(createExtendedInfos());
        return createdEntry;
    }

    protected LogEntry doCreateEntryAndPersist(String docId) {
        LogEntry entry = doCreateEntry(docId);
        entityManager.persist(entry);
        return entry;
    }

    protected List<LogEntry> doEncapsulate(LogEntry entry) {
        List<LogEntry> entries = new ArrayList<LogEntry>(1);
        entries.add(entry);
        return entries;
    }

    public void testAddLogEntry() throws AuditException {
        LogEntry entry = doCreateEntry("id");
        providerUnderTest.addLogEntry(entry);
        boolean hasId = entry.getId() != 0;
        assertTrue(hasId);
    }

    public void testHavingKey() throws AuditException {
        LogEntry entry = doCreateEntryAndPersist("id");
        providerUnderTest.addLogEntry(entry);
        List<LogEntry> entries = providerUnderTest.nativeQueryLogs("log.id = "
                + entry.getId() + " and log.extendedInfos['id'] is not null",
                1, 10);
        assertEquals(1, entries.size());
        assertEquals(
                new Long(1L),
                entries.get(0).getExtendedInfos().get("id").getValue(Long.class));
    }

    public void testByUUID() {
        LogEntry entry = doCreateEntryAndPersist("id");
        providerUnderTest.addLogEntry(entry);
        List<LogEntry> fetchedEntries = providerUnderTest.getLogEntriesFor("id");
        assertNotNull(fetchedEntries);
        int entriesCount = fetchedEntries.size();
        assertEquals(entriesCount, 1);
        LogEntry fetchedEntry = fetchedEntries.get(0);
        assertNotNull(fetchedEntry);
        assertEquals("id", fetchedEntry.getDocUUID());
    }

    public void testByFilter() {
        LogEntry one = doCreateEntryAndPersist("id");
        @SuppressWarnings("unused")
        LogEntry two = doCreateEntryAndPersist("id");

        Map<String, FilterMapEntry> filters = new HashMap<String, FilterMapEntry>();
        FilterMapEntry filterOne = new FilterMapEntry();
        filterOne.setQueryParameterName("yop");
        filterOne.setColumnName("eventDate");
        filterOne.setOperator("=");
        filterOne.setObject(one.getEventDate());
        filters.put("oups", filterOne);
        List<LogEntry> entries = providerUnderTest.getLogEntriesFor("id",
                filters, true);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals(one.getId(), entries.get(0).getId());
    }

    public void testById() {
        LogEntry one = doCreateEntryAndPersist("id");
        LogEntry fetchedEntry = providerUnderTest.getLogEntryByID(one.getId());
        assertNotNull(fetchedEntry);
        assertEquals(one.getId(), fetchedEntry.getId());
    }

    public void testByNativeQuery() {
        doCreateEntryAndPersist("one");
        doCreateEntryAndPersist("two");
        List<LogEntry> entries = providerUnderTest.nativeQueryLogs(
                "log.extendedInfos['id'] is not null order by log.eventDate desc",
                2, 1);
        assertNotNull(entries);
        int entryCount = entries.size();
        assertEquals(1, entryCount);
    }

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

    public void testLogsByPage() {
        LogEntry one = doCreateEntryAndPersist("one");
        Date limit = new Date();
        LogEntry two = doCreateEntryAndPersist("two");
        LogEntry three = doCreateEntryAndPersist("three");
        one.setCategory("nuch");
        three.setCategory("nuch");
        List<LogEntry> entries = providerUnderTest.queryLogsByPage(eventIds(),
                limit, "nuch", "/", 1, 1);
        assertNotNull(entries);
        int entryCount = entries.size();
        assertEquals(1, entryCount);
        assertEquals("three", entries.get(0).getDocUUID());
    }
    
    public void testRemove() {
        LogEntry one = doCreateEntryAndPersist("one");
        Date limit = new Date();
        LogEntry two = doCreateEntryAndPersist("two");
        LogEntry three = doCreateEntryAndPersist("three");
        int count = providerUnderTest.removeEntries(eventId(), "/");
        assertEquals(3, count);
    }
}
