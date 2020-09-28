/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.4
 */
@RunWith(FeaturesRunner.class)
@Features({ WorkflowFeature.class, WebEngineFeatureCore.class })
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-document-routing-activation-filters.xml")
public class WorkflowActivationFilterTest extends AbstractGraphRouteTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService routing;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
    }

    public void setRoute(String routeName, String activationFiltername) {
        routeDoc = createRoute(routeName, session);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        session.saveDocument(node1);
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.setPropertyValue(GraphRoute.PROP_AVAILABILITY_FILTER, activationFiltername);
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        validate(routeDoc, session);
    }

    @Test
    public void testWorkflowWithoutFilterCanBeStarted() {
        setRoute("testWorkflowWithoutFilterCanBeStarted", null);
        assertTrue(routing.canCreateInstance(session, List.of(doc.getId()), routeDoc.getName()));
    }

    @Test(expected = NuxeoException.class)
    public void testInvalidWorkflowName() {
        routing.canCreateInstance(session, List.of(doc.getId()), "WorkflowThatDoesNotExist");
    }

    @Test
    public void testWorkflowIsRunnable() {
        setRoute("testWorkflowIsRunnable", "test_wf_pass");
        List<DocumentRoute> runnables = routing.getRunnableWorkflows(session, List.of(doc.getId()));
        assertEquals(1, runnables.size());
    }

    @Test
    public void testWorkflowCanBeStarted() {
        setRoute("testWorkflowCanBeStarted", "test_wf_pass");
        assertTrue(routing.canCreateInstance(session, List.of(doc.getId()), routeDoc.getName()));
    }

    @Test
    public void testWorkflowIsNotRunnable() {
        setRoute("testWorkflowIsNotRunnable", "test_wf_fail");
        List<DocumentRoute> runnables = routing.getRunnableWorkflows(session, List.of(doc.getId()));
        assertEquals(0, runnables.size());
    }

    @Test
    public void testWorkflowCannotBeStarted() {
        setRoute("testWorkflowCannotBeStarted", "test_wf_fail");
        assertFalse(routing.canCreateInstance(session, List.of(doc.getId()), routeDoc.getName()));
    }
}
