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
 * $Id: WMWorkItemState.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.util.Arrays;
import java.util.List;

/**
 * Workflow task states.
 * <p>
 *
 * <verbatim>
 *
 * CREATED -> STARTED < --- > SUSPENDED
 *              |              |      |
 *              |             \ /     |
 *              --------->  CLOSED
 *              |                     |
 *              <--------->  REJECTED  |
 *              |                     |
 *              |                    \ /
 *              --------->      CANCELLED
 * </verbatim>
 *
 * <p>
 *
 * From a business point of view:
 *
 * <ul>
 *    <li><code>rejected</code> is used when someone reject an assigned task</li>
 *    <li><code>cancelled</code> is used when a task is removed.</li>
 *    <li><code>closed</code> is used when a user approve the task</li>
 *    <li>For the others it is meaningful enough to avoid explanations.</li>
 * </ul>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class WMWorkItemState {

    public static final String WORKFLOW_TASK_STATE_CREATED = "WORKFLOW_TASK_STATE_CREATED";

    public static final String WORKFLOW_TASK_STATE_ALL = "WORKFLOW_TASK_STATE_ALL";

    public static final String WORKFLOW_TASK_STATE_STARTED = "WORKFLOW_TASK_STATE_STARTED";

    public static final String WORKFLOW_TASK_STATE_CLOSED = "WORKFLOW_TASK_STATE_CLOSED";

    public static final String WORKFLOW_TASK_STATE_SUSPENDED = "WORKFLOW_TASK_STATE_SUSPENDED";

    public static final String WORKFLOW_TASK_STATE_CANCELLED = "WORKFLOW_TASK_STATE_CANCELLED";

    public static final String WORKFLOW_TASK_STATE_REJECTED = "WORKFLOW_TASK_STATE_REJECTED";

    private static final String[] WORKFLOW_TASK_STATE_ACTIVE = {
            WORKFLOW_TASK_STATE_CREATED, WORKFLOW_TASK_STATE_ALL,
            WORKFLOW_TASK_STATE_SUSPENDED, WORKFLOW_TASK_STATE_REJECTED, };

    private static final String[] WORKFLOW_TASK_STATE_NOT_ACTIVE = {
            WORKFLOW_TASK_STATE_CLOSED, WORKFLOW_TASK_STATE_CANCELLED, };

    // This is an utility class.
    private WMWorkItemState() { }

    /**
     * Returns the list of task states considered as active.
     *
     * @return the list of task states considered as active
     */
    public static List<String> getActiveStates() {
        return Arrays.asList(WORKFLOW_TASK_STATE_ACTIVE);
    }

    /**
     * Returns the list of task states considered as not active.
     *
     * @return the list of task states considered as not active
     */
    public static List<String> getNotActiveStates() {
        return Arrays.asList(WORKFLOW_TASK_STATE_NOT_ACTIVE);
    }

}
