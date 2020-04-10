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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.lib.stream.computation.AbstractBatchComputation;
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

    public static final String COMPUTATION_NAME = "audit/writer";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(
                               () -> new AuditLogWriterComputation(COMPUTATION_NAME),
                               Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public static class AuditLogWriterComputation extends AbstractBatchComputation {

        public AuditLogWriterComputation(String name) {
            super(name, 1, 0);
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
            // error log already done by abstract
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
            } catch (IOException e) {
                throw new NuxeoException("Invalid json logEntry" + json, e);
            }
        }
    }

}
