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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.actions;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk action to evaluate expression on record documents with an attached event-based retention rule. Depending on the
 * expression evaluation outcome, a determinate retention period is computed and set on the record document.
 *
 * @since 11.1
 */
public class EvalInputEventBasedRuleAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "evalInputEventBasedRule";

    public static final String ACTION_FULL_NAME = "retention/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(EvalInputEventBasedRuleComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class EvalInputEventBasedRuleComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(EvalInputEventBasedRuleComputation.class);

        protected boolean disableAudit;

        protected RetentionManager retentionManager;

        public EvalInputEventBasedRuleComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            Serializable auditParam = command.getParam(NXAuditEventsService.DISABLE_AUDIT_LOGGER);
            disableAudit = auditParam != null && Boolean.parseBoolean(auditParam.toString());
            retentionManager = Framework.getService(RetentionManager.class);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            for (DocumentModel recordDoc : loadDocuments(session, ids)) {
                if (!recordDoc.hasFacet(RetentionConstants.RECORD_FACET)) {
                    log.debug("Document {} is not a record, ignoring ...", recordDoc::getPathAsString);
                    continue;
                }
                Record record = recordDoc.getAdapter(Record.class);
                if (!record.isRetentionIndeterminate()) {
                    log.debug("Record {} has already a determinate retention date {}, ignoring ...",
                            recordDoc::getPathAsString,
                            () -> (recordDoc.getRetainUntil() == null ? null : recordDoc.getRetainUntil().toInstant()));
                    continue;
                }
                RetentionRule rule = record.getRule(session);
                if (!rule.isEventBased()) {
                    log.debug("Record {} does not have an event-based rule, ignoring ...", recordDoc::getPathAsString);
                    continue;
                }
                session.setRetainUntil(recordDoc.getRef(), record.getRule(session).getRetainUntilDateFromNow(), null);
            }
        }
    }

}
