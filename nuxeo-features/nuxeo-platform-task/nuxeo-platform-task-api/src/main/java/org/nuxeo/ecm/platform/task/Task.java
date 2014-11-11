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

import org.nuxeo.ecm.core.api.ClientException;
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

    List<String> getActors() throws ClientException;

    String getInitiator() throws ClientException;

    String getName() throws ClientException;

    /**
     * @since 5.6
     */
    String getType() throws ClientException;

    /**
     * @since 5.6
     */
    String getProcessId() throws ClientException;

    String getDescription() throws ClientException;

    String getDirective() throws ClientException;

    List<TaskComment> getComments() throws ClientException;

    String getVariable(String key) throws ClientException;

    Date getDueDate() throws ClientException;

    Date getCreated() throws ClientException;

    Boolean isCancelled() throws ClientException;

    Boolean isOpened() throws ClientException;

    Boolean hasEnded() throws ClientException;

    Boolean isAccepted() throws ClientException;

    Map<String, String> getVariables() throws ClientException;

    void setActors(List<String> actors) throws ClientException;

    void setInitiator(String initiator) throws ClientException;

    /**
     * @deprecated
     * @since 5.8, setTargetDocumentsIds(List<String> ids) should be used
     *        instead
     */
    void setTargetDocumentId(String targetDocumentId) throws ClientException;

    void setName(String name) throws ClientException;

    /**
     * @since 5.6
     */
    void setType(String type) throws ClientException;

    /**
     * @since 5.6
     */
    void setProcessId(String processId) throws ClientException;

    void setDescription(String description) throws ClientException;

    void setDirective(String directive) throws ClientException;

    void setVariable(String key, String value) throws ClientException;

    void setDueDate(Date dueDate) throws ClientException;

    void setCreated(Date created) throws ClientException;

    void setAccepted(Boolean accepted) throws ClientException;

    void setVariables(Map<String, String> variables) throws ClientException;

    void addComment(String author, String text) throws ClientException;

    void cancel(CoreSession coreSession) throws ClientException;

    void end(CoreSession coreSession) throws ClientException;

    enum TaskVariableName {
        needi18n, taskType
    };

    /**
     * @since 5.8
     */
    List<String> getDelegatedActors() throws ClientException;

    /**
     * @since 5.8
     */
    void setDelegatedActors(List<String> delegatedActors);

    /**
     * @since 5.8
     */
    List<String> getTargetDocumentsIds() throws ClientException;

    /**
     * The first id on the list is also set as 'targetDocumentId'
     *
     * @since 5.8
     */
    void setTargetDocumentsIds(List<String> ids) throws ClientException;
}