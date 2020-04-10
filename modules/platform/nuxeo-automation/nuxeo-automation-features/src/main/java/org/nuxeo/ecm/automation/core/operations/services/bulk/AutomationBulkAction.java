/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action that runs an automation operation
 *
 * @since 10.3
 */
public class AutomationBulkAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(AutomationBulkAction.class);

    public static final String ACTION_NAME = "automation";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String OPERATION_ID = "operationId";

    public static final String OPERATION_PARAMETERS = "parameters";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(AutomationComputation::new, Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class AutomationComputation extends AbstractBulkComputation {
        public static final String DOC_INPUT_TYPE = "document";

        public static final String DOCS_INPUT_TYPE = "documents";

        protected AutomationService service;

        protected String operationId;

        protected String inputType;

        protected Map<String, ?> params;

        public AutomationComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            operationId = null;
            service = Framework.getService(AutomationService.class);
            Map<String, Serializable> commandParams = getCurrentCommand().getParams();
            checkOperation((String) commandParams.get(OPERATION_ID));
            checkParams(commandParams.get(OPERATION_PARAMETERS));
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            if (operationId == null) {
                return;
            }
            DocumentModelList documents = loadDocuments(session, ids);
            if (DOCS_INPUT_TYPE.equals(inputType)) {
                runOperationOnAllDocuments(session, documents);
            } else {
                runOperationOnEachDocument(session, documents);
            }
        }

        protected void runOperationOnAllDocuments(CoreSession session, DocumentModelList documents) {
            try (OperationContext ctx = new OperationContext(session)) {
                ctx.setInput(documents);
                service.run(ctx, operationId, params);
            } catch (OperationException e) {
                throw new NuxeoException("Operation fails on documents: " + documents, e);
            }
        }

        protected void runOperationOnEachDocument(CoreSession session, DocumentModelList documents) {
            for (DocumentModel doc : documents) {
                try (OperationContext ctx = new OperationContext(session)) {
                    ctx.setInput(doc);
                    service.run(ctx, operationId, params);
                } catch (OperationException e) {
                    throw new NuxeoException("Operation fails on doc: " + doc.getId(), e);
                }
            }
        }

        protected void checkOperation(String operationId) {
            if (StringUtils.isBlank(operationId)) {
                log.warn("No operationId provided skipping command: " + getCurrentCommand().getId());
                return;
            }
            try {
                OperationType op = service.getOperation(operationId);
                inputType = op.getInputType();
                if (inputType == null || DOC_INPUT_TYPE.equals(inputType)) {
                    inputType = DOC_INPUT_TYPE;
                } else if (DOCS_INPUT_TYPE.equals(inputType)) {
                    inputType = DOCS_INPUT_TYPE;
                } else {
                    log.warn(String.format("Unsupported operation input type %s for command: %s", inputType,
                            getCurrentCommand().getId()));
                    return;
                }
            } catch (OperationNotFoundException e) {
                log.warn(String.format("Operation '%s' not found, skipping command: %s", operationId,
                        getCurrentCommand().getId()));
                return;
            }
            this.operationId = operationId;
        }

        protected void checkParams(Serializable serializable) {
            if (serializable == null) {
                params = null;
            } else if (serializable instanceof HashMap) {
                params = (Map<String, ?>) serializable;
            } else {
                log.warn("Unknown operation parameters type: " + serializable.getClass() + " for command: " + command);
                operationId = null;
            }
        }
    }

}
