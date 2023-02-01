/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ROUTE_NODE_DOCUMENT_TYPE;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author arussel
 */
@Ignore
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.task.api")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.routing.core")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core.test:OSGI-INF/test-sql-directories-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core.test:OSGI-INF/test-graph-types-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.audit:OSGI-INF/core-type-contrib.xml")
public class DocumentRoutingTestCase {

    public static final String ROOT_PATH = "/";

    public static final String WORKSPACES_PATH = "/default-domain/workspaces";

    public static final String TEST_BUNDLE = "org.nuxeo.ecm.platform.routing.core.test";

    public static final String ROUTE1 = "route1";

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService service;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected ContentTemplateService ctService;

    @Before
    public void setUp() throws Exception {
        CounterListener.resetCouner();
        service.invalidateRouteModelsCache();

        workManager.init();

        DocumentModel root = session.getRootDocument();
        ctService.executeFactoryForType(root);

        DocumentModel workspaces = session.getDocument(new PathRef(WORKSPACES_PATH));
        assertNotNull(workspaces);
        ACP acp = workspaces.getACP();
        ACL acl = acp.getOrCreateACL("local");
        acl.add(new ACE("bob", SecurityConstants.READ_WRITE, true));
        session.setACP(workspaces.getRef(), acp, true);
        session.saveDocument(workspaces);
        session.save();
    }

    public DocumentRoute createDocumentRoute(CoreSession session, String name) {
        DocumentModel model = createDocumentRouteModel(session, name, WORKSPACES_PATH);
        return model.getAdapter(DocumentRoute.class);
    }

    public DocumentModel createDocumentRouteModel(CoreSession session, String name, String path) {
        DocumentModel route = createDocumentModel(session, name, DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE,
                path);
        var step1Node = createNode(route, "step1", session);
        step1Node.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(step1Node, transition("transToStep2", "step2"));
        session.saveDocument(step1Node);

        var step2Node = createNode(route, "step2", session);
        setTransitions(step2Node, transition("transToParallel1", "parallel1"));
        session.saveDocument(step2Node);

        DocumentModel parallel1Node = createNode(route, "parallel1", session);
        setTransitions(parallel1Node, transition("transToParallel1", "step31"),
                transition("transToParallel2", "step32"));
        session.saveDocument(parallel1Node);

        var step31 = createNode(route, "step31", session);
        setTransitions(step31, transition("transToMergeNode", "mergeNode"));
        session.saveDocument(step31);

        var step32 = createNode(route, "step32", session);
        setTransitions(step32, transition("transToMergeNode", "mergeNode"));
        session.saveDocument(step32);

        var mergeNode = createNode(route, "mergeNode", session);
        mergeNode.setPropertyValue(GraphNode.PROP_MERGE, "all");
        mergeNode.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        session.saveDocument(mergeNode);

        session.save();
        return route;
    }

    protected DocumentModel createNode(DocumentModel route, String name, CoreSession session) throws PropertyException {
        DocumentModel node = session.createDocumentModel(route.getPathAsString(), name, ROUTE_NODE_DOCUMENT_TYPE);
        node.setPropertyValue(GraphNode.PROP_NODE_ID, name);
        return session.createDocument(node);
    }

    @SafeVarargs
    protected final void setTransitions(DocumentModel node, Map<String, Serializable>... transitions) {
        node.setPropertyValue(GraphNode.PROP_TRANSITIONS, (Serializable) List.of(transitions));
    }

    protected Map<String, Serializable> transition(String name, String target) {
        return transition(name, target, "true");
    }

    protected Map<String, Serializable> transition(String name, String target, String condition) {
        Map<String, Serializable> m = new HashMap<>();
        m.put(GraphNode.PROP_TRANS_NAME, name);
        m.put(GraphNode.PROP_TRANS_TARGET, target);
        m.put(GraphNode.PROP_TRANS_CONDITION, condition);
        return m;
    }

    public DocumentModel createDocumentModel(CoreSession session, String name, String type, String path) {
        DocumentModel route1 = session.createDocumentModel(path, name, type);
        route1.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, name);
        return session.createDocument(route1);
    }

    protected DocumentModel createTestDocument(String name, CoreSession session) {
        return createDocumentModel(session, name, "Note", WORKSPACES_PATH);
    }

}
