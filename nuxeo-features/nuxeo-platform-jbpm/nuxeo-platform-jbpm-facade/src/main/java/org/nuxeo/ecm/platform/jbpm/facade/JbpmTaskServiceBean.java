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
package org.nuxeo.ecm.platform.jbpm.facade;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmRuntimeException;
import org.nuxeo.runtime.api.Framework;

/**
 * EJB facade for the {@link JbpmTaskService}
 * 
 * @author Anahide Tchertchian
 */
@Stateless
@Local(JbpmTaskServiceLocal.class)
@Remote(JbpmTaskService.class)
public class JbpmTaskServiceBean implements JbpmTaskServiceLocal {

    private static final long serialVersionUID = 1L;

    private JbpmTaskService service;

    @PostConstruct
    public void postConstruct() {
        try {
            service = Framework.getLocalService(JbpmTaskService.class);
        } catch (Exception e) {
            throw new NuxeoJbpmRuntimeException(e);
        }
    }

    public void acceptTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment) throws NuxeoJbpmException {
        service.acceptTask(coreSession, principal, task, comment);
    }

    public boolean canEndTask(NuxeoPrincipal principal, TaskInstance task)
            throws NuxeoJbpmException {
        return service.canEndTask(principal, task);
    }

    public void createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, Serializable> taskVariables) throws NuxeoJbpmException {
        service.createTask(coreSession, principal, document, taskName,
                prefixedActorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables);
    }

    public void endTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment, String eventName,
            boolean isValidated) throws NuxeoJbpmException {
        service.endTask(coreSession, principal, task, comment, eventName,
                isValidated);
    }

    public void rejectTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment) throws NuxeoJbpmException {
        service.rejectTask(coreSession, principal, task, comment);
    }

}
