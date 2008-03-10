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
 *     bchaffangeon
 *
 * $Id: ForumEnterModerationHandler.java 29637 2008-01-25 16:31:04Z ldoguin $
 */

package org.nuxeo.ecm.platform.forum.workflow;

import java.util.Map;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 *
 */
public class ForumEnterModerationHandler extends
        AbstractForumWorkflowDocumentHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
        Token token = executionContext.getToken();
        TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();

        TaskNode taskNode = (TaskNode) executionContext.getNode();

        Map tasks = taskNode.getTasksMap();

        Object[] moderators = (Object[]) executionContext.getVariable(
                ForumConstants.FORUM_MODERATORS_LIST);

        if (moderators != null) {
            for (Object moderator : moderators) {
                for (Object k : tasks.keySet()) {
                    TaskInstance ti = tmi.createTaskInstance(
                            taskNode.getTask((String) k), token);
                    ti.start();
                    log.debug("Moderation : Creating and starting task ="
                            + ti.getId() + " for assignee : " + moderator);
                    ti.setActorId((String) moderator);
                }
            }
        }
    }

}
