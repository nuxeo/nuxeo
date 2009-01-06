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

import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
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
        super.setUp();
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.core.jbpm");
        deployBundle("org.nuxeo.ecm.platform.core.jbpm.test");
        service = Framework.getService(JbpmService.class);
        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);
        userManager.getAvailablePrincipals();
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
        assertEquals(3, pds.size());
        // create process instance
        ProcessInstance pd = service.createProcessInstance(administrator,
                "parallel-review", dm, null, null);
        assertNotNull(pd);
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.documentId.name()), dm.getId());
        assertEquals(pd.getContextInstance().getVariable(
                JbpmService.VariableName.documentRepositoryName.name()),
                dm.getRepositoryName());
        // get tasks
        List<TaskInstance> tasks = service.getTaskInstances(dm, administrator);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
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
}
