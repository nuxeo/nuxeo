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
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.platform.jbpm.test.AbstractProcessDefinitionTest;

/**
 * @author arussel
 *
 */
public class PDParallelValidationTest extends AbstractProcessDefinitionTest {

    @SuppressWarnings("unchecked")
    public void testPD() {
        JbpmContext context = null;
        try {
            context = configuration.createJbpmContext();
            context.setActorId("bob");
            assertNotNull(context);

            context.deployProcessDefinition(pd);
            ProcessInstance pi = context.newProcessInstanceForUpdate("review_parallel");
            TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
            ti.end();
            assertNotNull(pi);
            assertEquals("bob", pi.getTaskMgmtInstance().getSwimlaneInstance(
                    JbpmService.VariableName.initiator.name()).getActorId());

            Set<TaskInstance> tis = (Set<TaskInstance>) pi.getTaskMgmtInstance().getTaskInstances();
            assertNotNull(tis);
            assertEquals(2, tis.size());

            List bobstask = context.getTaskList("bob");
            assertEquals(1, bobstask.size());
            assertEquals("bob",
                    tis.toArray(new TaskInstance[] {})[0].getActorId());

            // bob finish choosing the participants
            List<VirtualTaskInstance> participants = new ArrayList<VirtualTaskInstance>();
            participants.add(new VirtualTaskInstance("bob", "dobob", "yobob", null));
            participants.add(new VirtualTaskInstance("trudy", "dotrudy", "yotrudy", null));
            pi.getContextInstance().setVariable("participants", participants);
            ti = (TaskInstance) context.getTaskList("bob").get(0);
            ti.end();
            // bob and trudy have tasks
            pi = context.getProcessInstance(pi.getId());
            assertNull(pi.getContextInstance().getVariable("participants"));
            assertEquals(1, context.getGroupTaskList(bob_list).size());
            assertEquals(1, context.getGroupTaskList(trudy_list).size());

            ti = (TaskInstance) context.getGroupTaskList(bob_list).get(0);
            assertEquals("yobob", ((Comment)(ti.getComments().get(0))).getMessage());
            assertEquals("dobob", ti.getVariable("directive"));

            ti.end();
            ti = (TaskInstance) context.getGroupTaskList(trudy_list).get(0);
            ti.end("reject");
            // process finished
            assertTrue(pi.hasEnded());
        } finally {
            context.close();
        }
    }

    @Override
    public String getProcessDefinitionResource() {
        return "/process/parallel-review.xml";
    }

}
