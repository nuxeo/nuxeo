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
 * $Id: WorkflowDocumentVersioningPolicyBean.java 22255 2007-07-10 13:46:56Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkflowDocumentVersioningPolicyLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkflowDocumentVersioningPolicyRemote;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyManager;

/**
 * Workflow versioning policy session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(WorkflowDocumentVersioningPolicyLocal.class)
@Remote(WorkflowDocumentVersioningPolicyRemote.class)
public class WorkflowDocumentVersioningPolicyBean implements
        WorkflowDocumentVersioningPolicyManager {

    protected static final String[] workflowVersioningPolicies = {
        WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_AUTO,
        WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_CASE_DEPENDENT,
        WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_NO_INCREMENT };

    private static final long serialVersionUID = 950424625015960447L;

    private static final Log log = LogFactory
            .getLog(WorkflowDocumentVersioningPolicyBean.class);

    protected final WorkflowDocumentRelationBusinessDelegate wDocBusinessDelegate
            = new WorkflowDocumentRelationBusinessDelegate();

    public String getVersioningPolicyFor(DocumentRef docRef)
            throws WMWorkflowException {

        String versioningPolicy = null;

        WorkflowDocumentRelationManager wDoc;
        WAPI wapi;
        try {
            wDoc = getWorkflowDocument();
            wapi = getWAPI();
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        String[] workflowInstanceIds = wDoc.getWorkflowInstanceIdsFor(docRef);
        if (workflowInstanceIds.length > 0) {
            // :XXX: The workflow architecture allows to start several processes
            // per document. For now, I take the first one and discard the rest.
            String pid = workflowInstanceIds[0];
            versioningPolicy = (String) wapi
                    .listProcessInstanceAttributes(pid)
                    .get(WorkflowConstants.DOCUMENT_VERSIONING_POLICY);
        } else {
            log.debug("No process (zero) associated to this document...");
        }

        return versioningPolicy;
    }

    /**
     * Returns the workflow document session bean from delegate.
     *
     * @return the workflow document session bean
     * @throws NamingException
     */
    protected WorkflowDocumentRelationManager getWorkflowDocument()
            throws Exception {
        return wDocBusinessDelegate.getWorkflowDocument();
    }

    /**
     * Returns the WAPI session bean from delegate.
     *
     * @return the WAPI session bean
     * @throws NamingException
     */
    protected WAPI getWAPI() throws WMWorkflowException {
        return WAPIBusinessDelegate.getWAPI();
    }

    public String[] getWorkflowVersioningPolicyIds() {
        return workflowVersioningPolicies;
    }

}
