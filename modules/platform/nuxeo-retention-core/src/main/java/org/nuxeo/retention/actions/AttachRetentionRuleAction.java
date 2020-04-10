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
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk action to attach a retention rule.
 *
 * @since 11.1
 */
public class AttachRetentionRuleAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "attachRetentionRule";

    public static final String ACTION_FULL_NAME = "retention/" + ACTION_NAME;

    public static final String PARAM_RULE_ID = "ruleId";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(EvaluateRuleComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class EvaluateRuleComputation extends AbstractBulkComputation {

        protected boolean disableAudit;

        protected RetentionManager retentionManager;

        protected String ruleId;

        public EvaluateRuleComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            Serializable auditParam = command.getParam(NXAuditEventsService.DISABLE_AUDIT_LOGGER);
            disableAudit = auditParam != null && Boolean.parseBoolean(auditParam.toString());
            retentionManager = Framework.getService(RetentionManager.class);
            ruleId = command.getParam(PARAM_RULE_ID);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            // Attach rule
            DocumentModel ruleDoc = session.getDocument(new IdRef(ruleId));
            RetentionRule rule = ruleDoc.getAdapter(RetentionRule.class);
            for (DocumentModel doc : loadDocuments(session, ids)) {
                if (!retentionManager.canAttachRule(doc, rule, session)) {
                    continue;
                }
                if (disableAudit) {
                    doc.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, Boolean.TRUE);
                }
                retentionManager.attachRule(doc, rule, session);
            }
        }
    }

}
