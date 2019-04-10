/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(WorkflowFeature.class)
@Ignore
public class AbstractGraphRouteTest {

    // a doc, associated to the route
    protected DocumentModel doc;

    // the route model we'll use
    protected DocumentModel routeDoc;

    protected static final String TYPE_ROUTE_NODE = "RouteNode";

    protected DocumentModel createRoute(String name, CoreSession session) throws PropertyException {
        DocumentModel route = session.createDocumentModel("/", name, DOCUMENT_ROUTE_DOCUMENT_TYPE);
        route.setPropertyValue(EXECUTION_TYPE_PROPERTY_NAME, graph.name());
        route.setPropertyValue("dc:title", name);
        route.setPropertyValue(ATTACHED_DOCUMENTS_PROPERTY_NAME, (Serializable) Collections.singletonList(doc.getId()));
        return session.createDocument(route);
    }

    protected DocumentModel createNode(DocumentModel route, String name, CoreSession session) throws
            PropertyException {
        DocumentModel node = session.createDocumentModel(route.getPathAsString(), name, TYPE_ROUTE_NODE);
        node.setPropertyValue(GraphNode.PROP_NODE_ID, name);
        return session.createDocument(node);
    }

    protected Map<String, Serializable> transition(String name, String target, String condition) {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_TRANS_NAME, name);
        m.put(GraphNode.PROP_TRANS_TARGET, target);
        m.put(GraphNode.PROP_TRANS_CONDITION, condition);
        return m;
    }

    protected Map<String, Serializable> transition(String name, String target, String condition, String chainId)
            {
        Map<String, Serializable> m = transition(name, target, condition);
        m.put(GraphNode.PROP_TRANS_CHAIN, chainId);
        return m;
    }

    protected Map<String, Serializable> transition(String name, String target) {
        return transition(name, target, "true");
    }

    protected void setTransitions(DocumentModel node, Map<String, Serializable>... transitions) {
        node.setPropertyValue(GraphNode.PROP_TRANSITIONS, (Serializable) Arrays.asList(transitions));
    }

    protected Map<String, Serializable> button(String name, String label, String filter) {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_BTN_NAME, name);
        m.put(GraphNode.PROP_BTN_LABEL, label);
        m.put(GraphNode.PROP_BTN_FILTER, filter);
        return m;
    }

    protected void setButtons(DocumentModel node, Map<String, Serializable>... buttons) {
        node.setPropertyValue(GraphNode.PROP_TASK_BUTTONS, (Serializable) Arrays.asList(buttons));
    }

    protected DocumentRoute instantiateAndRun(CoreSession session) {
        return instantiateAndRun(session, null);
    }

    protected DocumentRoute instantiateAndRun(CoreSession session, Map<String, Serializable> map)
            {
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        // route model
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        // draft -> validated
        if (!route.isValidated()) {
            route = routing.validateRouteModel(route, session);
        }
        session.save();
        // create instance and start
        String id = routing.createNewInstance(route.getDocument().getName(), Collections.singletonList(doc.getId()),
                map, session, true);
        return session.getDocument(new IdRef(id)).getAdapter(DocumentRoute.class);
    }

    protected DocumentRoute instantiateAndRun(CoreSession session, List<String> docIds, Map<String, Serializable> map)
            {
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        DocumentRoute route = validate(routeDoc, session);
        // create instance and start
        String id = Framework.getService(DocumentRoutingService.class).createNewInstance(
                route.getDocument().getName(), docIds, map, session, true);
        return session.getDocument(new IdRef(id)).getAdapter(DocumentRoute.class);
    }

    protected DocumentRoute validate(DocumentModel routeDoc, CoreSession session)
            throws DocumentRouteNotLockedException {
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        // draft -> validated
        if (!route.isValidated()) {
            route = Framework.getService(DocumentRoutingService.class).validateRouteModel(route, session);
        }
        return route;
    }
}
