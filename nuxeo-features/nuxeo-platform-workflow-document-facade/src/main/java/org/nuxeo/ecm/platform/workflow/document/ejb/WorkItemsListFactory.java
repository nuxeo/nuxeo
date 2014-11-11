/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: WorkItemsListFactory.java 22255 2007-07-10 13:46:56Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListEntry;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;

/**
 * Work items list factory.
 *
 * @see org.nuxeo.platform.workflow.ejb.WorkItemsListsBean
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkItemsListFactory implements Serializable {

    private static final long serialVersionUID = -1001657403902605977L;

    private static final Log log = LogFactory
            .getLog(WorkItemsListFactory.class);

    public WorkItemsListEntry feed(String pid, String participantName, String label)
            throws WorkItemsListException {

        WorkItemsListEntryImpl res = new WorkItemsListEntryImpl();
        res.setParticipantName(participantName);
        res.setName(label);

        WAPI wapi;
        try {
            wapi = WAPIBusinessDelegate.getWAPI();
        } catch (Exception e) {
            throw new WorkItemsListException(e);
        }

        WMProcessInstance pi;
        try {
            pi = wapi.getProcessInstanceById(pid,
                    WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE);
        } catch (WMWorkflowException we) {
            throw new WorkItemsListException(we);
        }

        if (pi == null) {
            log.error("Cannot find process with pid=" + pid);
        }

        res.setProcessName(pi.getProcessDefinition().getName());

        Collection<WMWorkItemInstance> wiis;
        Set<WMWorkItemInstance> filteredWis = new HashSet<WMWorkItemInstance>();

        wiis = wapi.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);

        for (WMWorkItemInstance wii : wiis) {
            if (wii.isCancelled()) {
                continue;
            }
            filteredWis.add(wii);
        }

        for (WMWorkItemInstance wii : filteredWis) {
            WorkItemEntryImpl wie = new WorkItemEntryImpl();

            wie.setWiDirective(wii.getDirective());
            wie.setWiDueDate(wii.getDueDate());
            wie.setWiName(wii.getName());
            wie.setWiOrder(wii.getOrder());
            wie.setWiParticipant(wii.getParticipantName());
            wie.setWiComment(wii.getComment());

            wie.setWorkItemsListEntry(res);

            res.getWorkItemEntries().add(wie);
        }

        if (res.getWorkItemEntries().isEmpty()) {
            res.setWorkItemEntries(null);
        }

        return res;
    }

}
