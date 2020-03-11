/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.audit.RoutingAuditHelper;

/**
 * @author arussel
 */
public class DocumentRouteImpl extends DocumentRouteStepsContainerImpl implements DocumentRoute {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentRouteImpl.class);

    public DocumentRouteImpl(DocumentModel doc, ElementRunner runner) {
        super(doc, runner);
    }

    @Override
    public boolean canUndoStep(CoreSession session) {
        return false;
    }

    protected void fireWorkflowCompletionEvent(CoreSession session) {
        EventFirer.fireEvent(session, this, null, DocumentRoutingConstants.Events.afterRouteFinish.name());

        Map<String, Serializable> eventProperties = new HashMap<>();

        // First compute duration
        long duration = RoutingAuditHelper.computeDurationSinceWfStarted(getDocument().getId());
        if (duration >= 0) {
            eventProperties.put(RoutingAuditHelper.TIME_SINCE_WF_STARTED, duration);
        }

        eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, this.getInitiator());

        // Add common info about workflow
        if (this instanceof GraphRoute) {
            eventProperties.put(RoutingAuditHelper.WORKFLOW_VARIABLES, (Serializable) ((GraphRoute) this).getVariables());
        }
        eventProperties.put("modelId", getModelId());
        eventProperties.put("modelName", getModelName());
        EventFirer.fireEvent(session, this, eventProperties, DocumentRoutingConstants.Events.afterWorkflowFinish.name());
    }

    @Override
    public String getInitiator() {
        return (String) document.getPropertyValue(DocumentRoutingConstants.ROUTING_INITIATOR_ID_KEY);
    }

    /**
     * @since 7.2
     */
    @Override
    public String getModelId() {
        return (String) document.getPropertyValue(DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCE_MODEL_ID);
    }

    /**
     * @since 7.2
     */
    @Override
    public String getModelName() {
        int firstDot = getName().indexOf(".");
        return firstDot > 0 ? getName().substring(0, firstDot) : getName();
    }

    @Override
    public void setDone(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toDone, session, false);

        fireWorkflowCompletionEvent(session);

        // Fire events for route audit log
        for (String attachDocumentID : this.getAttachedDocuments()) {
            try {
                DocumentModel doc = session.getDocument(new IdRef(attachDocumentID));
                AuditEventFirer.fireEvent(session, this, null, "auditLogRoute", doc);
            } catch (DocumentNotFoundException e) {
                log.error(String.format("Unable to fetch document with id '%s': %s", attachDocumentID, e.getMessage()));
                log.debug(e, e);
            }
        }
    }

}
