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
package org.nuxeo.retention.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;

/**
 * @since 11.1
 */
@Operation(id = AttachRetentionRule.ID, category = Constants.CAT_DOCUMENT, label = "Attach Retation Rule", description = "Attach the given retation rule to the input document.")
public class AttachRetentionRule {

    public static final String ID = "Retention.AttachRule";

    @Context
    protected CoreSession session;

    @Context
    protected RetentionManager retentionManager;

    @Param(name = "rule")
    protected DocumentModel rule;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) {
        if (!rule.hasFacet(RetentionConstants.RETENTION_RULE_FACET)) {
            throw new NuxeoException(String.format("Document is not a rule"));
        }
        RetentionRule rr = rule.getAdapter(RetentionRule.class);
        if (!rr.isManual()) {
            throw new IllegalArgumentException("Only manual rule can be manually attached");
        }
        document = retentionManager.attachRule(document, rr, session);
        return document;
    }

}
