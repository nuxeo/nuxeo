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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.bulk;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 2025.19
 */
public class CopyAuditAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "copyAudit";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String PARAMETER_BACKEND_NAME = "backendName";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(CopyAuditComputation::new, //
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class CopyAuditComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(CopyAuditComputation.class);

        protected AuditBackend sourceBackend;

        protected AuditBackend targetBackend;

        public CopyAuditComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            var auditService = Framework.getService(AuditService.class);
            // retrieve source backend
            var query = SQLQueryParser.parse(command.getQuery());
            sourceBackend = auditService.getAuditBackend(query.getFromClause().get(0));
            // retrieve target backend
            String targetBackendName = command.getParam(PARAMETER_BACKEND_NAME);
            targetBackend = auditService.getAuditBackend(targetBackendName);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            // fetch log entries
            var logEntries = sourceBackend.queryLogs(new AuditQueryBuilder().predicate(
                    Predicates.in(LOG_ID, ids.stream().map(Long::parseLong).toList())));
            try {
                // insert in target backend
                targetBackend.insertLogs(logEntries);
            } catch (ConcurrentUpdateException e) {
                log.trace("Copying logs that already exist in target backend", e);
                e.getInfos().forEach(info -> delta.incrementSkipCount());
            }
        }
    }
}
