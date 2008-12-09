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
 * $Id: ParallelReviewWorkflowDocumentSecurityPolicy.java 26221 2007-10-19 15:28:25Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.document.security.policy;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.WorkflowDocumentModificationConstants;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityConstants;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.AbstractWorkflowDocumentSecurityPolicy;

/**
 * Security for parallel review.
 *
 * <p>
 * Please add tests in
 * <code>TestParallelReviewWorkflowDocumentSecurityPolicy</code> if you are
 * changing implementation here.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ParallelReviewWorkflowDocumentSecurityPolicy extends
        AbstractWorkflowDocumentSecurityPolicy {

    private static final long serialVersionUID = 2427914243230250718L;

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy#getRules(java.lang.String,
     *      java.util.Map)
     */
    public List<UserEntry> getRules(String pid, Map<String, Serializable> infos)
            throws WMWorkflowException {

        List<UserEntry> userEntries = new ArrayList<UserEntry>();

        if (infos == null) {
            infos = new HashMap<String, Serializable>();
        }

        if (infos.get(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY) == null) {
            infos.put(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY,
                    getModificationPolicy(pid));
        }
        String modificationPolicy = (String) infos.get(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY);

        if (infos.get(WorkflowConstants.WORKFLOW_CREATOR) == null) {
            infos.put(WorkflowConstants.WORKFLOW_CREATOR, getCreatorName(pid));
        }
        String creator = (String) infos.get(WorkflowConstants.WORKFLOW_CREATOR);

        UserEntry ue;
        Collection<WMWorkItemInstance> tasks = getTasksFor(pid, null);

        // Check if we should deny the creator who will be granted later on by
        // getDefaultRules().
        // He should be denied if participants exist and if it's not part of
        // them.
        boolean denyProcessCreator = !tasks.isEmpty();

        // Loop over the participants.
        for (WMWorkItemInstance ti : tasks) {

            // do not set rights for invalid tasks
            if (ti.isCancelled()) {
                continue;
            }
            // XXX AT: should not set rights for rejected tasks either, but
            // they're not marked as valid again when user gets the document
            // back...

            ue = new UserEntryImpl(ti.getParticipantName());

            // First grant the process participants the ability to view the
            // review tab
            ue.addPrivilege(
                    WorkflowDocumentSecurityConstants.WORKFLOW_PARTICIPANT,
                    true, false);

            // Grant user the view permission. Useful when users are restricted
            // member.
            ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_VIEW,
                    true, false);

            // Only allow modification to the participants if allowed by the
            // process.
            if (modificationPolicy.equals(WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_ALLOWED)) {
                // If the task has been approved then do not grant him write
                // anymore.
                if (!ti.hasEnded()) {
                    // Here the creator should loose write rights of not in the
                    // stack anymore.
                    if (ti.getParticipantName().equals(creator)) {
                        denyProcessCreator = false;
                    } else {
                        ue.addPrivilege(
                                WorkflowDocumentSecurityConstants.DOCUMENT_MODIFY,
                                true, false);
                    }
                }
            }

            userEntries.add(ue);
        }

        if (denyProcessCreator) {
            // Deny creator (see above for comments)
            ue = new UserEntryImpl(creator);
            ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_MODIFY,
                    false, false);
            userEntries.add(ue);
        }

        // Deny everyone the write access once under review.
        // Order matters here. We need to add the deny *after* the grant if we
        // want the grant to be evaluated.
        userEntries.addAll(getDefaultRules(pid, infos));

        return userEntries;
    }

    public boolean canManageWorkflow(String pid, Principal principal)
            throws WMWorkflowException {

        boolean grant = false;

        if (pid == null || principal == null) {
            return grant;
        }

        grant = isCreator(pid, principal);
        if (!grant) {
            grant = hasParticipantImmediateAction(pid, principal);
        }

        return grant;
    }

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

    public boolean selectThisItem(WMWorkItemInstance item) {
        return true;
    }

    public boolean canEndWorkItem(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            String pid = wi.getProcessInstance().getId();
            int nbTasks = getFilteredTasksFor(pid, null).size();
            granted = wi.getParticipantName().equals(participant.getName())
                    && nbTasks > 1 && !wi.hasEnded()
                    && getFilteredTasksFor(pid, participant).size() < nbTasks;
        }
        return granted;
    }

    public boolean canRejectWorkItem(Principal participant,
            WMWorkItemInstance wi) throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            granted = ((wi.getParticipantName().equals(participant.getName()))
                    && (!wi.hasEnded()) && (!wi.isRejected()));
        }
        return granted;
    }

    public boolean canRemoveWorkItem(Principal participant,
            WMWorkItemInstance wi) throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            granted = ((!wi.getParticipantName().equals(participant.getName()))
                    && canManageWorkflow(wi.getProcessInstance().getId(),
                            participant) && (!wi.hasEnded()) && (!wi.isRejected()));
        }
        return granted;
    }

    public boolean canMoveDown(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        return false;
    }

    public boolean canMoveUp(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        return false;
    }


}
