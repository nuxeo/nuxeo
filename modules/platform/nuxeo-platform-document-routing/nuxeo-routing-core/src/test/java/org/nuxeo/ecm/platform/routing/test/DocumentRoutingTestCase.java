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

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
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
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
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
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
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
    protected DocumentRoutingEngineService engineService;

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

    public DocumentModel createDocumentRouteModel(CoreSession session, String name, String path) {
        DocumentModel route = createDocumentModel(session, name, DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE,
                path);
        createDocumentModel(session, "step1", DocumentRoutingConstants.STEP_DOCUMENT_TYPE, route.getPathAsString());
        createDocumentModel(session, "step2", DocumentRoutingConstants.STEP_DOCUMENT_TYPE, route.getPathAsString());
        DocumentModel parallelFolder1 = createDocumentModel(session, "parallel1",
                DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE, route.getPathAsString());
        parallelFolder1.setPropertyValue(DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME,
                DocumentRoutingConstants.ExecutionTypeValues.parallel.name());
        session.saveDocument(parallelFolder1);
        createDocumentModel(session, "step31", DocumentRoutingConstants.STEP_DOCUMENT_TYPE,
                parallelFolder1.getPathAsString());
        createDocumentModel(session, "step32", DocumentRoutingConstants.STEP_DOCUMENT_TYPE,
                parallelFolder1.getPathAsString());
        session.save();
        return route;
    }

    public DocumentModel createDocumentRouteModelWithConditionalFolder(CoreSession session, String name, String path)
            {
        DocumentModel route = createDocumentModel(session, name, DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE,
                path);
        createDocumentModel(session, "step1", DocumentRoutingConstants.STEP_DOCUMENT_TYPE, route.getPathAsString());
        DocumentModel condFolder = createDocumentModel(session, "conditionalStep2",
                DocumentRoutingConstants.CONDITIONAL_STEP_DOCUMENT_TYPE, route.getPathAsString());
        // create a step into each one of the 2 branches
        createDocumentModel(session, "executeIfOption1", DocumentRoutingConstants.STEP_DOCUMENT_TYPE,
                condFolder.getPathAsString() + "/option1");
        createDocumentModel(session, "executeIfOption2", DocumentRoutingConstants.STEP_DOCUMENT_TYPE,
                condFolder.getPathAsString() + "/option2");
        createDocumentModel(session, "step3", DocumentRoutingConstants.STEP_DOCUMENT_TYPE, route.getPathAsString());
        session.save();
        return route;
    }

    public DocumentModel createDocumentModel(CoreSession session, String name, String type, String path)
            {
        DocumentModel route1 = session.createDocumentModel(path, name, type);
        route1.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, name);
        return session.createDocument(route1);
    }

    public DocumentRoute createDocumentRoute(CoreSession session, String name) {
        DocumentModel model = createDocumentRouteModel(session, name, WORKSPACES_PATH);
        return model.getAdapter(DocumentRoute.class);
    }

    public DocumentRoute createDocumentRouteWithConditionalFolder(CoreSession session, String name)
            {
        DocumentModel model = createDocumentRouteModelWithConditionalFolder(session, name, WORKSPACES_PATH);
        return model.getAdapter(DocumentRoute.class);
    }

    protected DocumentModel createTestDocument(String name, CoreSession session) {
        return createDocumentModel(session, name, "Note", WORKSPACES_PATH);
    }

}
