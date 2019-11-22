/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ WorkflowFeature.class, WebEngineFeatureCore.class })
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-document-routing-scripting-contrib.xml")
public class WorkflowAutomationScripting extends AbstractGraphRouteTest {

    @Inject
    CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        doc = session.createDocument(doc);
        routeDoc = createRoute("myroute", session);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        session.saveDocument(node1);
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        validate(routeDoc, session);
    }

    @Test
    public void testStartWorkflowOperation() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            automationService.run(ctx, "Scripting.starytMyRoute");
            DocumentModel routeInstance = session.getDocument(new IdRef((String) ctx.get("WorkflowId")));
            GraphRoute graph = routeInstance.getAdapter(GraphRoute.class);
            Map<String, Serializable> vars = graph.getVariables();
            assertNotNull(vars.get("datefield"));
            assertEquals(vars.get("stringfield"), "test");
            assertNotNull(vars.get("myassignees"));
            assertTrue(vars.get("myassignees") instanceof String[]);
            String[] assignees = (String[]) vars.get("myassignees");
            assertArrayEquals(new String[] { "x", "y" }, assignees);
        }
    }

}
