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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues.graph;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.platform.routing.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-sql-directories-contrib.xml")
public class GraphRouteTest {

    protected static final String TYPE_ROUTE_NODE = "RouteNode";

    protected static final String RNODE_START = "rnode:start";

    protected static final String RNODE_STOP = "rnode:stop";

    @Inject
    CoreSession session;

    protected DocumentRoutingService routing;

    @Before
    public void setUp() {
        routing = Framework.getLocalService(DocumentRoutingService.class);
        assertNotNull(routing);
    }

    protected DocumentModel createRoute(String name) throws ClientException,
            PropertyException {
        DocumentModel route = session.createDocumentModel("/", name,
                DOCUMENT_ROUTE_DOCUMENT_TYPE);
        route.setPropertyValue(EXECUTION_TYPE_PROPERTY_NAME, graph.name());
        return session.createDocument(route);
    }

    protected DocumentModel createNode(DocumentModel route, String name)
            throws ClientException, PropertyException {
        DocumentModel node = session.createDocumentModel(
                route.getPathAsString(), name, TYPE_ROUTE_NODE);
        return session.createDocument(node);
    }

    @Test
    public void testRunGraphRoute() throws ClientException {
        DocumentModel routeDoc = createRoute("route");

        DocumentModel node1Doc = createNode(routeDoc, "node1");
        node1Doc.setPropertyValue(RNODE_START, Boolean.TRUE);
        session.saveDocument(node1Doc);

        DocumentModel node2Doc = createNode(routeDoc, "node2");
        node2Doc.setPropertyValue(RNODE_STOP, Boolean.TRUE);
        session.saveDocument(node2Doc);

        session.save();

        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        route.run(session);
    }

}
