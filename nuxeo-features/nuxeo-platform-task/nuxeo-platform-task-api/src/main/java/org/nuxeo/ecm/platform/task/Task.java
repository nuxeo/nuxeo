/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Ulrich, Antoine Taillefer
 *
 */

package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.5
 */
public interface Task extends Serializable {

    /**
     * @since 5.6
     */
    String TASK_PROVIDER_KEY = "taskProviderId";

    DocumentModel getDocument();

    String getId();

    List<String> getActors();

    String getInitiator();

    String getName();

    /**
     * @since 5.6
     */
    String getType();

    /**
     * @since 5.6
     */
    String getProcessId();

    /**
     * @since 7.4
     */
    String getProcessName();

    String getDescription();

    String getDirective();

    List<TaskComment> getComments();

    String getVariable(String key);

    Date getDueDate();

    Date getCreated();

    Boolean isCancelled();

    Boolean isOpened();

    Boolean hasEnded();

    Boolean isAccepted();

    Map<String, String> getVariables();

    void setActors(List<String> actors);

    void setInitiator(String initiator);

    void setName(String name);

    /**
     * @since 5.6
     */
    void setType(String type);

    /**
     * @since 5.6
     */
    void setProcessId(String processId);

    /**
     * @since 7.4
     */
    void setProcessName(String processName);

    void setDescription(String description);

    void setDirective(String directive);

    void setVariable(String key, String value);

    void setDueDate(Date dueDate);

    void setCreated(Date created);

    void setAccepted(Boolean accepted);

    void setVariables(Map<String, String> variables);

    void addComment(String author, String text);

    void cancel(CoreSession coreSession);

    void end(CoreSession coreSession);

    enum TaskVariableName {
        needi18n, taskType
    };

    /**
     * @since 5.8
     */
    List<String> getDelegatedActors();

    /**
     * @since 5.8
     */
    void setDelegatedActors(List<String> delegatedActors);

    /**
     * @since 5.8
     */
    List<String> getTargetDocumentsIds();

    /**
     * The first id on the list is also set as 'targetDocumentId'
     *
     * @since 5.8
     */
    void setTargetDocumentsIds(List<String> ids);
}
