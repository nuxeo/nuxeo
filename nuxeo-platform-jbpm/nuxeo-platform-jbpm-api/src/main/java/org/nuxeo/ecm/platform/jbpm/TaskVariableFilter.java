/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.jbpm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Anahide Tchertchian
 */
public class TaskVariableFilter implements JbpmListFilter {

    private static final long serialVersionUID = 1L;

    final Map<String, Serializable> variables;

    public TaskVariableFilter(String variableName, String variableValue) {
        variables = new HashMap<String, Serializable>();
        variables.put(variableName, variableValue);
    }

    public TaskVariableFilter(Map<String, Serializable> variables) {
        this.variables = new HashMap<String, Serializable>();
        if (variables != null) {
            this.variables.putAll(variables);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> filter(JbpmContext jbpmContext,
            DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
        ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();
        if (!variables.isEmpty()) {
            for (T t : list) {
                boolean isOk = true;
                TaskInstance task = (TaskInstance) t;
                for (Map.Entry<String, Serializable> variable : variables.entrySet()) {
                    Object value = task.getVariable(variable.getKey());
                    if (value == null && variable.getValue() == null) {
                        continue;
                    } else if (value != null
                            && value.equals(variable.getValue())) {
                        continue;
                    } else {
                        isOk = false;
                        break;
                    }
                }
                if (isOk) {
                    result.add(task);
                }
            }
        }
        return (ArrayList<T>) result;
    }
}
