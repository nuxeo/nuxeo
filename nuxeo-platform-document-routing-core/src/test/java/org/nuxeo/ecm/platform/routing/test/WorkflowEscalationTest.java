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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class,
        AutomationFeature.class })
@Deploy({
        "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.platform.task.api", //
        "org.nuxeo.ecm.platform.task.core", //
        "org.nuxeo.ecm.platform.task.testing",
        "org.nuxeo.ecm.platform.routing.api",
        "org.nuxeo.ecm.platform.routing.core" //
})
@LocalDeploy({
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-operations-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-types-contrib.xml" })
public class WorkflowEscalationTest extends AbstractGraphRouteTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected FeaturesRunner featuresRunner;

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected DocumentRoutingService routing;

    // init userManager now for early user tables creation (cleaner debug)
    @Inject
    protected UserManager userManager;

    @Inject
    protected TaskService taskService;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected DocumentRoutingEscalationService escalationService;

    @Inject
    protected WorkManager workManager;

    @Before
    public void setUp() throws Exception {
        assertNotNull(routing);
        doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        doc = session.createDocument(doc);
        routeDoc = createRoute("myroute", session);
    }

    @Test
    public void testEscalationSingleExecution() throws Exception {
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(
                node1,
                transition("trans1", "node2",
                        "NodeVariables[\"button\"] == \"trans1\"",
                        "testchain_title1"));
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setEscalationRules(node1,
                escalationRule("rule1", "true", "testchain_title1", false));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);
        instantiateAndRun(session);

        session.save();
        TransactionHelper.commitOrRollbackTransaction();

        List<String> nodes = escalationService.queryForSuspendedNodesWithEscalation(session);
        assertEquals(1, nodes.size());
        DocumentModel nodeDoc = session.getDocument(new IdRef(nodes.get(0)));
        GraphNode node = nodeDoc.getAdapter(GraphNode.class);
        assertEquals("node1", node.getId());
        List<GraphNode.EscalationRule> rules = escalationService.computeEscalationRulesToExecute(node);
        assertEquals(1, rules.size());
        escalationService.scheduleExecution(rules.get(0), session);
        workManager.awaitCompletion("escalation", 3, TimeUnit.SECONDS);
        assertEquals(0, workManager.getQueueSize("escalation", null));

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        // fetch node doc to check that the rule is marked as executed
        nodeDoc = session.getDocument(new IdRef(nodes.get(0)));
        node = nodeDoc.getAdapter(GraphNode.class);
        assertTrue(node.getEscalationRules().get(0).isExecuted());
        // check that the rule was executed
        doc = session.getDocument(doc.getRef());
        assertEquals("title 1", doc.getTitle());

        // check that no nodes with execution rules are found
        nodes = escalationService.queryForSuspendedNodesWithEscalation(session);
        assertEquals(0, nodes.size());
    }

    @Test
    public void testEscalationMultipleExecution() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(
                node1,
                transition("trans1", "node2",
                        "NodeVariables[\"button\"] == \"trans1\"",
                        "testchain_title1"));

        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setEscalationRules(node1,
                escalationRule("rule1", "true", "testchain_title1", true),
                escalationRule("rule2", "true", "testchain_title2", false));
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);
        instantiateAndRun(session);

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        List<String> nodes = escalationService.queryForSuspendedNodesWithEscalation(session);
        assertEquals(1, nodes.size());

        // execute rule1
        DocumentModel nodeDoc = session.getDocument(new IdRef(nodes.get(0)));
        GraphNode node = nodeDoc.getAdapter(GraphNode.class);
        assertEquals("node1", node.getId());
        List<EscalationRule> rules = escalationService.computeEscalationRulesToExecute(node);
        assertEquals(2, rules.size());
        escalationService.scheduleExecution(rules.get(0), session);
        workManager.awaitCompletion("escalation", 3, TimeUnit.SECONDS);
        assertEquals(0, workManager.getQueueSize("escalation", null));
        // check that the rule was executed
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        doc = session.getDocument(doc.getRef());
        assertEquals("title 1", doc.getTitle());

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        // check that are still 2 rules, since rule1 is marked for
        // multipleExecution
        nodeDoc = session.getDocument(new IdRef(node.getDocument().getId()));
        node = nodeDoc.getAdapter(GraphNode.class);
        rules = escalationService.computeEscalationRulesToExecute(node);
        assertEquals(2, rules.size());

        // execute rule2
        escalationService.scheduleExecution(rules.get(1), session);
        workManager.awaitCompletion("escalation", 3, TimeUnit.SECONDS);
        assertEquals(0, workManager.getQueueSize("escalation", null));
        // check that the rule was executed
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        doc = session.getDocument(doc.getRef());
        assertEquals("title 2", doc.getTitle());

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        // check that only one rule is found now
        nodeDoc = session.getDocument(new IdRef(node.getDocument().getId()));
        node = nodeDoc.getAdapter(GraphNode.class);
        rules = escalationService.computeEscalationRulesToExecute(node);
        assertEquals(1, rules.size());
    }

    protected void setEscalationRules(DocumentModel node,
            Map<String, Serializable>... rules) throws ClientException {
        node.setPropertyValue(GraphNode.PROP_ESCALATION_RULES,
                (Serializable) Arrays.asList(rules));
    }

    protected Map<String, Serializable> escalationRule(String id,
            String condition, String chain, boolean multipleExecution)
            throws ClientException {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_ESCALATION_RULE_ID, id);
        m.put(GraphNode.PROP_ESCALATION_RULE_CONDITION, condition);
        m.put(GraphNode.PROP_ESCALATION_RULE_CHAIN, chain);
        m.put(GraphNode.PROP_ESCALATION_RULE_MULTIPLE_EXECUTION,
                multipleExecution);
        return m;
    }

}
