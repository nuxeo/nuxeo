/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nicolas Ulrich
 */
package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleException;

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
    public void addComment(String author, String text) {
        List<Map<String, Serializable>> existingTasks = getPropertyValue(TaskConstants.TASK_COMMENTS_PROPERTY_NAME);
        if (existingTasks == null) {
            existingTasks = new ArrayList<>();
        }
        existingTasks.add(new TaskComment(author, text));
        setPropertyValue(TaskConstants.TASK_COMMENTS_PROPERTY_NAME, existingTasks);
    }

    @Override
    public void cancel(CoreSession coreSession) {
        followTransition(coreSession, TaskConstants.TASK_CANCEL_LIFE_CYCLE_TRANSITION);
    }

    @Override
    public void end(CoreSession coreSession) {
        followTransition(coreSession, TaskConstants.TASK_END_LIFE_CYCLE_TRANSITION);
    }

    protected void followTransition(CoreSession coreSession, String transition) throws LifeCycleException {
        if (doc.getAllowedStateTransitions().contains(transition)) {
            coreSession.followTransition(doc.getRef(), transition);
        } else {
            throw new LifeCycleException(
                    "Cannot follow transition " + transition + " on the document " + doc.getPathAsString());
        }

    }

    @Override
    public List<String> getActors() {
        return getPropertyValue(TaskConstants.TASK_USERS_PROPERTY_NAME);
    }

    @Override
    public List<TaskComment> getComments() {
        List<Map<String, Serializable>> taskCommentsProperty = getPropertyValue(
                TaskConstants.TASK_COMMENTS_PROPERTY_NAME);
        List<TaskComment> taskComments = new ArrayList<>(taskCommentsProperty.size());
        for (Map<String, Serializable> taskCommentMap : taskCommentsProperty) {
            taskComments.add(new TaskComment(taskCommentMap));
        }
        return taskComments;
    }

    @Override
    public Date getCreated() {
        return getDatePropertyValue(TaskConstants.TASK_CREATED_PROPERTY_NAME);
    }

    protected Date getDatePropertyValue(String propertyName) {
        Calendar cal = (Calendar) doc.getPropertyValue(propertyName);
        if (cal != null) {
            return cal.getTime();
        }
        return null;
    }

    @Override
    public List<String> getDelegatedActors() {
        return getPropertyValue(TaskConstants.TASK_DELEGATED_ACTORS_PROPERTY_NAME);
    }

    @Override
    public String getDescription() {
        return getPropertyValue(TaskConstants.TASK_DESCRIPTION_PROPERTY_NAME);
    }

    @Override
    public String getDirective() {
        return getPropertyValue(TaskConstants.TASK_DIRECTIVE_PROPERTY_NAME);

    }

    @Override
    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public Date getDueDate() {
        return getDatePropertyValue(TaskConstants.TASK_DUE_DATE_PROPERTY_NAME);

    }

    @Override
    public String getId() {
        return doc.getId();
    }

    @Override
    public String getInitiator() {
        return getPropertyValue(TaskConstants.TASK_INITIATOR_PROPERTY_NAME);
    }

    @Override
    public String getName() {
        return getPropertyValue(TaskConstants.TASK_NAME_PROPERTY_NAME);
    }

    @Override
    public String getProcessId() {
        return getPropertyValue(TaskConstants.TASK_PROCESS_ID_PROPERTY_NAME);
    }

    /**
     * @since 7.4
     */
    @Override
    public String getProcessName() {
        return getPropertyValue(TaskConstants.TASK_PROCESS_NAME_PROPERTY_NAME);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyValue(String propertyName) {
        Serializable value = doc.getPropertyValue(propertyName);
        if (value instanceof Object[]) {
            value = new ArrayList<>(Arrays.asList((Object[]) value));
        }
        return (T) value;
    }

    @Override
    public List<String> getTargetDocumentsIds() {
        return getPropertyValue(TaskConstants.TASK_TARGET_DOCUMENTS_IDS_PROPERTY_NAME);
    }

    @Override
    public String getType() {
        return getPropertyValue(TaskConstants.TASK_TYPE_PROPERTY_NAME);
    }

    @Override
    public String getVariable(String key) {
        Map<String, String> variables = getVariables();
        return variables.get(key);
    }

    @Override
    public Map<String, String> getVariables() {
        List<Map<String, String>> variables = getPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME);
        Map<String, String> variableMap = new HashMap<>(variables.size());
        for (Map<String, String> map : variables) {
            variableMap.put(map.get("key"), map.get("value"));
        }
        return variableMap;
    }

    @Override
    public Boolean hasEnded() {
        return TaskConstants.TASK_ENDED_LIFE_CYCLE_STATE.equals(doc.getCurrentLifeCycleState());
    }

    @Override
    public Boolean isAccepted() {
        Boolean isAccepted = getPropertyValue(TaskConstants.TASK_ACCEPTED_PROPERTY_NAME);
        return isAccepted == null ? false : isAccepted;
    }

    @Override
    public Boolean isCancelled() {
        return TaskConstants.TASK_CANCELLED_LIFE_CYCLE_STATE.equals(doc.getCurrentLifeCycleState());
    }

    @Override
    public Boolean isOpened() {
        return TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE.equals(doc.getCurrentLifeCycleState());
    }

    @Override
    public void setAccepted(Boolean accepted) {
        setPropertyValue(TaskConstants.TASK_ACCEPTED_PROPERTY_NAME, accepted);
    }

    @Override
    public void setActors(List<String> users) {
        setPropertyValue(TaskConstants.TASK_USERS_PROPERTY_NAME, users);
    }

    @Override
    public void setCreated(Date created) {
        setPropertyValue(TaskConstants.TASK_CREATED_PROPERTY_NAME, created);
    }

    @Override
    public void setDelegatedActors(List<String> delegatedActors) {
        setPropertyValue(TaskConstants.TASK_DELEGATED_ACTORS_PROPERTY_NAME, delegatedActors);
    }

    @Override
    public void setDescription(String description) {
        setPropertyValue(TaskConstants.TASK_DESCRIPTION_PROPERTY_NAME, description);
    }

    @Override
    public void setDirective(String directive) {
        setPropertyValue(TaskConstants.TASK_DIRECTIVE_PROPERTY_NAME, directive);
    }

    @Override
    public void setDueDate(Date dueDate) {
        setPropertyValue(TaskConstants.TASK_DUE_DATE_PROPERTY_NAME, dueDate);

    }

    @Override
    public void setInitiator(String initiator) {
        setPropertyValue(TaskConstants.TASK_INITIATOR_PROPERTY_NAME, initiator);
    }

    @Override
    public void setName(String name) {
        setPropertyValue(TaskConstants.TASK_NAME_PROPERTY_NAME, name);
    }

    @Override
    public void setProcessId(String processId) {
        setPropertyValue(TaskConstants.TASK_PROCESS_ID_PROPERTY_NAME, processId);
    }

    /**
     * @since 7.4
     */
    @Override
    public void setProcessName(String processName) {
        setPropertyValue(TaskConstants.TASK_PROCESS_NAME_PROPERTY_NAME, processName);
    }

    protected void setPropertyValue(String propertyName, Object value) {
        if (value != null) {
            if (value instanceof Date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime((Date) value);
                doc.setPropertyValue(propertyName, cal);
            } else {
                doc.setPropertyValue(propertyName, (Serializable) value);
            }
        }
    }

    @Override
    public void setTargetDocumentsIds(List<String> ids) {
        setPropertyValue(TaskConstants.TASK_TARGET_DOCUMENTS_IDS_PROPERTY_NAME, ids);
    }

    @Override
    public void setType(String type) {
        setPropertyValue(TaskConstants.TASK_TYPE_PROPERTY_NAME, type);
    }

    @Override
    public void setVariable(String key, String value) {
        List<Map<String, Serializable>> variables = getPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME);
        if (variables == null) {
            variables = new ArrayList<>();
        }
        Map<String, Serializable> variableMap = new HashMap<>(2);
        variableMap.put("key", key);
        variableMap.put("value", value);
        variables.add(variableMap);
        setPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME, variables);
    }

    @Override
    public void setVariables(Map<String, String> variables) {
        List<Map<String, Serializable>> variablesProperty = getPropertyValue(
                TaskConstants.TASK_VARIABLES_PROPERTY_NAME);
        if (variablesProperty == null) {
            variablesProperty = new ArrayList<>();
        }
        Map<String, Serializable> variable;
        for (Entry<String, String> entry : variables.entrySet()) {
            if (entry.getValue() != null) {
                variable = new HashMap<>(2);
                variable.put("key", entry.getKey());
                variable.put("value", entry.getValue());
                variablesProperty.add(variable);
            }
        }
        setPropertyValue(TaskConstants.TASK_VARIABLES_PROPERTY_NAME, variablesProperty);
    }

}
