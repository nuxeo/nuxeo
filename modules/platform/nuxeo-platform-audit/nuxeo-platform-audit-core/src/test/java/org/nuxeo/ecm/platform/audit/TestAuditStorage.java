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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.eq;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditStorage extends AbstractAuditStorageTest {

    @Inject
    protected TransactionalFeature txFeature;

    @Override
    @Test
    public void testStartsWith() throws Exception {
        super.testStartsWith();

        // A partial match is supported by the database query
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/eve");
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/od");
    }

    @Override
    protected void flush() throws Exception {
        txFeature.nextTransaction();
    }

    // NXP-30511
    @Test
    public void testSupportNullExtendedInfos() throws Exception {
        var logEntry = new LogEntryImpl();
        logEntry.setEventId("documentModified");
        logEntry.setCategory("cat");
        logEntry.setDocUUID("testSupportNullExtendedInfos");
        logEntry.setEventDate(new Date());
        logEntry.setRepositoryId("test");
        logEntry.setExtendedInfos(Map.of("nullValue", ExtendedInfoImpl.createExtendedInfo(null)));
        auditBackend.addLogEntries(List.of(logEntry));

        flush();

        var logEntries = auditBackend.queryLogs(
                new AuditQueryBuilder().predicate(eq(LOG_DOC_UUID, "testSupportNullExtendedInfos"))
                        .and(eq(LOG_EVENT_ID, "documentModified")));
        assertEquals(1, logEntries.size());
        var queriedLogEntry = logEntries.get(0);
        var nullValueExtendedInfo = queriedLogEntry.getExtendedInfos().get("nullValue");
        assertNotNull("ExtendedInfo should exist", nullValueExtendedInfo);
        assertNull("ExtendedInfo value should be null", nullValueExtendedInfo.getSerializableValue());
    }

    public static LogEntry doCreateEntry(String docId, String eventId, String category) {
        LogEntry createdEntry = new LogEntryImpl();
        createdEntry.setEventId(eventId);
        createdEntry.setCategory(category);
        createdEntry.setDocUUID(docId);
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath("/" + docId);
        createdEntry.setRepositoryId("test");
        createdEntry.setExtendedInfos(createExtendedInfos());

        return createdEntry;
    }

    protected static Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        ExtendedInfo info = ExtendedInfoImpl.createExtendedInfo(Long.valueOf(1));
        infos.put("id", info);
        return infos;
    }
}
