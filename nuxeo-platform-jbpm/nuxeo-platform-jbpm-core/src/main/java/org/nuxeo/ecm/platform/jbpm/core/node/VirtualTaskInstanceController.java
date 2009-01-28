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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;

/**
 * @author arussel
 *
 */
public class VirtualTaskInstanceController extends AbstractJbpmHandlerHelper {
    private static Log log = LogFactory.getLog(VirtualTaskInstanceController.class);

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public void initializeTaskVariables(TaskInstance taskInstance,
            ContextInstance contextInstance, Token token) {
        VirtualTaskInstance vti = (VirtualTaskInstance) contextInstance.getTransientVariable("participant");
        if(vti == null) {
            List<VirtualTaskInstance> vtis = (List<VirtualTaskInstance>)contextInstance.getVariable("participants");
            vti =  vtis.get(0);
        }
        taskInstance.setDueDate(vti.getDueDate());
        try {
            taskInstance.addComment(new Comment(
                    (String) contextInstance.getVariable("initiator"),
                    vti.getComment()));
            taskInstance.setVariableLocally("directive", vti.getDirective());
        } catch (Exception e) {
            log.error("Error in Virtual Task Instance Controller", e);
        }

    }

    @Override
    public void submitTaskVariables(TaskInstance taskInstance,
            ContextInstance contextInstance, Token token) {
    }
}
