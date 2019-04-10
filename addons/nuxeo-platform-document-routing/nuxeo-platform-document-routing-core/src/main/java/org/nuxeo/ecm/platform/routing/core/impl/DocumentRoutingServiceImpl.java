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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LocalizableDocumentRouteElement;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingPersister;
import org.nuxeo.ecm.platform.routing.core.runner.CreateNewRouteInstanceUnrestricted;
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

    public static final String PERSISTER_XP = "persister";

    protected Map<String, String> typeToChain = new HashMap<String, String>();

    protected DocumentRoutingPersister persister;

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
        } else if (PERSISTER_XP.equals(extensionPoint)) {
            PersisterDescriptor des = (PersisterDescriptor) contribution;
            persister = des.getKlass().newInstance();
        }
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> docIds, CoreSession session, boolean startInstance) {
        CreateNewRouteInstanceUnrestricted runner = new CreateNewRouteInstanceUnrestricted(
                session, model, docIds, startInstance, persister);
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        return runner.getInstance();
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
    public DocumentRoute validateRouteModel(final DocumentRoute routeModel,
            CoreSession userSession) throws ClientException {
        new UnrestrictedSessionRunner(userSession) {
            @Override
            public void run() throws ClientException {
                DocumentRoute route = session.getDocument(
                        routeModel.getDocument().getRef()).getAdapter(
                        DocumentRoute.class);
                route.validate(session);
            }
        }.runUnrestricted();
        return userSession.getDocument(routeModel.getDocument().getRef()).getAdapter(DocumentRoute.class);
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

    public List<DocumentRoute> getDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId) {
        List<DocumentRouteElement.ElementLifeCycleState> states = new ArrayList<DocumentRouteElement.ElementLifeCycleState>();
        states.add(DocumentRouteElement.ElementLifeCycleState.ready);
        states.add(DocumentRouteElement.ElementLifeCycleState.running);
        return getDocumentRoutesForAttachedDocument(session, attachedDocId,
                states);
    }

    public List<DocumentRoute> getDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId,
            List<DocumentRouteElement.ElementLifeCycleState> states) {
        DocumentModelList list = null;
        StringBuilder statesString = new StringBuilder();
        if (states != null && !states.isEmpty()) {
            statesString.append(" ecm:currentLifeCycleState IN (");
            for (DocumentRouteElement.ElementLifeCycleState state : states) {
                statesString.append("'" + state.name() + "',");
            }
            statesString.deleteCharAt(statesString.length() - 1);
            statesString.append(") AND");
        }
        String RELATED_TOUTES_QUERY = String.format(
                " SELECT * FROM DocumentRoute WHERE " + statesString.toString()
                        + " docri:participatingDocuments IN ('%s') ",
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

    @Override
    public boolean canUserCreateRoute(NuxeoPrincipal currentUser) {
        return currentUser.getGroups().contains(
                DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME);
    }

    @Override
    public boolean canUserValidateRoute(NuxeoPrincipal currentUser) {
        return currentUser.getGroups().contains(
                DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME);
    }
}