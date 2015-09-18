/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    /**
     * @deprecated
     * @since 5.8, getTargetDocumentsIds() should be used instead
     */
    String getTargetDocumentId();

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

    /**
     * @deprecated
     * @since 5.8, setTargetDocumentsIds(List<String> ids) should be used instead
     */
    void setTargetDocumentId(String targetDocumentId);

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
