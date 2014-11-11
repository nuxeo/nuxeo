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
 * $Id: WorkflowConstants.java 24294 2007-08-27 16:02:23Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.api.common;

/**
 * Workflow constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
// XXX: Maybe we will need to split this up and indirect to get those
// information.
public final class WorkflowConstants {

    // Workflow path

    public static final String WORKFLOW_INSTANCE_STATUS_ACTIVE = "ACTIVE";

    public static final String WORKFLOW_INSTANCE_STATUS_INACTIVE = "INACTIVE";

    // Workflow path

    public static final String WORKFLOW_PATH_STATUS_ACTIVE = "ACTIVE";

    public static final String WORKFLOW_PATH_STATUS_INACTIVE = "INACTIVE";

    // Workflow activityDefinition type

    public static final String WORKFLOW_NODE_TYPE_START_NODE = "START_NODE";

    public static final String WORKFLOW_NODE_TYPE_WORKFLOW_NODE = "WORKLFOW_NODE";

    public static final String TASK_NODE_TYPE_ID = "TaskNode";

    // Workflow task

    public static final String WORKFLOW_TASK_PROP_DIRECTIVE = "WORKLOW_TASK_PROP_DIRECTIVE";

    public static final String WORKFLOW_TASK_PROP_DUE_DATE = "WORKLOW_TASk_PROP_DUE_DATE";

    public static final String WORKFLOW_TASK_PROP_COMMENT = "WORKLOW_TASk_PROP_COMMENT";

    public static final String WORKFLOW_TASK_PROP_ORDER = "WORKLOW_TASk_PROP_ORDER";

    public static final String WORKFLOW_TASK_PROP_REJECTED = "WORKLOW_TASk_PROP_REJECTED";

    /** Mostly for UI. */
    public static final String WORKFLOW_USER_TASK_ASSIGNMENT_TYPE_ASSIGNED = "ASSIGNED_TASK";

    /** Mostly for UI. */
    public static final String WORKFLOW_USER_TASK_ASSIGNMENT_TYPE_POOLED = "POOLED_TASK";

    // Process variables
    public static final String WORKFLOW_CREATOR = "author";

    public static final String WORKFLOW_PARTICIPANT = "participant";

    public static final String WORKFLOW_REVIEWER = "reviewer";

    public static final String WORKFLOW_TASK_ASSIGNEE = "taskAssignee";

    public static final String WORKFLOW_DIRECTIVES = "workflowDirectives";

    public static final String WORKFLOW_STARTED_FLAG = "workflowStartedFlag";

    /**
     * Used when a process enters a review. We use this transition to bring the
     * content to the review state.
     */
    public static final String LIFE_CYCLE_TRANSITION_TO_REVIEW_STATE = "LifeCycleStateDuringReview";

    /**
     * Used when a process ends without life cycle update. The content goes back
     * to its old state through this transition.
     *
     * @see org.nuxeo.ecm.workflow.handlers
     */
    public static final String LIFE_CYCLE_TRANSITION_TO_FORMER_STATE = "LifeCycleTransitionToFormerState";

    /**
     * Used in case of reviews that change the life cycle of the document. The
     * content goes back to a destination state through this transition.
     */
    public static final String LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE = "LifeCycleTransitionToDestination";

    public static final String LIFE_CYCLE_STATE_DESTINATION = "LifeCycleStateDestinationState";

    public static final String DOCUMENT_MODIFICATION_POLICY = "documentModificationPolicy";

    public static final String DOCUMENT_REF = "documentRef";

    public static final String DOCUMENT_LOCATION_URI = "documentLocationURI";

    public static final String WORKLFOW_REVIEW_TYPE = "workflow_review_type";

    public static final String WORKFLOW_REVIEW_TYPE_PARALLEL = "workflowDocumentReviewTypeParallel";

    public static final String WORKFLOW_REVIEW_TYPE_SERIE = "workflowDocumentReviewTypeSerie";

    public static final String WORKFLOW_REVIEW_LEVEL = "workflowReviewLevel";

    public static final String WORKFLOW_FORMER_REVIEW_LEVEL = "workflowFormerReviewLevel";

    /**
     * Used by the workflow engine as a key within the process variable map.
     * <p>
     * Shouldn't be needed by third party code outside from the workflow engine
     * though.
     */
    public static final String DOCUMENT_VERSIONING_POLICY = "documentVersioningPolicy";

    // Constant utility class.
    private WorkflowConstants() {
    }

}
