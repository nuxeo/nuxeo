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
package org.nuxeo.ecm.platform.routing.core.runner;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingPersister;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class CreateNewRouteInstanceUnrestricted extends
        UnrestrictedSessionRunner {

    protected DocumentModel instance;

    protected DocumentRoute model;

    protected List<String> docIds;

    protected boolean startInstance;

    protected String initiator;

    protected DocumentRoutingPersister persister;

    /**
     *
     * @param session
     * @param model model of route. This document is not manipulated (ie: it is
     *            ok to use a document model)
     * @param docIds
     */
    public CreateNewRouteInstanceUnrestricted(CoreSession session,
            DocumentRoute model, List<String> docIds, boolean startInstance,
            DocumentRoutingPersister persister) {
        super(session);
        this.model = model;
        this.docIds = docIds;
        this.startInstance = startInstance;
        this.initiator = session.getPrincipal().getName();
        this.persister = persister;
    }

    @Override
    public void run() throws ClientException {
        instance = persister.createDocumentRouteInstanceFromDocumentRouteModel(
                model.getDocument(), session);
        DocumentRoute routeInstance = instance.getAdapter(DocumentRoute.class);
        routeInstance.setAttachedDocuments(docIds);
        routeInstance.save(session);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(DocumentRoutingConstants.INITIATOR_EVENT_CONTEXT_KEY,
                initiator);
        fireEvent(session, routeInstance, props,
                DocumentRoutingConstants.Events.beforeRouteReady.name());
        routeInstance.setReady(session);
        fireEvent(session, routeInstance, props,
                DocumentRoutingConstants.Events.afterRouteReady.name());
        routeInstance.save(session);
        if (startInstance) {
            fireEvent(session, routeInstance, null,
                    DocumentRoutingConstants.Events.beforeRouteStart.name());
            getEngineService().start(routeInstance, session);
        }
    }

    public DocumentRoute getInstance() {
        return instance.getAdapter(DocumentRoute.class);
    }

    protected DocumentRoutingEngineService getEngineService() {
        try {
            return Framework.getService(DocumentRoutingEngineService.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void fireEvent(CoreSession coreSession,
            DocumentRouteElement element,
            Map<String, Serializable> eventProperties, String eventName) {
        if (eventProperties == null) {
            eventProperties = new HashMap<String, Serializable>();
        }
        eventProperties.put(
                DocumentRoutingConstants.DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY,
                element);
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                DocumentRoutingConstants.ROUTING_CATEGORY);
        DocumentEventContext envContext = new DocumentEventContext(coreSession,
                coreSession.getPrincipal(), element.getDocument());
        envContext.setProperties(eventProperties);
        try {
            getEventProducer().fireEvent(envContext.newEvent(eventName));
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    protected EventProducer getEventProducer() {
        try {
            return Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
