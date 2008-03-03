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
 * $Id: OrderedReviewWorkflowDocumentSecurityPolicy.java 29671 2008-01-27 15:22:03Z atchertchian $
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
 * Security policy for ordered review.
 * <p>
 * Please add tests in
 * <code>TestOrderedReviewWorkflowDocumentSecurityPolicy</code> if you are
 * changing implementation here.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class OrderedReviewWorkflowDocumentSecurityPolicy extends
        AbstractWorkflowDocumentSecurityPolicy {

    private static final long serialVersionUID = -6068689688586728791L;

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

        if (infos.get(WorkflowConstants.WORKFLOW_REVIEW_LEVEL) == null) {
            infos.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL,
                    getCurrentReviewLevel(pid));
        }
        int currentReviewLevel = (Integer) infos.get(WorkflowConstants.WORKFLOW_REVIEW_LEVEL);

        if (infos.get(WorkflowConstants.WORKFLOW_CREATOR) == null) {
            infos.put(WorkflowConstants.WORKFLOW_CREATOR, getCreatorName(pid));
        }
        String creator = (String) infos.get(WorkflowConstants.WORKFLOW_CREATOR);

        Collection<WMWorkItemInstance> tasks = getTasksFor(pid, null);

        // Check if we should deny the creator who will be granted later on by
        // getDefaultRules().
        // He should be denied if participants exist and if it's not part of
        // them.
        boolean denyProcessCreator = true;
        if (tasks != null && tasks.isEmpty()) {
            denyProcessCreator = false;
        }

        // Loop over participants.
        UserEntry ue;
        for (WMWorkItemInstance ti : tasks) {

            // do not set rights for invalid tasks
            if (ti.isCancelled()) {
                continue;
            }
            // XXX AT: should not set rights for rejected tasks either, but
            // they're not marked as valid again when user gets the document
            // back...

            // First grant the process participants the ability to view the
            // review tab
            ue = new UserEntryImpl(ti.getParticipantName());
            ue.addPrivilege(
                    WorkflowDocumentSecurityConstants.WORKFLOW_PARTICIPANT,
                    true, false);

            // Grant user the view permission. Useful when users are restricted
            // member.
            ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_VIEW,
                    true, false);

            // Grant write if allowed by the process only to the user at current
            // level.
            if (modificationPolicy.equals(WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_ALLOWED)) {
                if (ti.getOrder() == currentReviewLevel) {
                    // Here the creator should loose write rights of not in the
                    // stack anymore.
                    if (ti.getParticipantName().equals(creator)) {
                        denyProcessCreator = false;
                    } else {
                        // Grant write
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

        // Add default

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
        int currentReviewLevel = getCurrentReviewLevel(pid);
        for (WMWorkItemInstance ti : getTasksFor(pid, null)) {
            if (ti.getParticipantName().equals(principal.getName())
                    && ti.getOrder() == currentReviewLevel) {
                hasAction = true;
                break;
            }
        }
        return hasAction;
    }

    public boolean canEndWorkItem(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            String pid = wi.getProcessInstance().getId();
            granted = wi.getParticipantName().equals(participant.getName())
                    && getCurrentReviewLevel(pid) == wi.getOrder();
        }
        return granted;
    }

    public boolean canRejectWorkItem(Principal particpant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        boolean granted = false;
        if (particpant != null && wi != null) {
            String pid = wi.getProcessInstance().getId();
            int currentReviewLevel = getCurrentReviewLevel(pid);
            granted = wi.getParticipantName().equals(particpant.getName())
                    && currentReviewLevel == wi.getOrder()
                    && currentReviewLevel > 0;
        }
        return granted;
    }

    public boolean canRemoveWorkItem(Principal participant,
            WMWorkItemInstance wi) throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            String pid = wi.getProcessInstance().getId();
            granted = !(wi.getParticipantName().equals(participant.getName()) && wi.getOrder() == getCurrentReviewLevel(pid))
                    && canManageWorkflow(pid, participant)
                    && getCurrentReviewLevel(pid) < wi.getOrder();
        }
        return granted;
    }

    public boolean canMoveDown(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            String pid = wi.getProcessInstance().getId();

            if (!canManageWorkflow(pid, participant)) {
                return granted;
            }

            int currentReviewLevel = getCurrentReviewLevel(pid);

            // Not possible to move items above or below current level.
            if (wi.getOrder() - 1 <= currentReviewLevel) {
                return granted;
            }

            // ok.
            granted = true;

        }
        return granted;
    }

    public boolean canMoveUp(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        boolean granted = false;
        if (participant != null && wi != null) {
            String pid = wi.getProcessInstance().getId();

            if (!canManageWorkflow(pid, participant)) {
                return granted;
            }

            int currentReviewLevel = getCurrentReviewLevel(pid);

            // Not possible to move items above or below current level.
            if (wi.getOrder() == currentReviewLevel
                    || wi.getOrder() + 1 <= currentReviewLevel) {
                return granted;
            }

            // Check if there is another task at a higher level.
            for (WMWorkItemInstance each : getTasksFor(pid, null)) {
                if (each.getOrder() > wi.getOrder()) {
                    granted = true;
                    break;
                }
            }

        }
        return granted;
    }

}
