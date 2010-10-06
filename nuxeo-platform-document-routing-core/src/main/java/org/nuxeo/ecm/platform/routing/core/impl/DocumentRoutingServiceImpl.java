/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LocalizableDocumentRouteElement;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingPersistenceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author arussel
 *
 */
public class DocumentRoutingServiceImpl extends DefaultComponent implements
        DocumentRoutingService {

    private static final String AVAILABLE_ROUTES_QUERY = String.format(
            "Select * from %s",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    public static final String CHAINS_TO_TYPE_XP = "chainsToType";

    protected Map<String, String> typeToChain = new HashMap<String, String>();

    protected DocumentRoutingPersistenceService getPersistenceService() {
        try {
            return Framework.getService(DocumentRoutingPersistenceService.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected DocumentRoutingEngineService getEngineService() {
        try {
            return Framework.getService(DocumentRoutingEngineService.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CHAINS_TO_TYPE_XP.equals(extensionPoint)) {
            ChainToTypeMappingDescriptor desc = (ChainToTypeMappingDescriptor) contribution;
            typeToChain.put(desc.getDocumentType(), desc.getChainId());
        }
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> docIds, CoreSession session, boolean startInstance) {
        DocumentModel routeInstanceDoc = getPersistenceService().createDocumentRouteInstanceFromDocumentRouteModel(
                model.getDocument(), session);
        DocumentRoute routeInstance = routeInstanceDoc.getAdapter(DocumentRoute.class);
        routeInstance.setAttachedDocuments(docIds);
        fireEvent(session, routeInstance, null,
                DocumentRoutingConstants.Events.beforeRouteReady.name());
        routeInstance.setReady(session);
        fireEvent(session, routeInstance, null,
                DocumentRoutingConstants.Events.afterRouteReady.name());
        routeInstance.save(session);
        if (Framework.isTestModeSet()) {
            Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        }
        if (startInstance) {
            fireEvent(session, routeInstance, null,
                    DocumentRoutingConstants.Events.beforeRouteStart.name());
            getEngineService().start(routeInstance, session);
        }
        return routeInstance;
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

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session, boolean startInstance) {
        return createNewInstance(model, Collections.singletonList(documentId),
                session, startInstance);
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session) {
        return createNewInstance(model, documentIds, session, true);
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session) {
        return createNewInstance(model, Collections.singletonList(documentId),
                session, true);
    }

    @Override
    public List<DocumentRoute> getAvailableDocumentRouteModel(
            CoreSession session) {
        DocumentModelList list = null;
        try {
            list = session.query(AVAILABLE_ROUTES_QUERY);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        List<DocumentRoute> routes = new ArrayList<DocumentRoute>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
        return routes;
    }

    @Override
    public String getOperationChainId(String documentType) {
        return typeToChain.get(documentType);
    }

    @Override
    public void validateRouteModel(DocumentRoute routeModel, CoreSession session)
            throws ClientException {
        routeModel.validate(session);
    }

    @Override
    public void getRouteElements(DocumentRouteElement routeElementDocument,
            CoreSession session,
            List<LocalizableDocumentRouteElement> routeElements, int depth)
            throws ClientException {
        if (depth > 0) {
            routeElements.add(new LocalizableDocumentRouteElement(
                    routeElementDocument, depth));
        }
        DocumentModelList children = session.getChildren(routeElementDocument.getDocument().getRef());
        if (children.size() > 0) {
            depth = depth + 1;
            for (DocumentModel documentModel : children) {
                getRouteElements(
                        documentModel.getAdapter(DocumentRouteElement.class),
                        session, routeElements, depth);
            }
        }
    }

    public List<DocumentRoute> getRelatedDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId) {
        DocumentModelList list = null;
        String RELATED_TOUTES_QUERY = String.format(
                " SELECT * FROM DocumentRoute WHERE (ecm:currentLifeCycleState = 'running' OR "
                        + " ecm:currentLifeCycleState = 'ready') AND docri:participatingDocuments IN ('%s') ",
                attachedDocId);
        try {
            list = session.query(RELATED_TOUTES_QUERY);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        List<DocumentRoute> routes = new ArrayList<DocumentRoute>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
        return routes;
    }
}