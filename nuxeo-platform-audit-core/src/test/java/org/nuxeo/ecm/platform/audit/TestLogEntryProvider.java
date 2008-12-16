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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.HibernateConfiguration;
import org.nuxeo.ecm.platform.audit.service.LogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.PersistenceProvider;

/**
 * Test the log entries persistence
 * 
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestLogEntryProvider extends TestCase {

    public static class TestHibernateConfiguration implements
            HibernateConfiguration {

        public Properties getProperties() {
            Properties properties = new Properties();
            properties.put("hibernate.connection.url",
                    "jdbc:hsqldb:mem:.;sql.enforce_strict_size=true");
            properties.put("hibernate.connection.driver_class",
                    "org.hsqldb.jdbcDriver");
            properties.put("hibernate.connection.auto_commit", "true");
            properties.put("hibernate.connection.pool_size", "1");
            properties.put("hibernate.dialect",
                    "org.hibernate.dialect.HSQLDialect");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", "true");
            properties.put("hibernate.format_sql", "true");

            return properties;
        }

    }

    protected PersistenceProvider persistenceProvider;

    protected EntityManager entityManager;

    private LogEntryProvider providerUnderTest;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        PersistenceProvider.hibernateConfigurationClass = TestHibernateConfiguration.class;
        persistenceProvider = new PersistenceProvider();

        persistenceProvider.openPersistenceUnit();

        entityManager = persistenceProvider.acquireEntityManagerWithActiveTransaction();

        providerUnderTest = LogEntryProvider.createProvider(entityManager);
    }

    public void tearDown() {
        persistenceProvider.releaseEntityManager(entityManager);
    }

    protected Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<String, ExtendedInfo>();
        ExtendedInfo info = ExtendedInfo.createExtendedInfo(new Long(1));
        infos.put("one", info);
        return infos;
    }

    protected LogEntry createEntry() {
        LogEntry createdEntry = new LogEntry();
        createdEntry.setEventId(TestLogEntryProvider.class.getSimpleName());
        createdEntry.setExtendedInfos(createExtendedInfos());
        return createdEntry;
    }

    protected List<LogEntry> doEncapsulate(LogEntry entry) {
        List<LogEntry> entries = new ArrayList<LogEntry>(1);
        entries.add(entry);
        return entries;
    }

    public void testAddLogEntry() throws AuditException {
        LogEntry entry = createEntry();
        providerUnderTest.addLogEntry(entry);
        boolean hasId = entry.getId() != 0;
        assertTrue(hasId);
    }

    public void testHavingKey() throws AuditException {
        LogEntry entry = createEntry();
        providerUnderTest.addLogEntry(entry);
        List<LogEntry> entries = providerUnderTest.nativeQueryLogs(
                "log.id = "
                        + entry.getId()
                        + " and log.extendedInfos['one'] is not null order by log.eventDate DESC",
                1, 10);
        assertTrue(entries.size() == 1);
        assertEquals(new Long(1L),
                entries.get(0).getExtendedInfos().get("one").getValue(
                        Long.class));
    }

}
