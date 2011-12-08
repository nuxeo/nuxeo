/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.helper.StepResumeRunner;


@Operation(id = SetTaskDone.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Set Task Done", description = "Set the task as done.")
public class SetTaskDone extends AbstractTaskStepOperation {
    public final static String ID = "Document.Routing.Task.Done";

    @Context
    protected OperationContext context;

    @OperationMethod
    public void setTaskDone() throws ClientException {
        String stepDocumentId = getRoutingStepDocumentId(context);
        StepResumeRunner runner = new StepResumeRunner(stepDocumentId);
        runner.resumeStep(context.getCoreSession());
    }

}
