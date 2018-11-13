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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_NAME;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.lib.stream.computation.AbstractBatchComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.jodah.failsafe.RetryPolicy;

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

    public static final int DEFAULT_BATCH_SIZE = 100;

    public static final int DEFAULT_BATCH_THRESHOLD_MS = 200;

    @Override
    public Topology getTopology(Map<String, String> options) {
        int batchSize = getOptionAsInteger(options, BATCH_SIZE_OPT, DEFAULT_BATCH_SIZE);
        int batchThresholdMs = getOptionAsInteger(options, BATCH_THRESHOLD_MS_OPT, DEFAULT_BATCH_THRESHOLD_MS);
        RetryPolicy retryPolicy = new RetryPolicy().withBackoff(1, 65, TimeUnit.SECONDS);
        ComputationPolicy policy = new ComputationPolicyBuilder().batchPolicy(batchSize,
                Duration.ofMillis(batchThresholdMs)).retryPolicy(retryPolicy).continueOnFailure(false).build();
        return Topology.builder()
                       .addComputation(
                               () -> new AuditLogWriterComputation(COMPUTATION_NAME, policy),
                               Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public static class AuditLogWriterComputation extends AbstractBatchComputation {

        public AuditLogWriterComputation(String name, ComputationPolicy policy) {
            super(name, 1, 0, policy);
        }

        @Override
        public void batchProcess(ComputationContext context, String inputStreamName, List<Record> records) {
            List<LogEntry> logEntries = new ArrayList<>(records.size());
            for (Record record : records) {
                try {
                    logEntries.add(getLogEntryFromJson(record.getData()));
                } catch (NuxeoException e) {
                    log.error("Discard invalid record: " + record, e);
                }
            }
            writeEntriesToAudit(logEntries);
        }

        @Override
        public void batchFailure(ComputationContext context, String inputStreamName, List<Record> records) {
            log.error("Stopping AuditLogWriter computation after failures");
        }

        protected void writeEntriesToAudit(List<LogEntry> logEntries) {
            if (logEntries.isEmpty()) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Writing %d log entries to audit backend.", logEntries.size()));
            }
            AuditLogger logger = Framework.getService(AuditLogger.class);
            logger.addLogEntries(logEntries);
        }

        protected LogEntry getLogEntryFromJson(byte[] data) {
            String json = "";
            try {
                json = new String(data, UTF_8);
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
