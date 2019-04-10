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
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_COMMENT;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_LIFE_CYCLE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_TYPE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EXTENDED;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_LOG_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_PRINCIPAL_NAME;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 9.10
 */
@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.audit.storage.directory")
public class TestDirectoryAuditStorage {

    protected static DirectoryAuditStorage storage;

    protected static List<String> jsonEntries;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeClass
    public static void before() throws IOException {
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        storage = (DirectoryAuditStorage) audit.getAuditStorage(DirectoryAuditStorage.NAME);

        Map<String, Object> jsonEntryMap1 = new HashMap<>();
        jsonEntryMap1.put("entity-type", "logEntry");
        jsonEntryMap1.put(LOG_CATEGORY, "Document");
        jsonEntryMap1.put(LOG_PRINCIPAL_NAME, "Administrator");
        jsonEntryMap1.put(LOG_COMMENT, null);
        jsonEntryMap1.put(LOG_DOC_LIFE_CYCLE, "Draft");
        jsonEntryMap1.put(LOG_DOC_PATH, "/My doc 1");
        jsonEntryMap1.put(LOG_DOC_TYPE, "File");
        jsonEntryMap1.put(LOG_EVENT_ID, "documentCreated");
        jsonEntryMap1.put(LOG_REPOSITORY_ID, "default");
        jsonEntryMap1.put(LOG_EVENT_DATE, "2017-10-10T10:35:13.102Z");
        jsonEntryMap1.put(LOG_DOC_UUID, "3f86a83f-1523-432a-92c5-8ec5f68a6451");
        jsonEntryMap1.put(LOG_LOG_DATE, "2017-10-10T10:35:13.138Z");
        jsonEntryMap1.put(LOG_EXTENDED, Collections.emptyMap());

        Map<String, Object> jsonEntryMap2 = new HashMap<>();
        jsonEntryMap2.put("entity-type", "logEntry");
        jsonEntryMap2.put(LOG_CATEGORY, "Document");
        jsonEntryMap2.put(LOG_PRINCIPAL_NAME, "Administrator");
        jsonEntryMap2.put(LOG_COMMENT, null);
        jsonEntryMap2.put(LOG_DOC_LIFE_CYCLE, "Approved");
        jsonEntryMap2.put(LOG_DOC_PATH, "/My doc 2");
        jsonEntryMap2.put(LOG_DOC_TYPE, "File");
        jsonEntryMap2.put(LOG_EVENT_ID, "documentModified");
        jsonEntryMap2.put(LOG_REPOSITORY_ID, "default");
        jsonEntryMap2.put(LOG_EVENT_DATE, "2017-11-13T09:15:13.102Z");
        jsonEntryMap2.put(LOG_DOC_UUID, "4f86a82f-3521-132b-92c3-6ac5f65c6422");
        jsonEntryMap2.put(LOG_LOG_DATE, "2017-11-13T09:15:13.138Z");
        jsonEntryMap2.put(LOG_EXTENDED, Collections.emptyMap());

        String jsonEntry1 = OBJECT_MAPPER.writeValueAsString(jsonEntryMap1);
        String jsonEntry2 = OBJECT_MAPPER.writeValueAsString(jsonEntryMap2);
        jsonEntries = Arrays.asList(jsonEntry1, jsonEntry2);
    }

    @Test
    public void testStorage() {
        assertNotNull(storage);
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

            String jsonEntry1 = jsonEntries.get(0);
            DocumentModel auditEntry1 = auditEntries.get(0);
            assertTrue(auditEntry1.hasSchema(schemaName));
            assertEquals(1L, auditEntry1.getPropertyValue(DirectoryAuditStorage.ID_COLUMN));
            assertEquals(jsonEntry1, auditEntry1.getPropertyValue(DirectoryAuditStorage.JSON_COLUMN));

            String jsonEntry2 = jsonEntries.get(1);
            DocumentModel auditEntry2 = auditEntries.get(1);
            assertTrue(auditEntry2.hasSchema(schemaName));
            assertEquals(2L, auditEntry2.getPropertyValue(DirectoryAuditStorage.ID_COLUMN));
            assertEquals(jsonEntry2, auditEntry2.getPropertyValue(DirectoryAuditStorage.JSON_COLUMN));
        }
    }

    @Test
    public void testQueryLogs() {
        storage.append(jsonEntries);

        // Empty query builder.
        QueryBuilder queryBuilder = new AuditQueryBuilder();
        List<LogEntry> logEntries = queryLogs(queryBuilder);
        assertEquals(2, logEntries.size());

        // Query builder with an orderBy DESC.
        queryBuilder.order(new OrderByExpr(new Reference(DirectoryAuditStorage.ID_COLUMN), true));
        logEntries = queryLogs(queryBuilder);
        assertEquals(2, logEntries.size());
        assertEquals("/My doc 2", logEntries.get(0).getDocPath());

        // Query builder with an orderBy ASC.
        queryBuilder = new AuditQueryBuilder();
        queryBuilder.order(new OrderByExpr(new Reference(DirectoryAuditStorage.ID_COLUMN), false));
        logEntries = queryLogs(queryBuilder);
        assertEquals(2, logEntries.size());
        assertEquals("/My doc 1", logEntries.get(0).getDocPath());

        // Query builder with a limit and an offset.
        queryBuilder.limit(1);
        logEntries = queryLogs(queryBuilder);
        assertEquals(1, logEntries.size());
        assertEquals("/My doc 1", logEntries.get(0).getDocPath());
        queryBuilder.offset(1);
        logEntries = queryLogs(queryBuilder);
        assertEquals(1, logEntries.size());
        assertEquals("/My doc 2", logEntries.get(0).getDocPath());

        // Queries with ' = ', 'LIKE', 'IN' operators are not supported.
    }

    @Test
    public void testScroll() {
        storage.append(jsonEntries);
        QueryBuilder queryBuilder = new AuditQueryBuilder();
        ScrollResult<String> scrollResult = storage.scroll(queryBuilder, 10, 1);
        assertNotNull(scrollResult.getScrollId());
        List<String> results = scrollResult.getResults();
        assertEquals(2, scrollResult.getResults().size());
        // check that we can deserialize them
        results.forEach(this::getLogEntryFromJson);
    }

    protected List<LogEntry> queryLogs(QueryBuilder builder) {
        return storage.queryLogs(builder).stream().map(this::getLogEntryFromJson).collect(Collectors.toList());
    }

    /**
     * Convert a Json entry to a LogEntry.
     */
    protected LogEntry getLogEntryFromJson(String jsonEntry) {
        try {
            return OBJECT_MAPPER.readValue(jsonEntry, LogEntryImpl.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
