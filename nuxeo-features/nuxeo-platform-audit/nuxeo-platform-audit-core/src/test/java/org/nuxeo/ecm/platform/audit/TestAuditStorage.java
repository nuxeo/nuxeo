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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Predicates;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 9.3
 */
public class TestAuditStorage extends TestNXAuditEventsService {

    @Test
    public void testSaveAndScroll() throws JsonProcessingException {

        DefaultAuditBackend backend = (DefaultAuditBackend) serviceUnderTest;
        List<String> jsonEntries = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < 42; i++) {
            ObjectNode logEntryJson = mapper.createObjectNode();
            logEntryJson.put("eventId", "idForAuditStorage");
            jsonEntries.add(mapper.writeValueAsString(logEntryJson));
        }

        // Save JSON entries into backend
        backend.append(jsonEntries);

        // Query all logs
        AuditQueryBuilder builder = new AuditQueryBuilder().predicates(Predicates.eq("eventId", "idForAuditStorage"));
        // builder.predicates()
        List<LogEntry> logs = backend.queryLogs(builder);
        assertEquals(42, logs.size());

        ScrollResult<LogEntry> scrollResult = backend.scroll(builder, 5, 10);
        int total = 0;
        while (scrollResult.hasResults()) {
            assertTrue(scrollResult.getResults().size() <= 5);
            List<LogEntry> entries = scrollResult.getResults();
            entries.forEach(entry -> assertEquals("idForAuditStorage", entry.getEventId()));
            total += entries.size();
            scrollResult = backend.scroll(scrollResult.getScrollId());
        }
        assertEquals(42, total);
    }
}
