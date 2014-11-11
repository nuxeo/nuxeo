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
 * $Id: DocumentWorkflowActions.java 29670 2008-01-27 15:21:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.web.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessDocument;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessModel;

/**
 * Workflow actions listener interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface DocumentWorkflowActions extends Serializable {

    /**
     * Returns the current life cycle state.
     *
     * @return the current life cycle state
     */
    String getCurrentLifeCycleState() throws WMWorkflowException;

    /**
     * Returns the life cycle policy.
     *
     * @return the life cycle policy
     */
    String getLifeCyclePolicy() throws WMWorkflowException;

    /**
     * Returns filtered workflow definitions based on defined rules.
     *
     * @return an array of WMProcessDefinition instances
     */
    Collection<WMProcessDefinition> getAllowedDefinitions()
            throws WMWorkflowException;

    /**
     * Starts a process from the interface.
     *
     * @return a string representing the redirect
     */
    String startOneWorkflow() throws WMWorkflowException;

    /**
     * Starts a process with given name from the interface.
     *
     * @param wfid the workflow definition id
     *
     * @return a string representing the redirect
     */
    String startWorkflow(String wfid) throws WMWorkflowException;

    /**
     * Ends the current workflow from the interface.
     *
     * @return a string representing the redirect
     */
    String abandonWorkflow() throws WMWorkflowException;

    /**
     * Returns all workflow instances this document is bound to.
     *
     * @return a collection of workflow instances
     */
    Collection<WMProcessInstance> getWorkflowInstancesForDocument()
            throws WMWorkflowException;

    /**
     * Returns the currently selected workflow.
     *
     * @return
     * @throws WMWorkflowException
     */
    String getWorkflowDefinitionLabel() throws WMWorkflowException;

    void updateCurrentLevelAfterDocumentChanged() throws WMWorkflowException;

    /**
     * Updated the document security.
     * <p>
     * Computation is done based on workflow security policy rules.
     *
     * @throws WMWorkflowException
     */
    void updateDocumentRights() throws WMWorkflowException;

    /**
     * Can the current user start a workflow?
     *
     * @return true if granted / false if not.
     * @throws ClientException
     */
    boolean canStartWorkflow() throws ClientException;

    /**
     * Invalidate workflow context variables.
     *
     * Aimed at being called by <code>@Observer</code>
     */
    void invalidateContextVariables();

    Map<String, String> getAvailableStateTransitionsMap();

    ProcessDocument computeProcessDocument();

    ProcessModel computeReviewModel();

    ProcessModel getReviewModelFor(DocumentModel doc);

    ProcessDocument getProcessDocumentFor(DocumentModel doc);

    /**
     * Seam <code>@Factory</code>.
     *
     * @return
     */
    List<SelectItem> computeWorkitemDirectives();

    String getUserComment();

    void setUserComment(String userComment);

    String getLifeCycleDestinationStateTransition();

    void setLifeCycleDestinationStateTransition(
            String lifeCycleDestinationStateTransition);

    String getWorkflowDefinitionId();

    void setWorkflowDefinitionId(String workflowDefinitionId);

    String getReviewModificationProperty();

    void setReviewModificationProperty(String reviewModificationProperty);

    String getReviewVersioningProperty();

    void setReviewVersioningProperty(String reviewVersioningProperty);

    Map<String, String> getReviewVersioningPropertiesMap();

    Map<String, String> getReviewModificationPropertiesMap();

    Map<String, String> getWorkflowDefinitionsMap();

    /**
     * Workflow startup callback.
     *
     * @return a JSF view id.
     * @throws WMWorkflowException
     */
    String startWorkflowCallback() throws WMWorkflowException;

    /**
     * Workflow has been started ?
     *
     * @return XXX
     * @throws WMWorkflowException
     */
    boolean isWorkflowStarted() throws WMWorkflowException;

    /**
     * Check if at least 2 different reviewers are registered for this workflow.
     * <p>
     * NXP-1967: this rule does not apply anymore in default workflows
     *
     * @return {@code true} if two different reviewers are registered,
     *         {@code false} otherwise.
     * @throws WMWorkflowException
     */
    boolean checkTaskList() throws WMWorkflowException;

}
