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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.core.service.JbpmServiceImpl;
import org.nuxeo.ecm.platform.jbpm.test.JbpmTestConstants;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class JbpmServiceTest extends RepositoryOSGITestCase {

    private JbpmService service;

    private UserManager userManager;

    private NuxeoPrincipal administrator;

    private NuxeoPrincipal user1;

    @Override
    protected void setUp() throws Exception {
        // clean up previous test.
        JbpmServiceImpl.contexts.set(null);
        super.setUp();
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");

        deployBundle("org.nuxeo.ecm.platform.core.jbpm");
        deployBundle(JbpmTestConstants.TESTING_BUNDLE_NAME);

        service = Framework.getService(JbpmService.class);
        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);
        administrator = userManager.getPrincipal("Administrator");
        assertNotNull(administrator);
        user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);
    }

    public void testProcessInstanceLifecycle() throws Exception {
        DocumentModel dm = getDocument();
        assertNotNull(dm);
        // list process definition
        List<ProcessDefinition> pds = service.getProcessDefinitions(
                administrator, dm);
        assertNotNull(pds);
        assertEquals(2, pds.size());
        // create process instance
        ProcessInstance pd = service.createProcessInstance(administrator,
                "review_parallel", dm, null, null);
        assertNotNull(pd);
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.documentId.name()), dm.getId());
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.documentRepositoryName.name()),
                dm.getRepositoryName());
        // get tasks
        List<TaskInstance> tasks = service.getTaskInstances(dm, administrator,
                null);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
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

    protected DocumentModel getDocument() throws Exception {
        openRepository();
        CoreSession session = getCoreSession();
        DocumentModel model = session.createDocumentModel(
                session.getRootDocument().getPathAsString(), "1", "File");
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);
        session.saveDocument(doc);
        session.save();
        return doc;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        JbpmServiceImpl.contexts.set(null);
    }
}
