/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.adapter;

import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.task.TaskImpl;

/**
 * @deprecated since 5.9.2 - Use only routes of type 'graph' The facet 'RoutingTask' is still used to mark tasks created
 *             by the workflow, but it this class is marked as deprecated as it extends the deprecated ActionableObject
 */
@Deprecated
public class RoutingTaskImpl extends TaskImpl implements RoutingTask {

    public RoutingTaskImpl(DocumentModel doc) {
        super(doc);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession coreSession) {
        // Don't handle all target documents ids as it will be removed in 9.10
        DocumentRef stepIdRef = new IdRef(getTargetDocumentsIds().get(0));
        DocumentModel targetDocument = coreSession.getDocument(stepIdRef);
        DocumentModelList docList = new DocumentModelListImpl();
        docList.add(targetDocument);
        return docList;
    }

    @Override
    public DocumentRouteStep getDocumentRouteStep(CoreSession coreSession) {
        String docStepId = getVariable(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
        DocumentRef stepIdRef = new IdRef(docStepId);
        DocumentModel docStep = coreSession.getDocument(stepIdRef);
        return docStep.getAdapter(DocumentRouteStep.class);
    }

    @Override
    public String getRefuseOperationChainId() {
        return getVariable(OperationTaskVariableName.rejectOperationChain.name());
    }

    @Override
    public String getValidateOperationChainId() {
        return getVariable(OperationTaskVariableName.acceptOperationChain.name());
    }

}
