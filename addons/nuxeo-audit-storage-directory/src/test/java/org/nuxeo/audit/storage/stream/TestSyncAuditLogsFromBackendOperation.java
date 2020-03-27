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
import org.nuxeo.audit.storage.operation.SyncAuditLogsFromBackendOperation;
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
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, RuntimeFeature.class, LogFeature.class, RestServerFeature.class,
        RepositoryElasticSearchFeature.class, AuditStorageFeature.class })
@RepositoryConfig(init = AuditRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestSyncAuditLogsFromBackendOperation extends RestoreAuditLogsTest {

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
    protected AuditLogger auditLogger;

    @Test
    public void testRestoreAuditLogsFromBackend() throws Exception {
        // *** test setup: generate audit logs and validate audit service
        // objects
		generateEventsOn(session, eventService, session.getDocument(new PathRef(YOUPS_PATH)));

        // *** test condition: at this time audit storage should have some logs
        pause();
		int originalLogCount = getLogEntries(auditStorage, YOUPS_PATH, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE).size();
        assertThat(originalLogCount).isGreaterThan(0);

        // *** test condition: add some audit logs to audit backend only; after
        // that audit backend should have more logs than audit storage
        ZonedDateTime startTime = ZonedDateTime.now();
        createAndPersistLogEntriesToBackend(session, auditLogger, FIRE_EVENT_COUNT);
        pause();
		assertEquals(originalLogCount + FIRE_EVENT_COUNT,
				getLogEntries((DefaultAuditBackend) auditBackend, YOUPS_PATH, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE)
                .size());
		int currentStorageLogCount = getLogEntries(auditStorage, YOUPS_PATH, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE)
				.size();
        assertEquals(originalLogCount, currentStorageLogCount);

        // *** test condition: call SyncAuditLogsFromStorageOperation to restore
        // missing audit logs from audit backend to audit storage
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        ctx.setInput(ZonedDateTime.now().toInstant().toEpochMilli());
        params.put("after", startTime.toInstant().toEpochMilli());
        Blob blob = (Blob) automationService.run(ctx, SyncAuditLogsFromBackendOperation.ID, params);
        assertEquals(FIRE_EVENT_COUNT, SyncAuditLogsFromStorageOperation.getSyncAuditLogsFromStorageCount(blob));

        pause();
		assertEquals(originalLogCount + FIRE_EVENT_COUNT,
				getLogEntries(auditStorage, YOUPS_PATH, DEFAULT_BATCH_SIZE, DEFAULT_KEEP_ALIVE).size());
    }
}
