/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 */
package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * @since 5.5
 */
public class TaskImpl implements Task {

    private static final long serialVersionUID = 1L;

    private DocumentModel doc;

    public TaskImpl(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public String getId() {
        return doc.getId();
    }

    @Override
    public String getTargetDocumentId() {
        return getPropertyValue(TaskConstants.TASK_TARGET_DOCUMENT_ID_PROPERTY_NAME);
    }

    @Override
    public List<String> getActors() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_USERS_PROPERTY_NAME);
    }

    @Override
    public String getInitiator() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_INITIATOR_PROPERTY_NAME);
    }

    @Override
    public String getDescription() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_DESCRIPTION_PROPERTY_NAME);
    }

    @Override
    public String getDirective() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_DIRECTIVE_PROPERTY_NAME);

    }

    @Override
    public List<TaskComment> getComments() throws ClientException {
        List<Map<String, Serializable>> taskCommentsProperty = getPropertyValue(TaskConstants.TASK_COMMENTS_PROPERTY_NAME);
        List<TaskComment> taskComments = new ArrayList<TaskComment>(
                taskCommentsProperty.size());
        for (Map<String, Serializable> taskCommentMap : taskCommentsProperty) {
            taskComments.add(new TaskComment(taskCommentMap));
        }
        return taskComments;
    }

    @Override
    public String getName() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_NAME_PROPERTY_NAME);
    }

    @Override
    public String getType() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_TYPE_PROPERTY_NAME);
    }

    @Override
    public String getProcessId() throws ClientException {
        return getPropertyValue(TaskConstants.TASK_PROCESS_ID_PROPERTY_NAME);
    }

    @Override
    public Date getCreated() throws ClientException {
        return getDatePropertyValue(TaskConstants.TASK_CREATED_PROPERTY_NAME);
    }

    @Override
    public Boolean isCancelled() throws ClientException {
        return TaskConstants.TASK_CANCELLED_LIFE_CYCLE_STATE.equals(doc.getCurrentLifeCycleState());
    }

    @Override
    public Boolean hasEnded() throws ClientException {
        return TaskConstants.TASK_ENDED_LIFE_CYCLE_STATE.equals(doc.getCurrentLifeCycleState());
    }

    @Override
    public Boolean isOpened() throws ClientException {
        return TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE.equals(doc.getCurrentLifeCycleState());
    }

    @Override
    public Boolean isAccepted() throws ClientException {
        Boolean isAccepted = getPropertyValue(TaskConstants.TASK_ACCEPTED_PROPERTY_NAME);
        return isAccepted == null ? false : isAccepted;
    }

    @Override
    public String getVariable(String key) throws ClientException {
        Map<String, String> variables = getVariables();
        return variables.get(key);
    }

    @Override
    public Date getDueDate() throws ClientException {
        return getDatePropertyValue(TaskConstants.TASK_DUE_DATE_PROPERTY_NAME);

    }

    @Override
    public Map<String, String> getVariables() throws ClientException {
        List<Map<String, String>> variables = getPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME);
        Map<String, String> variableMap = new HashMap<String, String>(
                variables.size());
        for (Map<String, String> map : variables) {
            variableMap.put(map.get("key"), map.get("value"));
        }
        return variableMap;
    }

    @Override
    public void setActors(List<String> users) throws ClientException {
        setPropertyValue(TaskConstants.TASK_USERS_PROPERTY_NAME, users);
    }

    @Override
    public void setTargetDocumentId(String targetDocId) throws ClientException {
        setPropertyValue(TaskConstants.TASK_TARGET_DOCUMENT_ID_PROPERTY_NAME,
                targetDocId);
    }

    @Override
    public void setDescription(String description) throws ClientException {
        setPropertyValue(TaskConstants.TASK_DESCRIPTION_PROPERTY_NAME,
                description);
    }

    @Override
    public void setDirective(String directive) throws ClientException {
        setPropertyValue(TaskConstants.TASK_DIRECTIVE_PROPERTY_NAME, directive);
    }

    @Override
    public void setName(String name) throws ClientException {
        setPropertyValue(TaskConstants.TASK_NAME_PROPERTY_NAME, name);
    }

    @Override
    public void setProcessId(String processId) throws ClientException {
        setPropertyValue(TaskConstants.TASK_PROCESS_ID_PROPERTY_NAME, processId);
    }

    @Override
    public void setType(String type) throws ClientException {
        setPropertyValue(TaskConstants.TASK_TYPE_PROPERTY_NAME, type);
    }

    @Override
    public void setInitiator(String initiator) throws ClientException {
        setPropertyValue(TaskConstants.TASK_INITIATOR_PROPERTY_NAME, initiator);
    }

    @Override
    public void setDueDate(Date dueDate) throws ClientException {
        setPropertyValue(TaskConstants.TASK_DUE_DATE_PROPERTY_NAME, dueDate);

    }

    @Override
    public void setCreated(Date created) throws ClientException {
        setPropertyValue(TaskConstants.TASK_CREATED_PROPERTY_NAME, created);
    }

    @Override
    public void cancel(CoreSession coreSession) throws ClientException {
        followTransition(coreSession,
                TaskConstants.TASK_CANCEL_LIFE_CYCLE_TRANSITION);
    }

    @Override
    public void end(CoreSession coreSession) throws ClientException {
        followTransition(coreSession,
                TaskConstants.TASK_END_LIFE_CYCLE_TRANSITION);
    }

    protected void followTransition(CoreSession coreSession, String transition)
            throws ClientException {
        if (doc.getAllowedStateTransitions().contains(transition)) {
            coreSession.followTransition(doc.getRef(), transition);
        } else {
            throw new ClientRuntimeException("Cannot follow transition "
                    + transition + " on the document " + doc.getPathAsString());
        }

    }

    @Override
    public void setAccepted(Boolean accepted) throws ClientException {
        setPropertyValue(TaskConstants.TASK_ACCEPTED_PROPERTY_NAME, accepted);
    }

    @Override
    public void setVariables(Map<String, String> variables)
            throws ClientException {
        List<Map<String, Serializable>> variablesProperty = getPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME);
        if (variablesProperty == null) {
            variablesProperty = new ArrayList<Map<String, Serializable>>();
        }
        Map<String, Serializable> variable;
        for (String key : variables.keySet()) {
            Object value = variables.get(key);
            if (value instanceof String) {
                variable = new HashMap<String, Serializable>(1);
                variable.put("key", key);
                variable.put("value", (Serializable) value);
                variablesProperty.add(variable);
            }
        }
        setPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME,
                variablesProperty);
    }

    @Override
    public void setVariable(String key, String value) throws ClientException {
        List<Map<String, Serializable>> variables = getPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME);
        if (variables == null) {
            variables = new ArrayList<Map<String, Serializable>>();
        }
        Map<String, Serializable> variableMap = new HashMap<String, Serializable>(
                2);
        variableMap.put("key", key);
        variableMap.put("value", value);
        variables.add(variableMap);
        setPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME, variables);
    }

    @Override
    public void addComment(String author, String text) throws ClientException {
        List<Map<String, Serializable>> existingTasks = getPropertyValue(TaskConstants.TASK_COMMENTS_PROPERTY_NAME);
        if (existingTasks == null) {
            existingTasks = new ArrayList<Map<String, Serializable>>();
        }
        existingTasks.add(new TaskComment(author, text));
        setPropertyValue(TaskConstants.TASK_COMMENTS_PROPERTY_NAME,
                existingTasks);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyValue(String propertyName) {
        try {
            return (T) doc.getPropertyValue(propertyName);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected Date getDatePropertyValue(String propertyName) {
        try {
            Calendar cal = (Calendar) doc.getPropertyValue(propertyName);
            if (cal != null) {
                return cal.getTime();
            }
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return null;
    }

    protected void setPropertyValue(String propertyName, Object value) {
        try {
            if (value != null) {
                if (value instanceof Date) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime((Date) value);
                    doc.setPropertyValue(propertyName, cal);
                } else {
                    doc.setPropertyValue(propertyName, (Serializable) value);
                }
            }
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }
}
