/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.task.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.providers.UserTaskPageProvider;
import org.nuxeo.ecm.platform.task.test.TaskUTConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.4.2
 */
public class TaskPageProvidersTest extends SQLRepositoryTestCase {

    protected TaskService taskService;

    protected PageProviderService ppService;

    protected UserManager userManager;

    protected NuxeoPrincipal administrator;

    protected DocumentModel document;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.task.core.test",
                "OSGI-INF/pageproviders-contrib.xml");

        deployBundle(TaskUTConstants.CORE_BUNDLE_NAME);
        deployBundle(TaskUTConstants.TESTING_BUNDLE_NAME);

        taskService = Framework.getService(TaskService.class);
        ppService = Framework.getService(PageProviderService.class);

        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);

        administrator = userManager.getPrincipal(SecurityConstants.ADMINISTRATOR);
        assertNotNull(administrator);

        openSession();
        document = getDocument();
        assertNotNull(document);

        // create isolated task
        List<String> actors = new ArrayList<String>();
        actors.add(NuxeoPrincipal.PREFIX + administrator.getName());
        actors.add(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6);
        // create one task
        taskService.createTask(session, administrator, document,
                "Test Task Name", actors, false, "test directive",
                "test comment", calendar.getTime(), null, null);
        // create another task to check pagination
        taskService.createTask(session, administrator, document,
                "Test Task Name 2", actors, false, "test directive",
                "test comment", calendar.getTime(), null, null);
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    public void testTaskPageProvider() throws Exception {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(UserTaskPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        PageProvider<DashBoardItem> taskProvider = (PageProvider<DashBoardItem>) ppService.getPageProvider(
                "current_user_tasks", null, null, null, properties,
                (Object[]) null);
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

    protected DocumentModel getDocument() throws Exception {
        DocumentModel model = session.createDocumentModel(
                session.getRootDocument().getPathAsString(), "1", "File");
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);

        session.saveDocument(doc);
        session.save();
        return doc;
    }

}
