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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.document.api.security.policy;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;

/**
 * Workflow document security policy.
 * <p>
 * Defines the policy regarding the distributions of rights to principals on a
 * document. Each policy is associated to a given process.
 *
 * @see org.nuxeo.ecm.platform.workflow.document.service.WorkflowDocumentRightsPolicySevice
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowDocumentSecurityPolicy extends Serializable {

    /**
     * Returns the policy name.
     *
     * @return the policy name
     */
    String getName();

    /**
     * Sets the policy name.
     *
     * @param name the policy name
     */
    void setName(String name);

    /**
     * Computes rules based on default and participant list.
     *
     * @param pid the process id.
     * @param infos a process info map.
     * @return a list of UserEntries.
     * @throws WMWorkflowException
     */
    List<UserEntry> getRules(String pid, Map<String, Serializable> infos)
            throws WMWorkflowException;

    /**
     * Computes default rules
     * <p>
     * Aimed at being used when the process starts up.
     *
     * @param pid the process id.
     * @param infos a process info map.
     * @return a list of user entries to use to build an ACL.
     * @throws WMWorkflowException
     */
    List<UserEntry> getDefaultRules(String pid, Map<String, Serializable> infos)
            throws WMWorkflowException;

    /**
     * Can the user manage workflow ?
     *
     * @param pid the process identifier
     * @param principal the principal.
     *
     * @return true if granted, false if not.
     * @throws WMWorkflowException
     */
    boolean canManageWorkflow(String pid, Principal principal)
            throws WMWorkflowException;

    /**
     * Has the participant a direct action to perform ?
     *
     * @param pid the process id
     * @param principal the principal
     * @return true if direct action to perform, false if not.
     * @throws WMWorkflowException
     */
    boolean hasParticipantImmediateAction(String pid, Principal principal)
            throws WMWorkflowException;

    /**
     * Should we show this item in a dashboard.
     * @param item
     * @return
     * @throws WMWorkflowException
     */
    boolean selectThisItem(WMWorkItemInstance item) throws WMWorkflowException;

    /**
     * Checks if a given participant can remove a given work item.
     *
     * @param participant the current workflow participant.
     * @param wi the work item instance
     * @return true if granted / false if denied
     * @throws WMWorkflowException TODO
     */
    boolean canRemoveWorkItem(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException;

    /**
     * Checks if a given participant can end a given work item.
     *
     * @param participant the current workflow participant.
     * @param wi the work item instance.
     * @return true if granted / false if denied
     * @throws WMWorkflowException TODO
     */
    boolean canEndWorkItem(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException;

    /**
     * Checks if a given participant can reject a given work item.
     *
     * @param particpant the actual workflow participant.
     * @param wi the work item instance.
     * @return true if granted / false if denied
     * @throws WMWorkflowException TODO
     */
    boolean canRejectWorkItem(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException;

    /**
     * Checks if a given participant can move up a given work item.
     * <p>
     * Here, it will change the order attribute of the work item if possible.
     * <p>
     * This is specific to ordered reviews.
     *
     * @param particpant the current workflow participant.
     * @param wi the work item instance.
     * @return true if granted / false if denied
     * @throws WMWorkflowException TODO
     */
    boolean canMoveUp(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException;

    /**
     * Checks if a given participant can move down a given work item.
     * <p>
     * Here, it will change the order attribute of the work item if possible.
     * <p>
     * This is specific to ordered reviews.
     *
     * @param particpant the current workflow participant.
     * @param wi the work item instance.
     * @return true if granted / false if denied
     * @throws WMWorkflowException TODO
     */
    boolean canMoveDown(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException;

}
