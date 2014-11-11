/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Jbpm list filter on task names
 *
 * @author Anahide Tchertchian
 */
public class TaskListFilter implements JbpmListFilter {

    private static final long serialVersionUID = 1L;

    final List<String> taskNames;

    public TaskListFilter(String... taskNames) {
        this.taskNames = new ArrayList<String>();
        this.taskNames.addAll(Arrays.asList(taskNames));
    }

    public TaskListFilter(List<String> taskNames) {
        this.taskNames = taskNames;
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> filter(JbpmContext jbpmContext,
            DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
        ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();
        if (taskNames != null && !taskNames.isEmpty()) {
            for (T t : list) {
                TaskInstance task = (TaskInstance) t;
                if (taskNames.contains(task.getName())) {
                    result.add(task);
                }
            }
        }
        return (ArrayList<T>) result;
    }
}
