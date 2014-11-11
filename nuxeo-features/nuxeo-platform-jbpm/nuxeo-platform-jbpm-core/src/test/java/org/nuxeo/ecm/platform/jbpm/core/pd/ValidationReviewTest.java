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
package org.nuxeo.ecm.platform.jbpm.core.pd;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.platform.jbpm.test.AbstractProcessDefinitionTest;

/**
 * @author arussel
 *
 */
public class ValidationReviewTest extends AbstractProcessDefinitionTest {

    @Override
    public String getProcessDefinitionResource() {
        return "/process/validation-review.xml";
    }

    @SuppressWarnings("unchecked")
    public void testPD() {
        JbpmContext context = null;
        try {
            context = configuration.createJbpmContext();
            context.setActorId("bob");
            assertNotNull(context);

            context.deployProcessDefinition(pd);
            ProcessInstance pi = context.newProcessInstanceForUpdate("review_approbation");
            TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
            ti.end();
            assertNotNull(pi);

            // choosing participant task
            List<TaskInstance> bobstask = context.getTaskList("bob");
            assertEquals(1, bobstask.size());
            List<VirtualTaskInstance> participants = new ArrayList<VirtualTaskInstance>();
            participants.add(new VirtualTaskInstance("bob", "dobob", "yobob", null));
            participants.add(new VirtualTaskInstance("trudy", "dotrudy", "yotrudy", null));
            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), participants);
            bobstask.get(0).end();
            // first evaluation
            bobstask = context.getGroupTaskList(bob_list);
            assertEquals(1, bobstask.size());
            List<TaskInstance> trudystask = context.getGroupTaskList(trudy_list);
            assertEquals(0, trudystask.size());

            bobstask.get(0).end();
            // second evaluation
            trudystask = context.getGroupTaskList(trudy_list);
            assertEquals(1, trudystask.size());

            bobstask = context.getGroupTaskList(bob_list);
            assertEquals(0, bobstask.size());

            trudystask.get(0).end("reject");

            // second evalutaion by bob
            bobstask = context.getGroupTaskList(bob_list);
            assertEquals(1, bobstask.size());

            trudystask = context.getGroupTaskList(trudy_list);
            assertEquals(0, trudystask.size());

            bobstask.get(0).end();
            // finished
            assertTrue(pi.hasEnded());
        } finally {
            context.close();
        }
    }

}
