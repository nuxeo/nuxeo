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
 * $Id: WorkflowBeansDelegate.java 19515 2007-05-28 12:00:12Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.web.api;

import java.io.Serializable;

import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.lifecycle.WorkflowDocumentLifeCycleManager;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;

/**
 * Workflow beans delegate.
 * <p>
 * Helper for all client listeners that want to interact with all the workflow
 * session beans.
 * <p>
 * This might be seen as the common client workflow framework.
 *
 * @See org.nuxeo.ecm.platform.workflow.web.delegate
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowBeansDelegate extends Serializable {

    /**
     * Returns the WAPI bean from the delegate.
     *
     * @return WAPI bean instance
     * @throws WMWorkflowException
     */
    WAPI getWAPIBean() throws WMWorkflowException;

    /**
     * Returns the WorkflowDocumentSecurityManager bean from the delegate.
     *
     * @return a WorkflowDocumentSecurityManager bean instance
     * @throws WMWorkflowException
     */
    WorkflowDocumentSecurityManager getWFSecurityManagerBean()
            throws WMWorkflowException;

    /**
     * Returns a WorkflowDocumentRelationManager bean from the delegate.
     *
     * @return a WorkflowDocumentRelationManager bean instance
     * @throws WMWorkflowException
     */
    WorkflowDocumentRelationManager getWorkflowDocumentBean()
            throws WMWorkflowException;

    /**
     * Returns a WorkflowDocumentLifeCycleManager bean from the delegate.
     *
     * @return a WorkflowDocumentLifeCycle bean instance
     * @throws WMWorkflowException
     */
    WorkflowDocumentLifeCycleManager getWfDocLifeCycleManagerBean()
            throws WMWorkflowException;

    /**
     * Returns a WorkflowRulesManager bean from the delegate.
     *
     * @return a WorkflowRulesManager bean instance
     */
    WorkflowRulesManager getWorkflowRulesBean() throws WMWorkflowException;

    /**
     * Returns a DocumentMessagerProducer bean from the delegate.
     *
     * @return a DocumentMessageProducer bean instance
     * @throws Exception
     */
    DocumentMessageProducer getDocumentMessageProducer()
            throws WMWorkflowException;

    /**
     * Returns a WorklowVersioningPolicy session bean from the delegate.
     *
     * @return a WorklowVersioningPolicy session bean from the delegate
     * @throws WMWorkflowException
     */
    WorkflowDocumentVersioningPolicyManager getWorkflowVersioningPolicy()
            throws WMWorkflowException;

    /**
     * Returns a WorkflowDocumentSecurityPolicyManager session bean from the
     * delegate.
     *
     * @return a WorkflowDocumentSecurityPolicyManager session bean from the
     *         delegate
     */
    WorkflowDocumentSecurityPolicyManager getWorkflowDocumentSecurityPolicy()
            throws WMWorkflowException;

    /**
     * Return a WorkItemsLists session bean from the corresponding delegate.
     *
     * @return a WorkItemsLists session bean from the corresponding delegate.
     * @throws WorkItemsListException
     */
    WorkItemsListsManager getWorkItemsLists() throws WorkItemsListException;

}
