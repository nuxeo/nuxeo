/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.audit.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditRouterIntrospection;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.Route;
import org.nuxeo.audit.mem.MemAuditFeature;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Integration tests for {@link StreamAuditWriter.AuditLogWriterComputation} using {@link MemAuditFeature}.
 *
 * @since 2025.21
 */
@RunWith(FeaturesRunner.class)
@Features(MemAuditFeature.class)
public class TestStreamAuditWriter {

    @Inject
    protected AuditBackend backend;

    /** Verifies that {@code batchProcess()} completes normally on stream replay with duplicate entry ids. */
    @Test
    public void testBatchProcessHandlesReplayWithDuplicateEntries() throws IOException {
        // Build an entry with an explicit id + logDate, as it would look after the sequencer
        // has already assigned its id during the first batchProcess() attempt
        var duplicate = LogEntry.builder("testEvent", new Date()).id(1000L).logDate(new Date()).build();
        var newEntry = LogEntry.builder("testEvent", new Date()).id(1001L).logDate(new Date()).build();

        // First insertion: simulates what the first batchProcess() run already persisted
        backend.insertLogs(List.of(duplicate));

        // Serialize both entries as stream Records (same JSON path as StreamAuditEventListener)
        var ctx = RenderingContext.CtxBuilder.get();
        var duplicateRecord = Record.of("replay", MarshallerHelper.objectToJson(duplicate, ctx).getBytes(UTF_8));
        var newRecord = Record.of("new", MarshallerHelper.objectToJson(newEntry, ctx).getBytes(UTF_8));

        // Create a computation that routes entries directly to the backend preserving their ids.
        // This simulates a replay where the KV-store sequencer has been reset to the same block
        // (e.g. after AuditCleanerFeature tears down the KV store between tests, as in NXP-33645).
        var computation = new SequencerResetComputation(backend);

        // batchProcess() must complete normally: MemAuditBackend.insertLogs() will throw
        // ConcurrentUpdateException for id=1000 which the fix must catch and treat as a no-op.
        // Non-duplicate entries in the same batch (id=1001) must still be persisted.
        computation.batchProcess(mock(ComputationContext.class), "audit/audit", List.of(duplicateRecord, newRecord));

        // The duplicate entry is still present and the new entry has been persisted
        assertNotNull(backend.getLogEntryByID(1000L));
        assertNotNull(backend.getLogEntryByID(1001L));
    }

    /** Routes entries directly to the backend preserving their ids, simulating a sequencer reset replay. */
    private static class SequencerResetComputation extends StreamAuditWriter.AuditLogWriterComputation {

        private final AuditBackend backend;

        SequencerResetComputation(AuditBackend backend) {
            super("test/writer");
            this.backend = backend;
        }

        @Override
        protected AuditRouter getAuditRouter() {
            return new DirectInsertRouter(backend);
        }
    }

    /** Calls {@link AuditBackend#insertLogs} directly, preserving entry ids without going through the sequencer. */
    private static class DirectInsertRouter implements AuditRouter {

        private final AuditBackend backend;

        DirectInsertRouter(AuditBackend backend) {
            this.backend = backend;
        }

        @Override
        public void routeToBackends(List<LogEntry> logEntries) {
            backend.insertLogs(logEntries);
        }

        @Override
        public List<LogEntry> computeLogEntries(Event event) {
            return List.of();
        }

        @Override
        public void routeToBackends(List<LogEntry> logEntries, List<Route> routes) {
        }

        @Override
        public AuditRouterIntrospection getIntrospection() {
            return null;
        }
    }
}
