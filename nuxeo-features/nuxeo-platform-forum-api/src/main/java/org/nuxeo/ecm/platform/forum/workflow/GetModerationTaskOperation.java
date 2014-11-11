/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forum.workflow;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;

/**
 * @author arussel
 * 
 */
public class GetModerationTaskOperation implements JbpmOperation {

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog(GetModerationTaskOperation.class);

    private long processId;

    public GetModerationTaskOperation(long processId) {
        this.processId = processId;
    }

    public Serializable run(JbpmContext context) throws NuxeoJbpmException {
        ProcessInstance pi = context.getProcessInstance(processId);
        if (pi != null) {
            Collection tasks = pi.getTaskMgmtInstance().getTaskInstances();
            if (tasks != null && !tasks.isEmpty()) {
                if (tasks.size() > 1) {
                    log.error("There are several moderation tasks, "
                            + "taking only first found");
                }
                TaskInstance task = (TaskInstance) tasks.iterator().next();
                return task;
            }
        }
        return null;
    }

}
