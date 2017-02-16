/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Run the operation chain for this step.
 *
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
@Deprecated
public class StepElementRunner implements ElementRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        if (element.isRunning()) {
            return;
        } else {
            element.setRunning(session);
        }
        if (!(element instanceof DocumentRouteStep)) {
            throw new RuntimeException("Method run should be overriden in parent class.");
        }
        EventFirer.fireEvent(session, element, null, DocumentRoutingConstants.Events.beforeStepRunning.name());
        try (OperationContext context = new OperationContext(session)) {
            context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, element);
            context.setInput(element.getAttachedDocuments(session));
            if (!element.isDone()) {
                EventFirer.fireEvent(session, element, null, DocumentRoutingConstants.Events.stepWaiting.name());
            }
            String chainId = getDocumentRoutingService().getOperationChainId(element.getDocument().getType());
            getAutomationService().run(context, chainId);
        } catch (OperationException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void run(CoreSession session, DocumentRouteElement element, Map<String, Serializable> map) {
        run(session, element);
    }

    @Override
    public void resume(CoreSession session, DocumentRouteElement element, String nodeId, String taskId,
            Map<String, Object> data, String status) {
        throw new UnsupportedOperationException();
    }

    public AutomationService getAutomationService() {
        return Framework.getService(AutomationService.class);
    }

    public DocumentRoutingService getDocumentRoutingService() {
        return Framework.getService(DocumentRoutingService.class);
    }

    @Override
    public void undo(CoreSession session, DocumentRouteElement element) {
        EventFirer.fireEvent(session, element, null, DocumentRoutingConstants.Events.beforeUndoingStep.name());
        try (OperationContext context = new OperationContext(session)) {
            context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, element);
            context.setInput(element.getAttachedDocuments(session));
            String operationChainId;
            String docType = element.getDocument().getType();
            if (element.isDone()) {
                operationChainId = getDocumentRoutingService().getUndoFromDoneOperationChainId(docType);
            } else if (element.isRunning()) {
                operationChainId = getDocumentRoutingService().getUndoFromRunningOperationChainId(docType);
            } else {
                throw new RuntimeException("Trying to undo a step neither in done nor running state.");
            }
            getAutomationService().run(context, operationChainId);
        } catch (OperationException e) {
            throw new NuxeoException(e);
        }
        EventFirer.fireEvent(session, element, null, DocumentRoutingConstants.Events.afterUndoingStep.name());
    }

    @Override
    public void cancel(CoreSession session, DocumentRouteElement element) {
        if (element.isCanceled()) {
            return;
        }
        if (element.isReady() || element.isDone()) {
            element.setCanceled(session);
        } else if (element.isRunning()) {
            try {
                undo(session, element);
            } finally {
                element.setCanceled(session);
            }
        } else {
            throw new RuntimeException("Not allowed to cancel an element neither in ready, done or running state.");
        }
    }
}
