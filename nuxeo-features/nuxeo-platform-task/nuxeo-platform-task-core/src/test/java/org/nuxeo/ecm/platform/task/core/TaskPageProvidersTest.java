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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.task.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.providers.UserTaskPageProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.4.2
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.platform.task.core", //
        "org.nuxeo.ecm.platform.task.testing", //
})
@Deploy({ "org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml", //
        "org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml", //
        "org.nuxeo.ecm.platform.task.core.test:OSGI-INF/pageproviders-contrib.xml", //
})
public class TaskPageProvidersTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected TaskService taskService;

    @Inject
    protected PageProviderService ppService;

    @Inject
    protected UserManager userManager;

    protected NuxeoPrincipal administrator;

    protected DocumentModel document;

    @Before
    public void setUp() throws Exception {
        administrator = userManager.getPrincipal(SecurityConstants.ADMINISTRATOR);
        document = getDocument();

        // create isolated task
        List<String> actors = new ArrayList<>();
        actors.add(NuxeoPrincipal.PREFIX + administrator.getName());
        actors.add(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, Calendar.JULY, 6);
        // create one task
        taskService.createTask(session, administrator, document, "Test Task Name", actors, false, "test directive",
                "test comment", calendar.getTime(), null, null);
        // create another task to check pagination
        taskService.createTask(session, administrator, document, "Test Task Name 2", actors, false, "test directive",
                "test comment", calendar.getTime(), null, null);
        // NXP-18868 create task without document (task from workflow without document)
        taskService.createTaskForProcess(session, administrator, Collections.emptyList(), null, "Test Task Name 3",
                "Task1a34", "a6dd157e-143d-4e03-a3cf-d33482c8de36", null, actors, false, "test directive",
                "test comment", calendar.getTime(), null, null, null);
        session.save();
    }

    @Test
    public void testTaskPageProvider() throws Exception {
        PageProvider<DashBoardItem> taskProvider = getPageProvider("current_user_tasks");
        List<DashBoardItem> tasks = taskProvider.getCurrentPage();
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        // check first single task
        DashBoardItem task = tasks.get(0);
        assertNotNull(task.getStartDate());
        // There is no sort order, we can not assert which one is the first
        assertTrue(task.getName(), task.getName().startsWith("Test Task Name"));
        assertEquals("test comment", task.getComment());
        assertNull(task.getDescription());
        assertEquals("test directive", task.getDirective());
        assertEquals(document.getRef(), task.getDocRef());
        assertEquals(document, task.getDocument());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getStartDate());
        assertFalse(taskProvider.isPreviousPageAvailable());
        assertTrue(taskProvider.isNextPageAvailable());
        taskProvider.nextPage();
        tasks = taskProvider.getCurrentPage();
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        // check second single task
        task = tasks.get(0);

        assertEquals("test comment", task.getComment());
        assertNull(task.getDescription());
        assertEquals("test directive", task.getDirective());
        assertEquals(document.getRef(), task.getDocRef());
        assertEquals(document, task.getDocument());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getStartDate());
        assertTrue(task.getName(), task.getName().startsWith("Test Task Name"));
        assertTrue(taskProvider.isPreviousPageAvailable());
        assertFalse(taskProvider.isNextPageAvailable());
    }

    @Test
    public void testTaskPageProviderSorting() {
        PageProvider<DashBoardItem> taskProvider = getPageProvider("current_user_tasks_sort_asc");
        List<DashBoardItem> tasks = taskProvider.getCurrentPage();
        assertNotNull(tasks);
        // NXP-18868 task without documents are not retrieved by page provider
        assertEquals(2, tasks.size());
        assertEquals("Test Task Name", tasks.get(0).getName());
        // Check task order
        taskProvider = getPageProvider("current_user_tasks_sort_desc");
        tasks = taskProvider.getCurrentPage();
        assertNotNull(tasks);
        // NXP-18868 task without documents are not retrieved by page provider
        assertEquals(2, tasks.size());
        // Check task order update
        assertEquals("Test Task Name 2", tasks.get(0).getName());
    }

    @SuppressWarnings("unchecked")
    private PageProvider<DashBoardItem> getPageProvider(String pageProviderName) {
        Map<String, Serializable> properties = Collections.singletonMap(UserTaskPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        return (PageProvider<DashBoardItem>) ppService.getPageProvider(pageProviderName, null, null, null, properties,
                (Object[]) null);

    }

    protected DocumentModel getDocument() throws Exception {
        DocumentModel model = session.createDocumentModel(session.getRootDocument().getPathAsString(), "1", "File");
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);

        session.saveDocument(doc);
        session.save();
        return doc;
    }

}
