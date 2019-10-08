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
package org.nuxeo.audit.storage.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.audit.TestNXAuditEventsService.MyInit.YOUPS_PATH;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.storage.impl.DirectoryAuditStorage;
import org.nuxeo.audit.storage.operation.SyncAuditLogsFromStorageOperation;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, RuntimeFeature.class, LogFeature.class, RestServerFeature.class,
        RepositoryElasticSearchFeature.class, AuditStorageFeature.class })
@RepositoryConfig(init = AuditRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestSyncAuditLogsFromStorageOperation extends RestoreAuditLogsTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected AuditBackend auditBackend;

    @Inject
    protected DirectoryAuditStorage auditStorage;

    @Inject
    protected StreamService streamService;

    @Inject
    protected AuditLogger auditLogger;

    @Test
    public void testRestoreAuditLogsFromStorage() throws Exception {
        // *** test setup: generate audit logs and validate audit service
        // objects
        generateEventsOn(session, eventService, session.getDocument(new PathRef(YOUPS_PATH)));

        // *** test condition: at this time audit storage should have some logs
        pause();
        int originalLogCount = getLogEntryCount(auditStorage, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE);
        assertThat(originalLogCount).isGreaterThan(0);

        // *** test condition: after purged audit backend, number of audit
        // backend logs should be 0
        cleanAudit(auditBackend);
		pause();
        assertEquals(0, getLogEntryCount((DefaultAuditBackend) auditBackend, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE));

        // *** test condition: execute sync operation, number of logs restored
        // should be the same as original number
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(ZonedDateTime.now().toInstant().toEpochMilli());
        Map<String, Object> params = new HashMap<>();
        params.put("after", ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli());
        Blob blob = (Blob) automationService.run(ctx, SyncAuditLogsFromStorageOperation.ID, params);
        assertEquals(originalLogCount, SyncAuditLogsFromStorageOperation.getSyncAuditLogsFromStorageCount(blob));

        // *** test condition: after executed sync operation, number of logs on
        // both storage should be the same as original number
		pause();
        assertEquals(originalLogCount, getLogEntryCount(auditStorage, 10, 10000));
        assertEquals(originalLogCount,
                getLogEntryCount((DefaultAuditBackend) auditBackend, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE));
    }
}
