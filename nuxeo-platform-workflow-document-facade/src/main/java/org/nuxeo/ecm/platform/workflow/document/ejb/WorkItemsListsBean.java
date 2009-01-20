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
 * $Id: WorkItemsListsBean.java 29625 2008-01-25 14:20:52Z div $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkItemsListsLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkItemsListsRemote;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListEntry;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;

/**
 * Work items lists session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(WorkItemsListsLocal.class)
@Remote(WorkItemsListsRemote.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class WorkItemsListsBean implements WorkItemsListsManager {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WorkItemsListsBean.class);

    protected EntityManager em;

    @PersistenceUnit(unitName = "NXWorkflowDocument")
    protected transient EntityManagerFactory emf;

    protected final WorkItemsListFactory factory = new WorkItemsListFactory();

    @PersistenceContext(unitName = "NXWorkflowDocument")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @SuppressWarnings("unchecked")
    public List<WorkItemsListEntry> getWorkItemListsFor(String participantName,
            String processName) throws WorkItemsListException {
        try {
            Query query = em.createNamedQuery("getWorkItemsListEntriesFor");
            query.setParameter("participantName", participantName);
            query.setParameter("processName", processName);
            return query.getResultList();
        } catch (Exception e) {
            throw new WorkItemsListException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<WorkItemsListEntry> getWorkItemListsForAll(String processName)
            throws WorkItemsListException {
        try {
            Query query = em.createNamedQuery("getWorkItemsListEntriesForAll");
            query.setParameter("processName", processName);
            return query.getResultList();
        } catch (Exception e) {
            throw new WorkItemsListException(e);
        }
    }

    private void _saveWorkItemsList(WorkItemsListEntry entry) {
        em.persist(entry);
        em.flush();
    }

    public void saveWorkItemsListFor(String pid, String participantName,
            String name) throws WorkItemsListException {
        WorkItemsListEntryImpl entry = (WorkItemsListEntryImpl) factory.feed(
                pid, participantName, name);
        _saveWorkItemsList(entry);
    }

    public void saveWorkItemsList(WorkItemsListEntry entry)
            throws WorkItemsListException {
        _saveWorkItemsList(entry);
    }

    public void restoreWorkItemsListFor(String pid, int wiListEntryId,
            boolean merge, boolean start) throws WorkItemsListException {
        applyWorkItemsListOn(pid, wiListEntryId, merge, start);

        if (!merge) {
            // Set the current review level to 0
            // :XXX: This is too specifics to be here I guess.
            WAPI wapi;
            try {
                wapi = WAPIBusinessDelegate.getWAPI();
            } catch (WMWorkflowException e) {
                throw new WorkItemsListException(e);
            }

            Map<String, Serializable> variables = new HashMap<String, Serializable>();
            variables.put(WorkflowConstants.WORKFLOW_FORMER_REVIEW_LEVEL, 0);
            variables.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL, 0);

            try {
                wapi.updateProcessInstanceAttributes(pid, variables);
            } catch (WMWorkflowException we) {
                throw new WorkItemsListException(we.getMessage());
            }
        }
    }

    public void deleteWorkItemsListById(int entryId)
            throws WorkItemsListException {
        WorkItemsListEntry wiListEntry = getWorkItemListEntry(entryId);
        if (wiListEntry != null) {
            em.remove(wiListEntry);
        } else {
            log.error("Cannot find entry with id=" + entryId);
        }
    }

    public WorkItemsListEntry getWorkItemListEntry(int entryId) {
        WorkItemsListEntry wiListEntry = null;
        try {
            wiListEntry = em.find(WorkItemsListEntryImpl.class, entryId);
        } catch (Exception e) {
            // :XXX: Hibernate bug
            // http://opensource.atlassian.com/projects/hibernate/browse/EJB-98
            // We will return null as it should
            // TODO: more robust exception handling?
            log.error(e);
        }
        return wiListEntry;
    }

    private static int getNextMaxReviewLevel(String pid) throws WorkItemsListException {
        WAPI wapi;
        try {
            wapi = WAPIBusinessDelegate.getWAPI();
        } catch (WMWorkflowException e) {
            throw new WorkItemsListException(e);
        }
        Map<String, Serializable> props = wapi.listProcessInstanceAttributes(pid);
        String reviewType = (String) props.get(WorkflowConstants.WORKLFOW_REVIEW_TYPE);
        // small optimization: do not iterate on tasks if there is no relevant
        // level info

        if (WorkflowConstants.WORKFLOW_REVIEW_TYPE_PARALLEL.equals(reviewType)) {
            return 0;
        }
        Collection<WMWorkItemInstance> taskInstances = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        int level = 0;
        if (taskInstances != null) {
            for (WMWorkItemInstance ti : taskInstances) {
                if (ti.isCancelled()) {
                    continue;
                }
                if (ti.getOrder() > level) {
                    level = ti.getOrder();
                }
            }
        }
        // next => increment level
        level += 1;
        return level;
    }

    /**
     * Applies a work items list on a running process instance.
     *
     * @param pid the process identifier
     * @param entryId the work items list entry id
     * @param merge boolean indicating if old entries should be kept
     */
    protected void applyWorkItemsListOn(String pid, int entryId, boolean merge,
            boolean start) throws WorkItemsListException {

        WorkItemsListEntryImpl wiListEntry = (WorkItemsListEntryImpl) getWorkItemListEntry(entryId);
        if (wiListEntry != null) {
            WAPI wapi;
            try {
                wapi = WAPIBusinessDelegate.getWAPI();
            } catch (WMWorkflowException e) {
                throw new WorkItemsListException(e);
            }

            // Save the id of existing workitems so that we can cancel them
            // after the one from the work items list will be created
            Collection<String> workItemsToCancel = null;
            if (!merge) {
                workItemsToCancel = new ArrayList<String>();
                Collection<WMWorkItemInstance> wiis = wapi.listWorkItems(pid,
                        WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
                for (WMWorkItemInstance wii : wiis) {
                    workItemsToCancel.add(wii.getId());
                }
            }

            int maxLevel = getNextMaxReviewLevel(pid);
            // Create work items taken from the workitem list
            for (WorkItemEntryImpl wie : wiListEntry.getWorkItemEntries()) {

                String wiDefName = wie.getWiName();

                Map<String, Serializable> variables = new HashMap<String, Serializable>();
                variables.put(WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE,
                        wie.getWiDirective());
                variables.put(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        wie.getWiDueDate());
                variables.put(WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT,
                        wie.getWiComment());
                variables.put(WorkflowConstants.WORKFLOW_TASK_ASSIGNEE,
                        wie.getWiParticipant());
                int order = wie.getWiOrder();
                if (merge) {
                    // shift order
                    order += maxLevel;
                }
                variables.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER, order);

                try {

                    // Get the actual work item def id for the target process
                    String workItemDefId = null;
                    Set<WMWorkItemDefinition> defs = wapi.getWorkItemDefinitionsFor(pid);
                    for (WMWorkItemDefinition def : defs) {
                        String defName = def.getName();
                        if (defName != null && defName.equals(wiDefName)) {
                            workItemDefId = def.getId();
                        }
                    }

                    if (workItemDefId == null) {
                        throw new WorkItemsListException(
                                "Cannot create work item with name="
                                        + wiDefName + "on process with pid"
                                        + pid);
                    }

                    WMWorkItemInstance newWii = wapi.createWorkItem(pid,
                            workItemDefId, variables);
                    if (start) {
                        wapi.startWorkItem(newWii.getId());
                    }
                } catch (WMWorkflowException we) {
                    throw new WorkItemsListException(we);
                }
            }

            // Remove old workitems
            if (!merge && workItemsToCancel != null) {
                for (String id : workItemsToCancel) {
                    try {
                        wapi.removeWorkItem(id);
                    } catch (WMWorkflowException we) {
                        throw new WorkItemsListException(we);
                    }
                }
            }
        } else {
            throw new WorkItemsListException("Cannot find entry with id="
                    + entryId);
        }
    }

    @SuppressWarnings("unchecked")
    public WorkItemsListEntry getWorkItemListEntryByName(
            String participantName, String name) throws WorkItemsListException {
        try {
            Query query = em.createNamedQuery("getWorkItemsListEntryByName");
            query.setParameter("participantName", participantName);
            query.setParameter("name", name);
            if (!query.getResultList().isEmpty()) {
                return (WorkItemsListEntry) query.getResultList().iterator().next();
            }
            return null;
        } catch (Exception e) {
            throw new WorkItemsListException(e);
        }
    }

}
