/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues.graph;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @since 5.7.2
 */
public class AbstractGraphRouteTest {

    // a doc, associated to the route
    protected DocumentModel doc;

    // the route model we'll use
    protected DocumentModel routeDoc;

    protected static final String TYPE_ROUTE_NODE = "RouteNode";

    protected DocumentModel createRoute(String name, CoreSession session)
            throws ClientException, PropertyException {
        DocumentModel route = session.createDocumentModel("/", name,
                DOCUMENT_ROUTE_DOCUMENT_TYPE);
        route.setPropertyValue(EXECUTION_TYPE_PROPERTY_NAME, graph.name());
        route.setPropertyValue("dc:title", name);
        route.setPropertyValue(ATTACHED_DOCUMENTS_PROPERTY_NAME,
                (Serializable) Collections.singletonList(doc.getId()));
        return session.createDocument(route);
    }

    protected DocumentModel createNode(DocumentModel route, String name,
            CoreSession session) throws ClientException, PropertyException {
        DocumentModel node = session.createDocumentModel(
                route.getPathAsString(), name, TYPE_ROUTE_NODE);
        node.setPropertyValue(GraphNode.PROP_NODE_ID, name);
        return session.createDocument(node);
    }

    protected Map<String, Serializable> transition(String name, String target,
            String condition) throws ClientException {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_TRANS_NAME, name);
        m.put(GraphNode.PROP_TRANS_TARGET, target);
        m.put(GraphNode.PROP_TRANS_CONDITION, condition);
        return m;
    }

    protected Map<String, Serializable> transition(String name, String target,
            String condition, String chainId) throws ClientException {
        Map<String, Serializable> m = transition(name, target, condition);
        m.put(GraphNode.PROP_TRANS_CHAIN, chainId);
        return m;
    }

    protected Map<String, Serializable> transition(String name, String target)
            throws ClientException {
        return transition(name, target, "true");
    }

    protected void setTransitions(DocumentModel node,
            Map<String, Serializable>... transitions) throws ClientException {
        node.setPropertyValue(GraphNode.PROP_TRANSITIONS,
                (Serializable) Arrays.asList(transitions));
    }

    protected Map<String, Serializable> button(String name, String label,
            String filter) {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_BTN_NAME, name);
        m.put(GraphNode.PROP_BTN_LABEL, label);
        m.put(GraphNode.PROP_BTN_FILTER, filter);
        return m;
    }

    protected void setButtons(DocumentModel node,
            Map<String, Serializable>... buttons) throws ClientException {
        node.setPropertyValue(GraphNode.PROP_TASK_BUTTONS,
                (Serializable) Arrays.asList(buttons));
    }

    protected DocumentRoute instantiateAndRun(CoreSession session)
            throws ClientException {
        return instantiateAndRun(session, null);
    }

    protected DocumentRoute instantiateAndRun(CoreSession session,
            Map<String, Serializable> map) throws ClientException {
        DocumentRoutingService routing = Framework.getLocalService(DocumentRoutingService.class);
        // route model
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        // draft -> validated
        if (!route.isValidated()) {
            route = routing.validateRouteModel(route, session);
        }
        // create instance and start
        String id = routing.createNewInstance(route.getDocument().getName(),
                Collections.singletonList(doc.getId()), map, session, true);
        return session.getDocument(new IdRef(id)).getAdapter(
                DocumentRoute.class);
    }

    protected DocumentRoute instantiateAndRun(CoreSession session,
            List<String> docIds, Map<String, Serializable> map)
            throws ClientException {
        DocumentRoute route = validate(routeDoc, session);
        // create instance and start
        String id = Framework.getLocalService(DocumentRoutingService.class).createNewInstance(
                route.getDocument().getName(), docIds, map, session, true);
        return session.getDocument(new IdRef(id)).getAdapter(
                DocumentRoute.class);
    }

    protected DocumentRoute validate(DocumentModel routeDoc, CoreSession session)
            throws DocumentRouteNotLockedException, ClientException {
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        // draft -> validated
        if (!route.isValidated()) {
            route = Framework.getLocalService(DocumentRoutingService.class).validateRouteModel(
                    route, session);
        }
        return route;
    }
}