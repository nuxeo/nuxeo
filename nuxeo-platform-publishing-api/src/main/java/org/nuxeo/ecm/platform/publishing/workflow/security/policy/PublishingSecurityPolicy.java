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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingSecurityPolicy.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.workflow.security.policy;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.platform.publishing.workflow.PublishingConstants;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityConstants;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.AbstractWorkflowDocumentSecurityPolicy;

/**
 * Publishing workflow security policy.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class PublishingSecurityPolicy extends
        AbstractWorkflowDocumentSecurityPolicy {

    private static final long serialVersionUID = 1L;

    public boolean canEndWorkItem(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canManageWorkflow(String pid, Principal principal)
            throws WMWorkflowException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canMoveDown(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canMoveUp(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canRejectWorkItem(Principal participant,
            WMWorkItemInstance wi) throws WMWorkflowException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canRemoveWorkItem(Principal participant,
            WMWorkItemInstance wi) throws WMWorkflowException {
        // TODO Auto-generated method stub
        return false;
    }

    public List<UserEntry> getRules(String pid, Map<String, Serializable> infos)
            throws WMWorkflowException {
        List<UserEntry> userEntries = new ArrayList<UserEntry>();

        if (infos == null) {
            infos = new HashMap<String, Serializable>();
        }

        Collection<WMWorkItemInstance> tasks = getTasksFor(pid, null);

        // Loop over participants and give view access to participants.
        UserEntry ue;
        for (WMWorkItemInstance ti : tasks) {
            ue = new UserEntryImpl(ti.getParticipantName());
            ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_VIEW,
                    true, false);
            userEntries.add(ue);

            ue = new UserEntryImpl(ti.getParticipantName());
            ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_MODIFY,
                    true, false);
            userEntries.add(ue);
        }

        // Give View permission to the user who submitted for publishing.
        String submittedBy = (String) getWorkflowVariable(pid,
                PublishingConstants.SUBMITTED_BY);
        ue = new UserEntryImpl(submittedBy);
        ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_VIEW,
                true, false);
        userEntries.add(ue);

        // Deny everyone the write and read access once process has started.
        // Order matters here. We need to add the deny *after* the grant if we
        // want the grant to be evaluated.
        userEntries.addAll(getDefaultRules(pid, infos));

        return userEntries;
    }

    @Override
    public List<UserEntry> getDefaultRules(String pid,
            Map<String, Serializable> infos) throws WMWorkflowException {

        List<UserEntry> userEntries = new ArrayList<UserEntry>();

        if (pid == null) {
            return userEntries;
        }

        if (infos == null) {
            infos = new HashMap<String, Serializable>();
        }

        // Remove write permission to everyone
        UserEntry ue = new UserEntryImpl(SecurityConstants.EVERYONE);
        ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_MODIFY,
                false, false);
        userEntries.add(ue);

        // Remove view permission to everyone.
        ue = new UserEntryImpl(SecurityConstants.EVERYONE);
        ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_VIEW, false,
                false);
        userEntries.add(ue);

        return userEntries;
    }

    /**
     * Dashboard uses this to display the task.
     */
    public boolean hasParticipantImmediateAction(String pid, Principal principal)
            throws WMWorkflowException {
        boolean hasAction = false;
        for (WMWorkItemInstance ti : getTasksFor(pid, null)) {
            if (ti.getParticipantName().equals(principal.getName())
                    && !ti.hasEnded()) {
                hasAction = true;
                break;
            }
        }
        return hasAction;
    }

    @Override
    public boolean selectThisItem(WMWorkItemInstance item) {
        return true;
    }

}
