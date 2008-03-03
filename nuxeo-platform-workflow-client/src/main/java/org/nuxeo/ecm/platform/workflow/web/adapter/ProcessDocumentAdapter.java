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
 * $Id: ProcessDocumentAdapter.java 28463 2008-01-03 18:02:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;

/**
 * Process document adapter implementation.
 *
 * <p>
 * This adapter span the workflow services and construct
 * <code>ProcessModel</code> instances holding properties related to the
 * processes against which the actual document is bound.
 * </p>
 *
 * <p>
 * Note, several processes may be bound to one document which is reflected by
 * the <code>ProcessModel</code> collection.
 * </p>
 *
 * @see org.nuxeo.ecm.platform.workflow.web.adapter.ProcessModel
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ProcessDocumentAdapter implements ProcessDocument {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ProcessDocumentAdapter.class);

    private WAPI wapi;

    private WorkflowDocumentRelationManager wfRel;

    protected ProcessModel[] processInfo;

    public ProcessDocumentAdapter() {
        processInfo = new ProcessModel[0];
    }

    public ProcessDocumentAdapter(DocumentModel doc) {
        processInfo = new ProcessModel[0];
        if (getWfRel() == null || getWAPI() == null) {
            log.error("Cannot reach Nuxeo workflow session beans..."
                    + " Cancelling adaptation");
        } else {
            String[] pids = wfRel.getWorkflowInstanceIdsFor(doc.getRef());
            if (pids.length != 0) {
                List<ProcessModel> info = new ArrayList<ProcessModel>();
                for (String pid : pids) {
                    try {
                        WMProcessInstance pi = wapi.getProcessInstanceById(
                                pid, null);
                        if (pi != null) {
                            info.add(createProcessInfoFor(pi));
                        }
                    } catch (WMWorkflowException e) {

                    }
                }
                processInfo = info.toArray(processInfo);
            }
        }
    }

    public ProcessModel[] getProcessInfo() {
        return processInfo;
    }

    /**
     * Creates a process info instance given a process instance.
     *
     * @param pi the actual process instance.
     * @return a <code>ProcessInfo</code> instance.
     */
    private ProcessModel createProcessInfoFor(WMProcessInstance pi) {

        String pid = pi.getId();
        String name = pi.getName();
        String authorName = pi.getAuthorName();
        String status = pi.getState();

        Map<String, Serializable> props = getWAPI().listProcessInstanceAttributes(
                pid);
        String modificationPolicy = (String) props.get(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY);
        String versioningPolicy = (String) props.get(WorkflowConstants.DOCUMENT_VERSIONING_POLICY);
        String reviewType = (String) props.get(WorkflowConstants.WORKLFOW_REVIEW_TYPE);

        int reviewCurrentLevel = 0;
        int reviewFormerLevel = 0;
        try {
            reviewCurrentLevel = (Integer) props.get(WorkflowConstants.WORKFLOW_REVIEW_LEVEL);
            reviewFormerLevel = (Integer) props.get(WorkflowConstants.WORKFLOW_FORMER_REVIEW_LEVEL);
        } catch (NullPointerException npe) {
            log.debug("Cannot get review level information....");
        } catch (ClassCastException cce) {
            log.debug("Cannot get review level information....");
        }

        return new ProcessModelImpl(pid, name, authorName, status,
                modificationPolicy, versioningPolicy, reviewType,
                reviewCurrentLevel, reviewFormerLevel);
    }

    /**
     * Returns the WAPI session bean.
     *
     * @return an EJB stub
     */
    private WAPI getWAPI() {
        if (wapi == null) {
            try {
                wapi = WAPIBusinessDelegate.getWAPI();
            } catch (WMWorkflowException e) {
                log.error(e.getMessage());
            }
        }
        return wapi;
    }

    /**
     * Returns the workflow document relation manager session bean.
     *
     * @return an EJB stub.
     */
    private WorkflowDocumentRelationManager getWfRel() {
        if (wfRel == null) {
            try {
                wfRel = new WorkflowDocumentRelationBusinessDelegate().getWorkflowDocument();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return wfRel;
    }

}
