/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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

import static java.util.stream.Collectors.joining;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
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
 * @since 11.5
 */
public abstract class AbstractAutomationBulkAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(AbstractAutomationBulkAction.class);

    public static final String FAIL_ON_ERROR_OPTION = "failOnError";

    public static final String OPERATION_ID = "operationId";

    public static final String OPERATION_PARAMETERS = "parameters";

    protected abstract String getActionName();

    protected String getActionFullName() {
        return "bulk/" + getActionName();
    }

    @Override
    public Topology getTopology(Map<String, String> options) {
        boolean failOnError = BooleanUtils.toBoolean(options.get(FAIL_ON_ERROR_OPTION));
        return Topology.builder()
                       .addComputation(() -> new AutomationComputation(getActionFullName(), failOnError),
                               Arrays.asList(INPUT_1 + ":" + getActionFullName(), //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class AutomationComputation extends AbstractBulkComputation {
        public static final String DOC_INPUT_TYPE = "document";

        public static final String DOCS_INPUT_TYPE = "documents";

        protected final boolean failOnError;

        protected AutomationService service;

        protected String operationId;

        protected String inputType;

        protected Map<String, ?> params;

        public AutomationComputation(String name, boolean failOnError) {
            super(name);
            this.failOnError = failOnError;
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
                handleError(documents, e);
            }
        }

        protected void runOperationOnEachDocument(CoreSession session, DocumentModelList documents) {
            for (DocumentModel doc : documents) {
                try (OperationContext ctx = new OperationContext(session)) {
                    ctx.setInput(doc);
                    service.run(ctx, operationId, params);
                } catch (OperationException | NuxeoException e) {
                    handleError(List.of(doc), e);
                }
            }
        }

        protected void handleError(List<DocumentModel> documents, Exception e) {
            String documentIds = documents.stream().map(DocumentModel::getId).collect(joining(",", "[", "]"));
            String message = String.format("Bulk Action Operation with commandId: %s fails on documents: %s",
                    command.getId(), documentIds);
            if (failOnError) {
                throw new NuxeoException(message, e);
            } else {
                delta.inError(documents.size(), message);
                log.warn(message, e);
            }
        }

        protected void checkOperation(String operationId) {
            if (StringUtils.isBlank(operationId)) {
                log.warn("No operationId provided skipping command: {}", getCurrentCommand().getId());
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
                    log.warn("Unsupported operation input type: {} for command: {}", inputType,
                            getCurrentCommand().getId());
                    return;
                }
            } catch (OperationNotFoundException e) {
                log.warn("Operation: '{}' not found, skipping command: {}", operationId, getCurrentCommand().getId());
                return;
            }
            this.operationId = operationId;
        }

        @SuppressWarnings("unchecked")
        protected void checkParams(Serializable serializable) {
            if (serializable == null) {
                params = null;
            } else if (serializable instanceof HashMap) {
                params = (Map<String, ?>) serializable;
            } else {
                log.warn("Unknown operation parameters type: {} for command: {}", serializable.getClass(), command);
                operationId = null;
            }
        }
    }

}
