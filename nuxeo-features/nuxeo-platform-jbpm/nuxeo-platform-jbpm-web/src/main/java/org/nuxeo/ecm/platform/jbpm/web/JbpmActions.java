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
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;

/**
 * @author Anahide Tchertchian
 */
public interface JbpmActions extends Serializable {

    boolean getCanCreateProcess() throws ClientException;

    boolean getCanManageProcess() throws ClientException;

    boolean getCanManageParticipants() throws ClientException;

    boolean getCanEndTask(TaskInstance taskInstance) throws ClientException;

    String createProcessInstance(NuxeoPrincipal principal, String pd,
            DocumentModel dm, String endLifeCycle) throws ClientException;

    ProcessInstance getCurrentProcess() throws ClientException;

    String getCurrentProcessInitiator() throws ClientException;

    List<TaskInstance> getCurrentTasks(String... taskNames)
            throws ClientException;

    ArrayList<VirtualTaskInstance> getCurrentVirtualTasks()
            throws ClientException;

    boolean getShowAddVirtualTaskForm() throws ClientException;

    void toggleShowAddVirtualTaskForm(ActionEvent event) throws ClientException;

    VirtualTaskInstance getNewVirtualTask();

    String addNewVirtualTask() throws ClientException;

    String removeVirtualTask(int index) throws ClientException;

    String moveUpVirtualTask(int index) throws ClientException;

    String moveDownVirtualTask(int index) throws ClientException;

    /**
     * Returns the list of allowed life cycle state transitions for given
     * document.
     */
    List<String> getAllowedStateTransitions(DocumentRef ref)
            throws ClientException;

    String getUserComment();

    void setUserComment(String comment);

    void validateTaskDueDate(FacesContext context, UIComponent component,
            Object value);

    boolean isProcessStarted(String startTaskName) throws ClientException;

    String startProcess(String startTaskName) throws ClientException;

    String validateTask(TaskInstance taskInstance, String transition)
            throws ClientException;

    String rejectTask(TaskInstance taskInstance, String transition)
            throws ClientException;

    String abandonCurrentProcess() throws ClientException;

    void resetCurrentData();

    /**
     * Returns true if given document type has process definitions attached to
     * it.
     *
     * @since 5.5
     * @param documentType the document type name
     */
    boolean hasProcessDefinitions(String documentType);

}
