/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.RoutingTaskService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Button;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Task validators
 *
 * @since 5.6
 *
 */
@Scope(CONVERSATION)
@Name("routingTaskActions")
public class RoutingTaskActionsBean {

    public static final String SUBJECT_PATTERN = "([a-zA-Z_0-9]*(:)[a-zA-Z_0-9]*)";

    private static final Log log = LogFactory.getLog(RoutingTaskActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @RequestParameter("button")
    protected String button;

    protected RoutingTaskService routingTaskService;

    protected Map<String, Serializable> formVariables;

    protected Task currentTask;

    public void validateTaskDueDate(FacesContext context,
            UIComponent component, Object value) {
        final String DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        String messageString = null;
        if (value != null) {
            Date today = null;
            Date dueDate = null;
            try {
                dueDate = dateFormat.parse(dateFormat.format((Date) value));
                today = dateFormat.parse(dateFormat.format(new Date()));
            } catch (ParseException e) {
                messageString = "label.workflow.error.date_parsing";
            }
            if (dueDate.before(today)) {
                messageString = "label.workflow.error.outdated_duedate";
            }
        }

        if (messageString != null) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.workflow.error.outdated_duedate"),
                    null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
        }
    }

    public void validateSubject(FacesContext context, UIComponent component,
            Object value) {
        if (!((value instanceof String) && ((String) value).matches(SUBJECT_PATTERN))) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.document.routing.invalid.subject"),
                    null);
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    public String getTaskLayout(Task task) throws ClientException {
        GraphNode node = getSourceGraphNode(task);
        return node.getTaskLayout();
    }

    public List<Button> getTaskButtons(Task task) throws ClientException {
        GraphNode node = getSourceGraphNode(task);
        if (node == null) {
            return new ArrayList<Button>();
        }
        // TODO evaluate action filter?
        return node.getTaskButtons();
    }

    public String endTask(Task task) throws ClientException {
        // collect form data
        Map<String, Object> data = new HashMap<String, Object>();
        if (formVariables != null) {
            data.putAll(formVariables);
        }
        // add the button name that was clicked
        try {
            getRoutingTaskService().endTask(documentManager, task, data, button);
        } catch (DocumentRouteException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.error.resume.workflow"));
        }
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);
        clear();
        return null;
    }

    private void clear() {
        currentTask = null;
        formVariables = null;
    }

    public Task setCurrentTask(Task task) throws ClientException {
        currentTask = task;
        return currentTask;
    }

    protected GraphNode getSourceGraphNode(Task task) throws ClientException {
        GraphRoute route = getSourceGraphRoute(task);
        String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (route == null || nodeId == null) {
            throw new ClientException(
                    "Can not get the source graph of this task");
        }
        return route.getNode(nodeId);
    }

    protected GraphRoute getSourceGraphRoute(Task task) throws ClientException {
        String routeDocId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (routeDocId == null) {
            throw new ClientException(
                    "Can not get the source node for this task");
        }
        DocumentModel doc = documentManager.getDocument(new IdRef(routeDocId));
        return doc.getAdapter(GraphRoute.class);
    }

    private RoutingTaskService getRoutingTaskService() {
        if (routingTaskService == null) {
            try {
                routingTaskService = Framework.getService(RoutingTaskService.class);
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
        }
        return routingTaskService;
    }

    public Map<String, Serializable> getFormVariables() throws ClientException {
        if (formVariables == null) {
            if (currentTask == null) {
                throw new ClientException("No current task defined");
            }
            GraphNode node = getSourceGraphNode(currentTask);
            GraphRoute route = getSourceGraphRoute(currentTask);
            formVariables = new HashMap<String, Serializable>();
            formVariables.putAll(node.getVariables());
            formVariables.putAll(route.getVariables());
        }
        return formVariables;
    }

    public void setFormVariables(Map<String, Serializable> formVariables) {
        this.formVariables = formVariables;
    }

    /**
     * @since 5.6
     */
    public boolean isRoutingTask(Task task) {
       return task.getDocument().hasFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME);
    }
}
