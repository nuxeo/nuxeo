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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingPersistenceService;
import org.nuxeo.runtime.api.Framework;
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

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> docIds, CoreSession session,
            boolean startInstance) {
        DocumentModel routeInstanceDoc = getPersistenceService().createDocumentRouteInstanceFromDocumentRouteModel(
                model.getDocument(), session);
        DocumentRoute routeInstance = routeInstanceDoc.getAdapter(DocumentRoute.class);
        routeInstance.setAttachedDocuments(docIds);
        routeInstance.save(session);
        if(startInstance) {
            getEngineService().start(routeInstance, session);
        }
        return routeInstance;
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

}
