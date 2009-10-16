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
package org.nuxeo.ecm.platform.jbpm.operations;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;

/**
 * @author arussel
 *
 */
public class GetRecipientsForTaskOperation implements JbpmOperation {

    private static final long serialVersionUID = 1L;

    private final long taskId;

    public GetRecipientsForTaskOperation(long taskId) {
        this.taskId = taskId;
    }

    @SuppressWarnings("unchecked")
    public Serializable run(JbpmContext context) throws NuxeoJbpmException {
        HashSet<String> recipients = new HashSet<String>();
        ProcessInstance pi = context.getTaskInstance(taskId).getProcessInstance();
        SwimlaneInstance swimlane = pi.getTaskMgmtInstance().getSwimlaneInstance(
                JbpmService.VariableName.initiator.name());
        recipients.add(swimlane.getActorId());
        for (PooledActor pa : (Set<PooledActor>) swimlane.getPooledActors()) {
            recipients.add(pa.getActorId());
        }
        return recipients;
    }

}
