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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

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
                    "initiator").getActorId());
            Set<TaskInstance> tis = (Set<TaskInstance>) pi.getTaskMgmtInstance().getTaskInstances();
            assertNotNull(tis);
            assertEquals(2, tis.size());
            List bobstask = context.getTaskList("bob");
            assertEquals(1, bobstask.size());
            assertEquals("bob",
                    tis.toArray(new TaskInstance[] {})[0].getActorId());
            // bob finish choosing the participants
            pi.getContextInstance().setVariable("participants",
                    Arrays.asList(new String[] { "bob", "trudy" }));
            ti = (TaskInstance) context.getTaskList("bob").get(0);
            ti.end();
            // bob and trudy have tasks
            assertEquals(1, context.getTaskList("bob").size());
            assertEquals(1, context.getTaskList("trudy").size());
            for (String actorId : new String[] { "bob", "trudy" }) {
                ti = (TaskInstance) context.getTaskList(actorId).get(0);
                ti.end();
            }
            List l = pi.findAllTokens();
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
