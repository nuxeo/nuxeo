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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.facade;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmActorsListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmRuntimeException;
import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy.Action;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
@Stateless
@Local(JbpmService.class)
@Remote(JbpmService.class)
public class JbpmServiceBean implements JbpmService {

    private JbpmService service;

    @PostConstruct
    public void postConstruct() {
        try {
            service = Framework.getLocalService(JbpmService.class);
        } catch (Exception e) {
            throw new NuxeoJbpmRuntimeException(e);
        }
    }

    public ProcessInstance createProcessInstance(NuxeoPrincipal user,
            String processInstanceName, DocumentModel dm,
            Map<String, Serializable> variables,
            Map<String, Serializable> transientVariables)
            throws NuxeoJbpmException {
        return service.createProcessInstance(user, processInstanceName, dm,
                variables, transientVariables);
    }

    public void endProcessInstance(Long processId) throws NuxeoJbpmException {
        service.endProcessInstance(processId);
    }

    public void endTask(Long taskInstanceId, String transition,
            Map<String, Serializable> taskVariables,
            Map<String, Serializable> variables,
            Map<String, Serializable> transientVariables, NuxeoPrincipal principal)
            throws NuxeoJbpmException {
        service.endTask(taskInstanceId, transition, taskVariables, variables,
                transientVariables, principal);
    }

    public Serializable executeJbpmOperation(JbpmOperation operation)
            throws NuxeoJbpmException {
        return service.executeJbpmOperation(operation);
    }

    public List<String> getAvailableTransitions(Long taskInstanceId,
            NuxeoPrincipal principal) throws NuxeoJbpmException {
        return service.getAvailableTransitions(taskInstanceId, principal);
    }

    public JbpmConfiguration getConfiguration() {
        return service.getConfiguration();
    }

    public List<ProcessInstance> getCurrentProcessInstances(
            NuxeoPrincipal principal, JbpmListFilter filter)
            throws NuxeoJbpmException {
        return service.getCurrentProcessInstances(principal, filter);
    }

    public List<TaskInstance> getCurrentTaskInstances(
            NuxeoPrincipal currentUser, JbpmListFilter filter)
            throws NuxeoJbpmException {
        return service.getCurrentTaskInstances(currentUser, filter);
    }

    public DocumentModel getDocumentModel(TaskInstance ti, NuxeoPrincipal user)
            throws NuxeoJbpmException {
        return service.getDocumentModel(ti, user);
    }

    public DocumentModel getDocumentModel(ProcessInstance pi,
            NuxeoPrincipal user) throws NuxeoJbpmException {
        return service.getDocumentModel(pi, user);
    }

    public ProcessInstance getProcessInstance(Long processInstanceId)
            throws NuxeoJbpmException {
        return service.getProcessInstance(processInstanceId);
    }

    public List<ProcessInstance> getProcessInstances(DocumentModel dm,
            NuxeoPrincipal user, JbpmListFilter jbpmListFilter)
            throws NuxeoJbpmException {
        return service.getProcessInstances(dm, user, jbpmListFilter);
    }

    public List<TaskInstance> getTaskInstances(Long processInstanceId,
            NuxeoPrincipal principal, JbpmListFilter filter)
            throws NuxeoJbpmException {
        return service.getTaskInstances(processInstanceId, principal, filter);
    }

    public List<TaskInstance> getTaskInstances(DocumentModel dm,
            NuxeoPrincipal user, JbpmListFilter jbpmListFilter)
            throws NuxeoJbpmException {
        return service.getTaskInstances(dm, user, jbpmListFilter);
    }

    public ProcessInstance persistProcessInstance(ProcessInstance pi)
            throws NuxeoJbpmException {
        return service.persistProcessInstance(pi);
    }

    public void saveTaskInstances(List<TaskInstance> taskInstances)
            throws NuxeoJbpmException {
        service.saveTaskInstances(taskInstances);
    }

    public List<ProcessDefinition> getProcessDefinitions(NuxeoPrincipal user,
            DocumentModel dm, JbpmListFilter filter) throws NuxeoJbpmException {
        return service.getProcessDefinitions(user, dm, filter);
    }

    public Map<String, List<String>> getTypeFilterConfiguration() {
        return service.getTypeFilterConfiguration();
    }

    public void deleteProcessInstance(NuxeoPrincipal principal, Long processId)
            throws NuxeoJbpmException {
        service.deleteProcessInstance(principal, processId);
    }

    public Boolean getPermission(ProcessInstance pi, Action action,
            DocumentModel dm, NuxeoPrincipal principal)
            throws NuxeoJbpmException {
        return service.getPermission(pi, action, dm, principal);
    }

    public ProcessDefinition getProcessDefinitionByName(String name)
            throws NuxeoJbpmException {
        return service.getProcessDefinitionByName(name);
    }

    public List<ProcessDefinition> getProcessDefinitionsByType(String type)
            throws NuxeoJbpmException {
        return service.getProcessDefinitionsByType(type);
    }

    public List<ProcessInstance> getCurrentProcessInstances(
            List<String> actors, JbpmActorsListFilter filter)
            throws NuxeoJbpmException {
        return service.getCurrentProcessInstances(actors, filter);
    }

    public List<TaskInstance> getCurrentTaskInstances(List<String> actors,
            JbpmActorsListFilter filter) throws NuxeoJbpmException {
        return service.getCurrentTaskInstances(actors, filter);
    }

    public List<TaskInstance> getTaskInstances(DocumentModel dm,
            List<String> actors, JbpmActorsListFilter filter)
            throws NuxeoJbpmException {
        return service.getTaskInstances(dm, actors, filter);
    }

}
