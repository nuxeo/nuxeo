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
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk action to retrieve event-based retention rules. For each rule, a
 * {@link org.nuxeo.retention.actions.EvalInputEventBasedRuleAction} is scheduled.
 *
 * @since 11.1
 */
public class ProcessRetentionEventAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "processRetentionEvent";

    public static final String ACTION_FULL_NAME = "retention/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(ProcessRetentionEventComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class ProcessRetentionEventComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(ProcessRetentionEventComputation.class);

        protected boolean disableAudit;

        protected RetentionManager retentionManager;

        public ProcessRetentionEventComputation() {
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
            for (DocumentModel ruleDoc : loadDocuments(session, ids)) {
                if (!ruleDoc.hasFacet(RetentionConstants.RETENTION_RULE_FACET)) {
                    log.debug("Document {} is not a retention rule, ignoring ...", ruleDoc::getPathAsString);
                    continue;
                }
                RetentionRule rule = ruleDoc.getAdapter(RetentionRule.class);
                scheduleInputEventBasedRule(rule);
            }
        }

        protected void scheduleInputEventBasedRule(RetentionRule rule) {
            if (!rule.isEnabled() || !rule.isEventBased()) {
                throw new IllegalArgumentException("Rule is disabled or not event-based");
            }
            BulkService bulkService = Framework.getService(BulkService.class);
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            StringBuilder query = new StringBuilder(RetentionConstants.RULE_RECORD_DOCUMENT_QUERY);
            query.append(" AND ") //
                 .append(RetentionConstants.RECORD_RULE_IDS_PROP) //
                 .append(" = '" + rule.getDocument().getId() + "'");
            for (String repositoryName : repositoryService.getRepositoryNames()) {
                BulkCommand command = new BulkCommand.Builder(EvalInputEventBasedRuleAction.ACTION_NAME,
                        query.toString(), SecurityConstants.SYSTEM_USERNAME).repository(repositoryName).build();
                bulkService.submit(command);
            }
        }
    }

}
