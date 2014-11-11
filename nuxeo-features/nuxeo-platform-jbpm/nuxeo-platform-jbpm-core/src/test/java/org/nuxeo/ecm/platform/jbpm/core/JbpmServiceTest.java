/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.platform.jbpm.core.service.JbpmServiceImpl;
import org.nuxeo.ecm.platform.jbpm.test.JbpmUTConstants;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 */
public class JbpmServiceTest extends RepositoryOSGITestCase {

    private JbpmService service;

    private UserManager userManager;

    private NuxeoPrincipal administrator;

    private NuxeoPrincipal user1;

    @Override
    public void setUp() throws Exception {
        // clean up previous test.
        JbpmServiceImpl.contexts.set(null);
        super.setUp();

        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployContrib("org.nuxeo.ecm.platform.jbpm.core.test",
                "OSGI-INF/jbpmService-contrib.xml");

        deployBundle(JbpmUTConstants.CORE_BUNDLE_NAME);
        deployBundle(JbpmUTConstants.TESTING_BUNDLE_NAME);

        service = Framework.getService(JbpmService.class);
        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);

        administrator = userManager.getPrincipal(SecurityConstants.ADMINISTRATOR);
        assertNotNull(administrator);

        user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
        JbpmServiceImpl.contexts.set(null);
    }

    public void testTypeFilter() {
        Map<String, List<String>> typeFilters = service.getTypeFilterConfiguration();
        assertEquals(2, typeFilters.size());
        assertEquals(2, typeFilters.get("Note").size());
        assertTrue(typeFilters.get("Note").contains("review_parallel"));
        assertTrue(typeFilters.get("Note").contains("review_approbation"));
    }

    public void testProcessInstanceLifecycle() throws Exception {
        List<String> administratorList = new ArrayList<String>();
        administratorList.add(NuxeoPrincipal.PREFIX + administrator.getName());
        for (String group : administrator.getAllGroups()) {
            administratorList.add(NuxeoGroup.PREFIX + group);
        }
        DocumentModel dm = getDocument();
        assertNotNull(dm);

        // list process definition
        List<ProcessDefinition> pds = service.getProcessDefinitions(
                administrator, dm, null);
        assertNotNull(pds);
        assertEquals(2, pds.size());

        List<VirtualTaskInstance> participants = new ArrayList<VirtualTaskInstance>();
        participants.add(new VirtualTaskInstance("bob", "dobob", "yobob", null));
        participants.add(new VirtualTaskInstance("trudy", "dotrudy", "yotrudy",
                null));
        // create process instance
        ProcessInstance pd = service.createProcessInstance(administrator,
                "review_parallel", dm, Collections.singletonMap("participants",
                        (Serializable) participants), null);
        Long pdId = Long.valueOf(pd.getId());
        assertNotNull(pd);
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.initiator.name()),
                NuxeoPrincipal.PREFIX + SecurityConstants.ADMINISTRATOR);
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.documentId.name()), dm.getId());
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.documentRepositoryName.name()),
                dm.getRepositoryName());

        // get process instance
        List<ProcessInstance> pis1 = service.getCurrentProcessInstances(
                administrator, null);
        assertEquals(1, pis1.size());
        List<ProcessInstance> pis2 = service.getCurrentProcessInstances(
                administratorList, null);
        assertEquals(1, pis2.size());

        // get tasks
        List<TaskInstance> tasks = service.getTaskInstances(dm, administrator,
                null);
        List<TaskInstance> tasks2 = service.getTaskInstances(dm,
                administratorList, null);
        assertEquals(tasks2.size(), tasks.size());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        tasks = service.getCurrentTaskInstances(administrator, null);
        assertEquals(1, tasks.size());
        final long cancelledTi = tasks.get(0).getId();
        service.executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                TaskInstance ti = context.getTaskInstance(cancelledTi);
                ti.cancel();
                return null;
            }
        });
        tasks = service.getCurrentTaskInstances(administrator, null);
        assertEquals(0, tasks.size());

        service.deleteProcessInstance(administrator, Long.valueOf(pd.getId()));
        pd = service.getProcessInstance(pdId);
        assertNull(pd);

        List<TaskInstance> tis = service.getCurrentTaskInstances(administrator,
                null);
        assertTrue(tis.isEmpty());
    }

    public void testMultipleTaskPerDocument() throws Exception {
        DocumentModel dm = getDocument();
        assertNotNull(dm);

        // list process definition
        List<ProcessDefinition> pds = service.getProcessDefinitions(
                administrator, dm, null);
        assertNotNull(pds);
        assertEquals(2, pds.size());

        List<VirtualTaskInstance> participants = new ArrayList<VirtualTaskInstance>();
        String prefixedUser1 = NuxeoPrincipal.PREFIX + user1.getName();
        participants.add(new VirtualTaskInstance(prefixedUser1, "dobob1",
                "yobob1", null));
        participants.add(new VirtualTaskInstance(prefixedUser1, "dobob2",
                "yobob1", null));

        // create process instance
        service.createProcessInstance(administrator, "review_parallel", dm,
                Collections.singletonMap("participants",
                        (Serializable) participants), null);
        List<TaskInstance> tasks = service.getTaskInstances(dm, administrator,
                null);
        service.endTask(Long.valueOf(tasks.get(0).getId()), null, null, null,
                null, null);
        // tasks.get(0).end();
        tasks = service.getTaskInstances(dm, administrator, null);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());

        tasks = service.getTaskInstances(dm, user1, null);
        assertEquals(2, tasks.size());
        List<String> transitions = service.getAvailableTransitions(tasks.get(0).getId(), user1);
        for(String t : transitions) {
            assertNotNull(t);
        }
        assertNotNull(transitions);
    }

    public void testTaskManagement() throws Exception {
        DocumentModel dm = getDocument();
        TaskInstance ti = new TaskInstance();
        ti.setName("publication task");
        ti.setActorId("bob");
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(JbpmService.VariableName.documentId.name(), dm.getId());
        variables.put(JbpmService.VariableName.documentRepositoryName.name(),
                "demo");
        ti.addVariables(variables);
        service.saveTaskInstances(Collections.singletonList(ti));
        List<TaskInstance> lists = service.getTaskInstances(dm,
                new NuxeoPrincipalImpl("bob"), null);
        assertNotNull(lists);
    }

    public void testProcessInstancePersistence() throws Exception {
        DocumentModel dm = getDocument();
        // create process instance
        ProcessInstance pi = service.createProcessInstance(administrator,
                "review_parallel", dm, null, null);
        Long pid = Long.valueOf(pi.getId());
        // edit
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("foo", "bar");
        pi.getContextInstance().addVariables(variables);
        service.persistProcessInstance(pi);

        ProcessInstance editedPi = service.getProcessInstance(pid);
        assertEquals("bar", editedPi.getContextInstance().getVariable("foo"));
    }

    protected DocumentModel getDocument() throws Exception {
        CoreSession session = getCoreSession();
        DocumentModel model = session.createDocumentModel(
                session.getRootDocument().getPathAsString(), "1", "File");
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);

        session.saveDocument(doc);
        session.save();
        return doc;
    }

}
