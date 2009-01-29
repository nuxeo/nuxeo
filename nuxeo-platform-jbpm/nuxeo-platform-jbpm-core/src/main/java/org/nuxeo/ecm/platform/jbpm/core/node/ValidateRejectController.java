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
package org.nuxeo.ecm.platform.jbpm.core.node;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;

/**
 *
 * @author arussel
 *
 */
public class ValidateRejectController extends AbstractJbpmHandlerHelper {

    public static final String WORKFLOW_DIRECTIVE_TASK_REJECTED = "workflowDirectiveTaskRejected";

    private static final long serialVersionUID = 1L;

    @Override
    public void submitTaskVariables(TaskInstance taskInstance,
            ContextInstance contextInstance, Token token) {
    }

    @Override
    public void initializeTaskVariables(TaskInstance taskInstance,
            ContextInstance contextInstance, Token token) {
        taskInstance.addComment((Comment) token.getComments().get(
                token.getComments().size() - 1));
        taskInstance.setVariableLocally("directive",
                WORKFLOW_DIRECTIVE_TASK_REJECTED);
    }

}
