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
 *     Funsho David
 *
 */

package org.nuxeo.audit.storage.impl;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.mongodb.audit.MongoDBAuditFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, MongoDBAuditFeature.class })
@Deploy("org.nuxeo.audit.storage.directory")
public class TestRestorationFromDirectoryAuditStorage {

    @Test
    public void testRestoration() throws Exception {

        String testEventId = "testEventId";
        int nbEntries = 5000;

        QueryBuilder queryBuilder = new AuditQueryBuilder().predicates(Predicates.eq(LOG_EVENT_ID, testEventId));

        ObjectMapper mapper = new ObjectMapper();
        List<String> jsonEntries = new ArrayList<>();
        for (long i = 1; i <= nbEntries; i++) {
            ObjectNode logEntryJson = mapper.createObjectNode();
            logEntryJson.put(LOG_ID, i);
            logEntryJson.put(LOG_EVENT_ID, testEventId);
            jsonEntries.add(mapper.writeValueAsString(logEntryJson));
        }

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        DirectoryAuditStorage storage = (DirectoryAuditStorage) audit.getAuditStorage(DirectoryAuditStorage.NAME);

        storage.append(jsonEntries);
        try (Session storageSession = storage.getAuditDirectory().getSession()) {
            assertEquals(nbEntries, storageSession.query(Collections.emptyMap()).size());
        }

        AuditBackend backend = audit.getBackend();
        assertEquals(0, backend.queryLogs(queryBuilder).size());

        backend.restore(storage, 500, 10);

        List<LogEntry> logs = backend.queryLogs(queryBuilder);
        assertEquals(nbEntries, logs.size());
    }

}
