/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.audit.impl;

import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_NAME;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Computation that consumes a stream of log entries and write them to the audit backend.
 *
 * @since 9.3
 */
public class StreamAuditWriter implements StreamProcessorTopology {
    private static final Log log = LogFactory.getLog(StreamAuditWriter.class);

    public static final String COMPUTATION_NAME = "AuditLogWriter";

    public static final String BATCH_SIZE_OPT = "batchSize";

    public static final String BATCH_THRESHOLD_MS_OPT = "batchThresholdMs";

    public static final int DEFAULT_BATCH_SIZE = 10;

    public static final int DEFAULT_BATCH_THRESHOLD_MS = 200;

    @Override
    public Topology getTopology(Map<String, String> options) {
        int batchSize = getOptionAsInteger(options, BATCH_SIZE_OPT, DEFAULT_BATCH_SIZE);
        int batchThresholdMs = getOptionAsInteger(options, BATCH_THRESHOLD_MS_OPT, DEFAULT_BATCH_THRESHOLD_MS);
        return Topology.builder()
                       .addComputation(
                               () -> new AuditLogWriterComputation(COMPUTATION_NAME, batchSize, batchThresholdMs),
                               Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public static class AuditLogWriterComputation extends AbstractComputation {
        protected final int batchSize;

        protected final int batchThresholdMs;

        protected final List<LogEntry> logEntries;

        public AuditLogWriterComputation(String name, int batchSize, int batchThresholdMs) {
            super(name, 1, 0);
            this.batchSize = batchSize;
            this.batchThresholdMs = batchThresholdMs;
            logEntries = new ArrayList<>(batchSize);
        }

        @Override
        public void init(ComputationContext context) {
            log.debug(String.format("Starting computation: %s reading on: %s, batch size: %d, threshold: %dms",
                    COMPUTATION_NAME, STREAM_NAME, batchSize, batchThresholdMs));
            context.setTimer("batch", System.currentTimeMillis() + batchThresholdMs);
        }

        @Override
        public void processTimer(ComputationContext context, String key, long timestamp) {
            writeEntriesToAudit(context);
            context.setTimer("batch", System.currentTimeMillis() + batchThresholdMs);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            try {
                logEntries.add(getLogEntryFromJson(record.getData()));
            } catch (NuxeoException e) {
                log.error("Discard invalid record: " + record, e);
                return;
            }
            if (logEntries.size() >= batchSize) {
                writeEntriesToAudit(context);
            }
        }

        @Override
        public void destroy() {
            log.debug(
                    String.format("Destroy computation: %s, pending entries: %d", COMPUTATION_NAME, logEntries.size()));
        }

        protected void writeEntriesToAudit(ComputationContext context) {
            if (logEntries.isEmpty()) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Writing %d log entries to audit backend.", logEntries.size()));
            }
            AuditLogger logger = Framework.getService(AuditLogger.class);
            logger.addLogEntries(logEntries);
            logEntries.clear();
            context.askForCheckpoint();
        }

        protected LogEntry getLogEntryFromJson(byte[] data) {
            String json = "";
            try {
                json = new String(data, "UTF-8");
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, LogEntryImpl.class);
            } catch (UnsupportedEncodingException e) {
                throw new NuxeoException("Discard log entry, invalid byte array", e);
            } catch (IOException e) {
                throw new NuxeoException("Invalid json logEntry" + json, e);
            }
        }
    }

    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.valueOf(value);
    }

}
