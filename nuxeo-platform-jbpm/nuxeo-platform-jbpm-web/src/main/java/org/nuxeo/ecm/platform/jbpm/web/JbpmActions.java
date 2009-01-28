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

package org.nuxeo.ecm.platform.jbpm.web;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Anahide Tchertchian
 *
 */
public interface JbpmActions extends Serializable {

    boolean getCanStartProcess() throws ClientException;

    boolean getCanManageProcess() throws ClientException;

    boolean getCanEndTask(TaskInstance taskInstance) throws ClientException;

    String createProcessInstance(NuxeoPrincipal principal, String pd,
            DocumentModel dm, String endLifeCycle) throws ClientException;

    ProcessInstance getCurrentProcess() throws ClientException;

    String getCurrentProcessInitiator() throws ClientException;

    List<TaskInstance> getCurrentTasks(String... taskNames)
            throws ClientException;

    String getVirtualTasksLayoutMode() throws ClientException;

    List<VirtualTaskInstance> getCurrentVirtualTasks() throws ClientException;

    VirtualTaskInstance getNewVirtualTask() throws ClientException;

    String addNewVirtualTask() throws ClientException;

    String removeVirtualTask(int index) throws ClientException;

    String moveUpVirtualTask(int index) throws ClientException;

    String moveDownVirtualTask(int index) throws ClientException;

    String getUserComment() throws ClientException;

    void setUserComment(String comment) throws ClientException;

    void validateTaskDueDate(FacesContext context, UIComponent component,
            Object value);

    String endTask(TaskInstance taskInstance, String transition,
            Map<String, Serializable> variables,
            Map<String, Serializable> transientVariables)
            throws ClientException;

    String abandonCurrentProcess() throws ClientException;

    void resetCurrentData() throws ClientException;

}
