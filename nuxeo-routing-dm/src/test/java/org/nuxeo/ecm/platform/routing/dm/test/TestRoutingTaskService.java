/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.test;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.RoutingTaskService;
import org.nuxeo.ecm.platform.routing.dm.adapter.RoutingTask;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author ldoguin
 * @deprecated since 5.9.2 - Use only routes of type 'graph'. This class tests {@link RoutingTaskService} deprecated
 *             since 5.6
 */
@Deprecated
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.platform.task.core", //
        "org.nuxeo.ecm.platform.routing.core", //
        "org.nuxeo.ecm.platform.test", //
        "org.nuxeo.ecm.platform.task.core", //
        "org.nuxeo.ecm.platform.task.testing", //
        "org.nuxeo.ecm.platform.routing.dm", //
        "org.nuxeo.ecm.platform.audit:OSGI-INF/core-type-contrib.xml", //
})
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml")
public class TestRoutingTaskService {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TaskService taskService;

    @Inject
    protected DocumentRoutingService routing;

    protected NuxeoPrincipal administrator;

    protected NuxeoPrincipal user1;

    protected NuxeoPrincipal user2;

    protected NuxeoPrincipal user3;

    protected NuxeoPrincipal user4;

    protected DocumentModel targetDoc;

    @Before
    public void setUp() throws Exception {
        administrator = userManager.getPrincipal(SecurityConstants.ADMINISTRATOR);
        assertNotNull(administrator);

        user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        user3 = userManager.getPrincipal("myuser3");
        assertNotNull(user3);

        user4 = userManager.getPrincipal("myuser4");
        assertNotNull(user4);

        targetDoc = session.createDocumentModel("/", "targetDocument", "File");
        targetDoc = session.createDocument(targetDoc);
    }

    @Test
    public void testService() throws Exception {
        List<String> actorIds = new ArrayList<String>();
        List<Task> tasks = taskService.createTask(session, administrator, targetDoc, "MyRoutingTask", actorIds, false,
                null, null, null, null, "/");
        routing.makeRoutingTasks(session, tasks);
        session.save();
        DocumentModel taskDoc = session.getDocument(new PathRef("/MyRoutingTask"));
        RoutingTask routingTask = taskDoc.getAdapter(RoutingTask.class);
        assertNotNull(routingTask);
    }

    @Test
    @Deprecated
    public void testTaskStep() throws Exception {
        DocumentModel taskStep = session.createDocumentModel("/", "simpleTask", "SimpleTask");
        assertNotNull(taskStep);
        taskStep = session.createDocument(taskStep);
        assertNotNull(taskStep);
    }

}
