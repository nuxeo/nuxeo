/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 *
 */

package org.nuxeo.ecm.platform.jbpm.core;

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskListService;
import org.nuxeo.ecm.platform.jbpm.TaskList;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.platform.jbpm.core.service.JbpmServiceImpl;
import org.nuxeo.ecm.platform.jbpm.test.JbpmUTConstants;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

public class JbpmTaskListServiceTest extends SQLRepositoryTestCase {

    public static String userWorkspacePath = "/default-domain/UserWorkspaces/Administrator";

    @Override
    public void setUp() throws Exception {

        // clean up previous test.
        JbpmServiceImpl.contexts.set(null);

        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.api");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.types");
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core",
                "OSGI-INF/userworkspace-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core",
                "OSGI-INF/userWorkspaceImpl.xml");

        deployContrib("org.nuxeo.ecm.platform.jbpm.core.test",
                "OSGI-INF/jbpmService-contrib.xml");

        deployBundle(JbpmUTConstants.CORE_BUNDLE_NAME);
        deployBundle(JbpmUTConstants.TESTING_BUNDLE_NAME);

        fireFrameworkStarted();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
        JbpmServiceImpl.contexts.set(null);
    }

    public void testUserWorkspaceService() throws Exception {
        DocumentModel userWorkspace = getUserWorkspace(session);
        assertNotNull(userWorkspace);
    }

    public void testAdapter() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "list1",
                "TaskList");
        doc = session.createDocument(doc);
        assertNotNull(doc);

        TaskList list = doc.getAdapter(TaskList.class);
        assertNotNull(list);

        VirtualTaskInstance task = new VirtualTaskInstance();
        Date date = new GregorianCalendar().getTime();
        task.setActors(Arrays.asList("user1", "user2"));
        task.setDirective("directive1");
        task.setComment("comment1");
        task.getParameters().put("right", "Read");
        task.setDueDate(date);

        list.addTask(task);

        assertEquals(list.getTasks().get(0).getActors(), Arrays.asList("user1",
                "user2"));
        assertEquals("directive1", list.getTasks().get(0).getDirective());
        assertEquals("comment1", list.getTasks().get(0).getComment());
        assertEquals("Read", list.getTasks().get(0).getParameters().get("right"));
        assertEquals(list.getTasks().get(0).getDueDate(), date);
    }

    public void testAdapterFail() throws ClientException {
        try {
            session.getRootDocument().getAdapter(TaskList.class);
            fail("Should throw exception");
        } catch (Exception e) {
        }
    }

    public void testTaskListService() throws Exception {

        // Retrieve the service
        JbpmTaskListService service = Framework.getService(JbpmTaskListService.class);
        assertNotNull(service);

        // Create a task list
        TaskList list = service.createTaskList(session, "List");
        assertNotNull(list);
        assertEquals("List", list.getName());

        // Add a task
        VirtualTaskInstance task = new VirtualTaskInstance();
        Date date = new GregorianCalendar().getTime();
        task.setActors(Arrays.asList("user1", "user2"));
        task.setDirective("directive1");
        task.setComment("comment1");
        task.getParameters().put("right", "Read");
        task.setDueDate(date);

        list.addTask(task);

        assertEquals(1, list.getTasks().size());
        assertEquals(list.getTasks().get(0).getActors(), Arrays.asList("user1",
                "user2"));
        assertEquals("directive1", list.getTasks().get(0).getDirective());
        assertEquals("comment1", list.getTasks().get(0).getComment());
        assertEquals(list.getTasks().get(0).getDueDate(), date);
        assertEquals("Read", list.getTasks().get(0).getParameters().get("right"));

        // Save the list
        service.saveTaskList(session, list);

        closeSession();
        openSession();

        // Try to load unknown list
        TaskList listFake = service.getTaskList(session, "ListFake");
        assertNull(listFake);
        assertEquals(1, service.getTaskLists(session).size());

        // Load the list
        TaskList list2 = service.getTaskList(session, list.getUUID());
        assertNotNull(list2);

        assertEquals(list.getTasks().size(), list2.getTasks().size());

        assertEquals(list.getTasks().get(0).getActors(), Arrays.asList("user1",
                "user2"));
        assertEquals("directive1", list.getTasks().get(0).getDirective());
        assertEquals("comment1", list.getTasks().get(0).getComment());
        assertEquals(list.getTasks().get(0).getDueDate(), date);
        assertEquals("Read", list.getTasks().get(0).getParameters().get("right"));

        // Try to delete an unknown it
        service.deleteTaskList(session, "ListFake");

        // Delete it
        service.deleteTaskList(session, list.getUUID());

        closeSession();
        openSession();

        // Check it is deleted
        TaskList list3 = service.getTaskList(session, list.getUUID());
        assertNull(list3);
    }

    private static DocumentModel getUserWorkspace(CoreSession session)
            throws ClientException {
        UserWorkspaceService uws = Framework.getLocalService(UserWorkspaceService.class);
        return uws.getCurrentUserPersonalWorkspace(session, null);
    }

}
