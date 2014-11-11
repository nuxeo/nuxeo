/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestActionHandler.java 22430 2007-07-12 13:14:31Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestActionHandler extends JbpmWorkflowActionHandler {

    private static final long serialVersionUID = 1327637414446001773L;

    private static final Log log = LogFactory
            .getLog(TestActionHandler.class);

    public void execute(ExecutionContext executionContext) throws Exception {
        Token token = executionContext.getToken();
        TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();

        TaskNode taskNode = (TaskNode) executionContext.getNode();

        Map tasks = taskNode.getTasksMap();
        for (Object k : tasks.keySet()) {
            log.info("Create task =" + k);
            TaskInstance ti = tmi.createTaskInstance(taskNode.getTask((String) k), token);
            ti.start();
        }

    }

}
