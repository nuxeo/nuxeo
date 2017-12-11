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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Predicates;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditStorage {

    @Inject
    protected AuditBackend backend;

    @Test
    public void testSaveAndScroll() throws JsonProcessingException {

        DefaultAuditBackend backend = (DefaultAuditBackend) this.backend;

        String idForAuditStorage = "idForAuditStorage";
        ObjectMapper mapper = new ObjectMapper();
        List<String> jsonEntries = new ArrayList<>();
        for (int i = 1; i <= 42; i++) {
            ObjectNode logEntryJson = mapper.createObjectNode();
            logEntryJson.put(LOG_ID, Integer.valueOf(i + 100).longValue());
            logEntryJson.put(LOG_EVENT_ID, idForAuditStorage);
            jsonEntries.add(mapper.writeValueAsString(logEntryJson));
        }
        // Save JSON entries into backend
        backend.append(jsonEntries);

        // Query all logs
        AuditQueryBuilder builder = new AuditQueryBuilder().predicates(Predicates.eq(LOG_EVENT_ID, idForAuditStorage));
        // builder.predicates()
        List<LogEntry> logs = backend.queryLogs(builder);
        assertEquals(42, logs.size());

        ScrollResult<String> scrollResult = backend.scroll(builder, 5, 10);
        int total = 0;
        while (scrollResult.hasResults()) {
            assertTrue(scrollResult.getResults().size() <= 5);
            jsonEntries = scrollResult.getResults();
            List<LogEntry> entries = jsonEntries.stream().map(json -> {
                try {
                    return mapper.readValue(json, LogEntryImpl.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).collect(Collectors.toList());
            for (LogEntry entry : entries) {
                assertEquals(idForAuditStorage, entry.getEventId());
            }
            total += entries.size();
            scrollResult = backend.scroll(scrollResult.getScrollId());
        }
        assertEquals(42, total);
    }
}
