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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.publishing;

import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.workflow.PublishingConstants;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;

/**
 * @author Alexandre Russel
 */
public class PublishingTasks {

    private final WorkflowDocumentRelationBusinessDelegate wrbd = new WorkflowDocumentRelationBusinessDelegate();
    private final DocumentModel currentDocument;
    private final WAPI wapi;
    private final Principal currentUser;

    public PublishingTasks(DocumentModel doc, Principal cu) throws PublishingException {
        currentDocument = doc;
        currentUser = cu;
        try {
            wapi =  WAPIBusinessDelegate.getWAPI();
        } catch (WMWorkflowException e) {
            throw new PublishingException(e);
        }
    }

    public WMWorkItemInstance getPublishingWorkItem()
            throws PublishingException {
        if(currentDocument == null) {
            return null;
        }
        WMWorkItemInstance workItem = null;

        try {
            String[] pids = wrbd.getWorkflowDocument().getWorkflowInstanceIdsFor(
                    currentDocument.getRef());
            for (String pid : pids) {

                // Check we are on the right process.
                WMProcessInstance pi = wapi.getProcessInstanceById(pid,
                        WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE);
                if (!pi.getName().equals(
                        PublishingConstants.WORKFLOW_DEFINITION_NAME)) {
                    continue;
                }

                boolean found = false;
                for (WMWorkItemInstance wi : wapi.listWorkItems(pid,
                        WMWorkItemState.WORKFLOW_TASK_STATE_STARTED)) {
                    if (wi.getParticipantName().equals(currentUser.getName())) {
                        workItem = wi;
                        found = true;
                        break;
                    }
                    // Try group resolution
                    if (currentUser instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                        for (String groupName : groupNames) {
                            if (wi.getParticipantName().equals(groupName)) {
                                workItem = wi;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        break;
                    }

                }

                if (found) {
                    break;
                }

            }
        } catch (Exception e) {
            throw new PublishingException(e);
        }
        return workItem;
    }

}
