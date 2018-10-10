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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.BULK_LOG_MANAGER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DONE_STREAM;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestBulkProcessor {

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Inject
    public StreamService stream;

    @Test
    public void testEmptyQuery() throws InterruptedException {
        LogManager logManager = stream.getLogManager(BULK_LOG_MANAGER_NAME);
        try (LogTailer<Record> tailer = logManager.createTailer("test", DONE_STREAM)) {
            tailer.toLastCommitted();

            String nxql = "SELECT * from Document where ecm:parentId='nonExistentId'";
            assertEquals(0, session.query(nxql).size());

            String commandId = service.submit(
                    new BulkCommand.Builder("setProperties", nxql).repository(session.getRepositoryName())
                                                                  .user(session.getPrincipal().getName())
                                                                  .build());
            assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));

            BulkStatus status = service.getStatus(commandId);
            assertEquals(commandId, status.getCommandId());
            assertEquals(BulkStatus.State.COMPLETED, status.getState());
            assertEquals(0, status.getTotal());

            LogRecord<Record> record = tailer.read(Duration.ofSeconds(10));
            assertNotNull("No done status found", record);
            BulkStatus doneStatus = BulkCodecs.getStatusCodec().decode(record.message().getData());
            assertEquals(status, doneStatus);
        }

    }
}
