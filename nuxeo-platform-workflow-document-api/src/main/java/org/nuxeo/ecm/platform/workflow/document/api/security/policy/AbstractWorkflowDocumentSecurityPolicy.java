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
 * $Id: AbstractWorkflowDocumentSecurityPolicy.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.security.policy;

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
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.WorkflowDocumentModificationConstants;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityConstants;

/**
 * Abstract workflow document rights policy.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractWorkflowDocumentSecurityPolicy implements
        WorkflowDocumentSecurityPolicy {

    private static final long serialVersionUID = 5259069687175015784L;

    protected String name;

    protected AbstractWorkflowDocumentSecurityPolicy() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserEntry> getDefaultRules(String pid,
            Map<String, Serializable> infos) throws WMWorkflowException {

        List<UserEntry> userEntries = new ArrayList<UserEntry>();

        if (pid == null) {
            return userEntries;
        }

        if (infos == null) {
            infos = new HashMap<String, Serializable>();
        }

        // Grant initiator
        if (infos.get(WorkflowConstants.WORKFLOW_CREATOR) == null) {
            infos.put(WorkflowConstants.WORKFLOW_CREATOR, getCreatorName(pid));
        }

        if (infos.get(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY) == null) {
            infos.put(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY,
                    getModificationPolicy(pid));
        }

        String initiator = (String) infos.get(WorkflowConstants.WORKFLOW_CREATOR);
        String modificationPolicy = (String) infos.get(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY);

        UserEntry ue = new UserEntryImpl(initiator);
        ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_VIEW, true,
                false);
        userEntries.add(ue);
        if (modificationPolicy != null
                && modificationPolicy.equals(WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_ALLOWED)) {
            ue = new UserEntryImpl(initiator);
            ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_MODIFY,
                    true, false);
            userEntries.add(ue);
        }

        // Order matters here. We need to add the deny *after* the grant if we
        // want the grant to be evaluated.
        ue = new UserEntryImpl(SecurityConstants.EVERYONE);
        ue.addPrivilege(WorkflowDocumentSecurityConstants.DOCUMENT_MODIFY,
                false, false);
        userEntries.add(ue);

        return userEntries;
    }

    protected Collection<WMWorkItemInstance> getTasksFor(String pid,
            Principal principal) throws WMWorkflowException {
        Collection<WMWorkItemInstance> wiis = new ArrayList<WMWorkItemInstance>();
        if (pid != null) {
            WAPI wapi = getWAPI();
            if (principal == null) {
                wiis = wapi.listWorkItems(pid,
                        WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
            } else {
                wiis = wapi.getWorkItemsFor(pid,
                        WMWorkItemState.WORKFLOW_TASK_STATE_ALL,
                        new WMParticipantImpl(principal.getName()));
            }
        }
        return wiis;
    }

    protected Collection<WMWorkItemInstance> getFilteredTasksFor(String pid,
            Principal principal) throws WMWorkflowException {
        Collection<WMWorkItemInstance> wiis = getTasksFor(pid, principal);
        Collection<WMWorkItemInstance> filtered = new ArrayList<WMWorkItemInstance>();
        for (WMWorkItemInstance wii : wiis) {
            if (!wii.isCancelled()) {
                filtered.add(wii);
            }
        }
        return filtered;
    }

    protected static WAPI getWAPI() throws WMWorkflowException {
        return WAPIBusinessDelegate.getWAPI();
    }

    protected static String getModificationPolicy(String pid)
            throws WMWorkflowException {

        String policy = WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_NOT_ALLOWED;

        WAPI wapi = getWAPI();
        Map<String, Serializable> variables = wapi.listProcessInstanceAttributes(pid);

        if (variables != null) {
            policy = (String) variables.get(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY);
        }

        return policy;
    }

    protected static int getCurrentReviewLevel(String pid) throws WMWorkflowException {
        int level = 0;
        if (pid == null) {
            return level;
        }
        Map<String, Serializable> vars = getWorkflowVariables(pid);
        if (vars != null) {
            try {
                level = (Integer) vars.get(WorkflowConstants.WORKFLOW_REVIEW_LEVEL);
            } catch (NullPointerException e) {
                // :XXX:
            }
        }
        return level;
    }

    protected static Map<String, Serializable> getWorkflowVariables(String pid)
            throws WMWorkflowException {
        if (pid == null) {
            return new HashMap<String, Serializable>();
        }
        WAPI wapi = getWAPI();
        return wapi.listProcessInstanceAttributes(pid);
    }

    protected static Serializable getWorkflowVariable(String pid, String name)
            throws WMWorkflowException {
        Serializable target = null;
        Map<String, Serializable> vars = getWorkflowVariables(pid);
        if (vars != null && vars.containsKey(name)) {
            target = vars.get(name);
        }
        return target;
    }

    protected static String getCreatorName(String pid) throws WMWorkflowException {
        String creator = null;
        Map<String, Serializable> vars = getWorkflowVariables(pid);
        if (vars != null) {
            if (vars.containsKey(WorkflowConstants.WORKFLOW_CREATOR)) {
                creator = (String) vars.get(WorkflowConstants.WORKFLOW_CREATOR);
            }
        }
        return creator;
    }

    protected static boolean isCreator(String pid, Principal principal)
            throws WMWorkflowException {

        boolean isCreator = false;

        if (principal != null) {
            String creator = getCreatorName(pid);
            if (creator != null && creator.equals(principal.getName())) {
                isCreator = true;
            }
        }

        return isCreator;
    }

}
