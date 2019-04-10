/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.api;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;

/**
 * Wraps the list of {@link GraphNode.TaskInfo} on a {@link GraphNode} to expose
 * in a pretty way information to MVEL scripts.
 *
 * @since 5.7.3
 */
public class TasksInfoWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    protected List<GraphNode.TaskInfo> tasks;

    public TasksInfoWrapper(List<GraphNode.TaskInfo> tasks) {
        this.tasks = tasks;
    }

    public int getNumberEndedWithStatus(String status) {
        int noEndedWithStatus = 0;
        for (GraphNode.TaskInfo taskInfo : tasks) {
            if (taskInfo.getStatus() != null
                    && status.equals(taskInfo.getStatus())) {
                noEndedWithStatus++;
            }
        }
        return noEndedWithStatus;
    }
}
