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

import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author arussel
 *
 */
public class ValidationReviewTest extends AbstractProcessDefinitionTest {

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.jbpm.core.pd.AbstractProcessDefinitionTest#getProcessDefinitionResource()
     */
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
            ProcessInstance pi = context.newProcessInstanceForUpdate("validation-review");
            TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
            ti.end();
            assertNotNull(pi);
            //choosing participant task
            List<TaskInstance> bobstask = context.getTaskList("bob");
            assertEquals(1, bobstask.size());
            pi.getContextInstance().setVariable("participants", getPrincipalsList());
            bobstask.get(0).end();
            //first evaluation
            bobstask = context.getTaskList("bob");
            assertEquals(1, bobstask.size());
            List<TaskInstance> trudystask = context.getTaskList("trudy");
            assertEquals(0,trudystask.size());
            bobstask.get(0).end();
            //second evaluation
            trudystask = context.getTaskList("trudy");
            assertEquals(1,trudystask.size());
            bobstask = context.getTaskList("bob");
            assertEquals(0, bobstask.size());
            trudystask.get(0).end();
            //finished
            assertTrue(pi.hasEnded());
        }
        finally {
            context.close();
        }
    }

}
