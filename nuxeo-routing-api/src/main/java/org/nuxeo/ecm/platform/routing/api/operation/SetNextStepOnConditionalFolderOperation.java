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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api.operation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.helper.ConditionalFolderUpdateRunner;

/***
 * Set the position of the child to be run once the step with the given id it's finished.
 *
 * @since 5.5
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
@Operation(id = SetNextStepOnConditionalFolderOperation.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Choose branch", description = "Update branch to be executed ", addToStudio = false)
public class SetNextStepOnConditionalFolderOperation {

    public static final String ID = "Update.NextStep.ConditionalFolder";

    @Param(name = "nextStepPos", required = true)
    protected String nextStepPos;

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @OperationMethod
    public void setStepToBeExcutedNext() {
        DocumentRouteStep step = (DocumentRouteStep) context.get(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
        ConditionalFolderUpdateRunner condFolderUpdater = new ConditionalFolderUpdateRunner(step.getDocument().getId());
        condFolderUpdater.setStepToBeExecutedNext(session, nextStepPos);
    }
}
