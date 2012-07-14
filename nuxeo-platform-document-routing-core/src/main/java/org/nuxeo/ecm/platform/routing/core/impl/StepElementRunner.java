/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Run the operation chain for this step.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class StepElementRunner implements ElementRunner {
    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        if (element.isRunning()) {
            return;
        } else {
            element.setRunning(session);
        }
        if (!(element instanceof DocumentRouteStep)) {
            throw new RuntimeException(
                    "Method run should be overriden in parent class.");
        }
        EventFirer.fireEvent(session, element, null,
                DocumentRoutingConstants.Events.beforeStepRunning.name());
        OperationContext context = new OperationContext(session);
        context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
                element);
        context.setInput(element.getAttachedDocuments(session));
        if (!element.isDone()) {
            EventFirer.fireEvent(session, element, null,
                    DocumentRoutingConstants.Events.stepWaiting.name());
        }
        try {
            String chainId = getDocumentRoutingService().getOperationChainId(
                    element.getDocument().getType());
            getAutomationService().run(context, chainId);
        } catch (InvalidChainException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume(CoreSession session, DocumentRouteElement element,
            String nodeId, Map<String, Object> data, String status) {
        throw new UnsupportedOperationException();
    }

    public AutomationService getAutomationService() {
        try {
            return Framework.getService(AutomationService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentRoutingService getDocumentRoutingService() {
        try {
            return Framework.getService(DocumentRoutingService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void undo(CoreSession session, DocumentRouteElement element) {
        EventFirer.fireEvent(session, element, null,
                DocumentRoutingConstants.Events.beforeUndoingStep.name());
        OperationContext context = new OperationContext(session);
        context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
                element);
        context.setInput(element.getAttachedDocuments(session));
        String operationChainId;
        String docType = element.getDocument().getType();
        if (element.isDone()) {
            operationChainId = getDocumentRoutingService().getUndoFromDoneOperationChainId(
                    docType);
        } else if (element.isRunning()) {
            operationChainId = getDocumentRoutingService().getUndoFromRunningOperationChainId(
                    docType);
        } else {
            throw new RuntimeException(
                    "Trying to undo a step neither in done nor running state.");
        }
        try {
            getAutomationService().run(context, operationChainId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        EventFirer.fireEvent(session, element, null,
                DocumentRoutingConstants.Events.afterUndoingStep.name());
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
            throw new RuntimeException(
                    "Not allowed to cancel an element neither in ready, done or running state.");
        }
    }
}
