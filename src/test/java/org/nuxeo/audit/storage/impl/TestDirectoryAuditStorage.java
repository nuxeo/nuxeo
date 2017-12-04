/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.audit.storage.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Predicates;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, AuditFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.audit.storage" })
public class TestDirectoryAuditStorage {

    protected static DirectoryAuditStorage storage;

    protected static String jsonEntry1 = "{\"entity-type\":\"logEntry\",\"category\":\"Document\",\"principalName\":\"Administrator\","
            + "\"comment\":null,\"docLifeCycle\":\"Draft\",\"docPath\":\"/My doc 1\",\"docType\":\"File\","
            + "\"eventId\":\"documentCreated\",\"repositoryId\":\"default\",\"eventDate\":\"2017-10-10T10:35:13.102Z\","
            + "\"docUUID\":\"3f86a83f-1523-432a-92c5-8ec5f68a6451\",\"logDate\":\"2017-10-10T10:35:13.138Z\",\"extended\":{}}";

    protected static String jsonEntry2 = "{\"entity-type\":\"logEntry\",\"category\":\"Document\",\"principalName\":\"Administrator\","
            + "\"comment\":null,\"docLifeCycle\":\"Approved\",\"docPath\":\"/My doc 2\",\"docType\":\"File\","
            + "\"eventId\":\"documentModified\",\"repositoryId\":\"default\",\"eventDate\":\"2017-11-13T09:15:13.102Z\","
            + "\"docUUID\":\"4f86a82f-3521-132b-92c3-6ac5f65c6422\",\"logDate\":\"2017-11-13T09:15:13.138Z\",\"extended\":{}}";

    protected static List<String> jsonEntries = Arrays.asList(jsonEntry1, jsonEntry2);

    @BeforeClass
    public static void before() {
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        storage = (DirectoryAuditStorage) audit.getAuditStorage(DirectoryAuditStorage.NAME);
    }

    @Test
    public void testStorage() {
        assertNotNull(storage);
        assertTrue(storage instanceof DirectoryAuditStorage);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAppend() {
        storage.append(jsonEntries);

        Directory directory = storage.getAuditDirectory();
        String schemaName = directory.getSchema();
        try (Session session = directory.getSession()) {
            DocumentModelList auditEntries = session.getEntries();
            assertEquals(2, auditEntries.size());

            DocumentModel auditEntry1 = auditEntries.get(0);
            assertTrue(auditEntry1.hasSchema(schemaName));
            assertEquals(new Long(1), auditEntry1.getPropertyValue(DirectoryAuditStorage.ID_COLUMN));
            assertEquals(jsonEntry1, auditEntry1.getPropertyValue(DirectoryAuditStorage.JSON_COLUMN));

            DocumentModel auditEntry2 = auditEntries.get(1);
            assertTrue(auditEntry2.hasSchema(schemaName));
            assertEquals(new Long(2), auditEntry2.getPropertyValue(DirectoryAuditStorage.ID_COLUMN));
            assertEquals(jsonEntry2, auditEntry2.getPropertyValue(DirectoryAuditStorage.JSON_COLUMN));
        }
    }

    @Test
    public void testGetLogEntryFromJson() {
        LogEntry logEntry = storage.getLogEntryFromJson(jsonEntry1);
        assertNotNull(logEntry);
        assertEquals("Document", logEntry.getCategory());
        assertEquals("Administrator", logEntry.getPrincipalName());
        assertEquals("Draft", logEntry.getDocLifeCycle());
        assertEquals("/My doc 1", logEntry.getDocPath());
        assertEquals("File", logEntry.getDocType());
        assertEquals("documentCreated", logEntry.getEventId());
        assertEquals("default", logEntry.getRepositoryId());
        assertTrue(logEntry.getExtendedInfos().isEmpty());
    }

    @Test
    public void testQueryLogs() {
        storage.append(jsonEntries);

        // Empty query builder.
        AuditQueryBuilder queryBuilder = new AuditQueryBuilder();
        List<LogEntry> logEntries = storage.queryLogs(queryBuilder);
        assertEquals(2, storage.queryLogs(queryBuilder).size());

        // Query builder with an orderBy DESC.
        queryBuilder.order(new OrderByExpr(new Reference(DirectoryAuditStorage.ID_COLUMN), true));
        logEntries = storage.queryLogs(queryBuilder);
        assertEquals(2, logEntries.size());
        assertEquals("/My doc 2", logEntries.get(0).getDocPath());

        // Query builder with an orderBy ASC.
        queryBuilder = new AuditQueryBuilder();
        queryBuilder.order(new OrderByExpr(new Reference(DirectoryAuditStorage.ID_COLUMN), false));
        logEntries = storage.queryLogs(queryBuilder);
        assertEquals(2, logEntries.size());
        assertEquals("/My doc 1", logEntries.get(0).getDocPath());

        // Query builder with a limit and an offset.
        queryBuilder.limit(1);
        logEntries = storage.queryLogs(queryBuilder);
        assertEquals(1, logEntries.size());
        assertEquals("/My doc 1", logEntries.get(0).getDocPath());
        queryBuilder.offset(1);
        logEntries = storage.queryLogs(queryBuilder);
        assertEquals(1, logEntries.size());
        assertEquals("/My doc 2", logEntries.get(0).getDocPath());

        // Query builder with a 'AND entry = ' condition.
        queryBuilder = new AuditQueryBuilder();
        queryBuilder.addAndPredicate(Predicates.eq(DirectoryAuditStorage.JSON_COLUMN, jsonEntry2));
        logEntries = storage.queryLogs(queryBuilder);
        assertEquals(1, logEntries.size());
        assertEquals("/My doc 2", logEntries.get(0).getDocPath());

        // Query builder with a 'AND entry LIKE ' condition.
        queryBuilder = new AuditQueryBuilder();
        queryBuilder.addAndPredicate(new Predicate(new Reference(DirectoryAuditStorage.JSON_COLUMN), Operator.LIKE,
                Literals.toLiteral("My doc 2")));
        logEntries = storage.queryLogs(queryBuilder);
        assertEquals(1, logEntries.size());
        assertEquals("/My doc 2", logEntries.get(0).getDocPath());

        // Query builder with a 'AND entry IN ' condition.
        // => Not supported.
    }

    @Test
    public void testScroll() {
        storage.append(jsonEntries);
        AuditQueryBuilder queryBuilder = new AuditQueryBuilder();
        ScrollResult scrollResult = storage.scroll(queryBuilder, 10, 1);
        assertNotNull(scrollResult.getScrollId());
        List<Object> results = scrollResult.getResults();
        assertEquals(2, scrollResult.getResults().size());
        results.forEach(result -> assertTrue(result instanceof LogEntryImpl));
    }
}
