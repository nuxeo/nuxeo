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
 * $Id: PostModerationWorkflowSecurityPolicy.java 22395 2007-07-11 18:59:28Z janguenot $
 */

package org.nuxeo.ecm.platform.forum.workflow.security.policy;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.AbstractWorkflowDocumentSecurityPolicy;

/**
 * Post moderation security policy.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class PostModerationWorkflowSecurityPolicy extends
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
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Used by the dashboard actions bean to know if whether or not it should
     * display the work item in its list for the current user.
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

    public boolean canMoveDown(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        return false;
    }

    public boolean canMoveUp(Principal participant, WMWorkItemInstance wi)
            throws WMWorkflowException {
        return false;
    }

}
