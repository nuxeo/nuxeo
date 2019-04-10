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
package org.nuxeo.audit.storage.stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.audit.storage.impl.DirectoryAuditStorage;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Computation that consumes a stream of Json log entries and write them to the Directory Audit Storage.
 *
 * @since 9.10
 */
public class StreamAuditStorageWriter implements StreamProcessorTopology {
    private static final Log log = LogFactory.getLog(StreamAuditStorageWriter.class);

    public static final String COMPUTATION_NAME = "AuditStorageLogWriter";

    public static final String BATCH_SIZE_OPT = "batchSize";

    public static final String BATCH_THRESHOLD_MS_OPT = "batchThresholdMs";

    public static final int DEFAULT_BATCH_SIZE = 10;

    public static final int DEFAULT_BATCH_THRESHOLD_MS = 200;

    @Override
    public Topology getTopology(Map<String, String> options) {
        int batchSize = getOptionAsInteger(options, BATCH_SIZE_OPT, DEFAULT_BATCH_SIZE);
        int batchThresholdMs = getOptionAsInteger(options, BATCH_THRESHOLD_MS_OPT, DEFAULT_BATCH_THRESHOLD_MS);
        return Topology.builder()
                       .addComputation(() -> new AuditStorageLogWriterComputation(COMPUTATION_NAME, batchSize,
                               batchThresholdMs), Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public class AuditStorageLogWriterComputation extends AbstractComputation {
        protected final int batchSize;

        protected final int batchThresholdMs;

        protected final List<String> jsonEntries;

        public AuditStorageLogWriterComputation(String name, int batchSize, int batchThresholdMs) {
            super(name, 1, 0);
            this.batchSize = batchSize;
            this.batchThresholdMs = batchThresholdMs;
            jsonEntries = new ArrayList<>(batchSize);
        }

        @Override
        public void init(ComputationContext context) {
            log.debug(String.format("Starting computation: %s reading on: %s, batch size: %d, threshold: %dms",
                    COMPUTATION_NAME, STREAM_NAME, batchSize, batchThresholdMs));
            context.setTimer("batch", System.currentTimeMillis() + batchThresholdMs);
        }

        @Override
        public void processTimer(ComputationContext context, String key, long timestamp) {
            writeJsonEntriesToAudit(context);
            context.setTimer("batch", System.currentTimeMillis() + batchThresholdMs);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            jsonEntries.add(new String(record.data, UTF_8));
            if (jsonEntries.size() >= batchSize) {
                writeJsonEntriesToAudit(context);
            }
        }

        @Override
        public void destroy() {
            log.debug(String.format("Destroy computation: %s, pending entries: %d", COMPUTATION_NAME,
                    jsonEntries.size()));
        }

        /**
         * Store JSON entries in the Directory Audit Storage
         */
        protected void writeJsonEntriesToAudit(ComputationContext context) {
            if (jsonEntries.isEmpty()) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Writing %d log entries to the directory audit storage %s.", jsonEntries.size(),
                        DirectoryAuditStorage.NAME));
            }
            NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                         .getComponent(NXAuditEventsService.NAME);
            DirectoryAuditStorage storage = (DirectoryAuditStorage) audit.getAuditStorage(DirectoryAuditStorage.NAME);
            storage.append(jsonEntries);
            jsonEntries.clear();
            context.askForCheckpoint();
        }
    }

    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

}
