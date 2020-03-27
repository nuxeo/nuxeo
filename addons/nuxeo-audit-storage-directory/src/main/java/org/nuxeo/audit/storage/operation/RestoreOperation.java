/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     - Ku Chang <kchang@nuxeo.com>
 */
package org.nuxeo.audit.storage.operation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class RestoreOperation {

    public static final int DEAFULT_BATCH_SIZE = 100;

    public static final int DEFAULT_KEEP_ALIVE_SECONDS = 10000;

    protected static class SyncAuditLogsFromStorage {
        public int count;
    }

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected abstract void populateResultList(List<? extends Object> resultList, String logString, LogEntry logEntry);

    protected abstract void writeEntries(Object storageDestination, List<? extends Object> entries);

    public static int getSyncAuditLogsFromStorageCount(Blob blob) {
        try {
            SyncAuditLogsFromStorage syncAuditLogsFromStorageCount = OBJECT_MAPPER.readValue(blob.getString(),
                    SyncAuditLogsFromStorage.class);
            return syncAuditLogsFromStorageCount.count;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected Blob generateOutput(int count) {
        return new StringBlob(String.format("{\"count\": %d}", count));
    }

    protected int restore(AuditStorage source, Object dest, int batchSize, int keepAlive, Long before, Long after) {
        int count = 0;
        List<String> logEntries = new ArrayList<String>();

        // get directory audit log service object
        QueryBuilder queryBuilder = new AuditQueryBuilder();
        ScrollResult<String> scrollResult = source.scroll(queryBuilder, batchSize, keepAlive);

        while (scrollResult.hasResults()) {
            List<String> results = scrollResult.getResults();
            for (String result : results) {
                LogEntry logEntry = SyncAuditLogsFromStorageOperation.getLogEntryFromJson(result);

                Date beforeDate = Date.from(Instant.ofEpochMilli(before));
                Date afterDate = Date.from(Instant.ofEpochMilli(after));

                if (logEntry.getEventDate().before(beforeDate) && logEntry.getEventDate().after(afterDate)) {
                    populateResultList(logEntries, result, logEntry);
                    count++;
                }
            }

            if (logEntries.size() > 0) {
                writeEntries(dest, logEntries);
            }

            logEntries.clear();
            scrollResult = source.scroll(scrollResult.getScrollId());
        }

        return count;
    }
}
