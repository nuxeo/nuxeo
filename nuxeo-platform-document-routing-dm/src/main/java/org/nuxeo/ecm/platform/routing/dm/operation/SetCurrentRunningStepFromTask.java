/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 * Set the current running step as <document.routing.step> context variable.
 * 
 * @author mcedica
 * @since 5.6
 * 
 */
@Operation(id = SetCurrentRunningStepFromTask.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Set Current Step from Task", description = "Set the current running step as <document.routing.step> context variable.")
public class SetCurrentRunningStepFromTask extends AbstractTaskStepOperation {
    public final static String ID = "Document.Routing.SetRunningStepFromTask";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @OperationMethod
    public void setStepDocument() throws ClientException {
        String stepDocumentId = getRoutingStepDocumentId(context);
        DocumentModel docStep = session.getDocument(new IdRef(stepDocumentId));
        context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
                docStep.getAdapter(DocumentRouteStep.class));
    }

}
