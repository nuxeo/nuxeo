/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Anguenot
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.audit.api.comment.UIAuditComment;

/**
 * Log entry.
 */
public interface LogEntry extends Serializable {

    /**
     * @return the log identifier
     */
    long getId();

    void setId(long id);

    /**
     * Returns the name of the principal who originated the log entry.
     *
     * @return the name of the principal who originated the log entry
     */
    String getPrincipalName();

    void setPrincipalName(String principalName);

    /**
     * Returns the identifier of the event that originated the log entry.
     *
     * @return the identifier of the event that originated the log entry
     */
    String getEventId();

    void setEventId(String eventId);

    /**
     * @return the date of the log insertion: this up to max transaction timeout later than eventDate. This date is
     *         useful for services such as Nuxeo Drive that need fine grained incremental near-monotonic access to the
     *         audit log.
     * @since 5.7
     * @since 5.6-HF16
     */
    Date getLogDate();

    void setLogDate(Date logDate);

    /**
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    Date getEventDate();

    void setEventDate(Date eventDate);

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc UUID related to the log entry.
     */
    String getDocUUID();

    void setDocUUID(String docUUID);

    void setDocUUID(DocumentRef docRef);

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc path related to the log entry.
     */
    String getDocPath();

    void setDocPath(String docPath);

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to any document.
     *
     * @return the doc type related to the log entry.
     */
    String getDocType();

    void setDocType(String docType);

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any ways.
     *
     * @return the category for this log entry.
     */
    String getCategory();

    void setCategory(String category);

    /**
     * Returns the associated comment for this log entry.
     *
     * @return the associated comment for this log entry
     */
    String getComment();

    void setComment(String comment);

    /**
     * Return the life cycle if the document related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    String getDocLifeCycle();

    void setDocLifeCycle(String docLifeCycle);

    /**
     * Returns the repository id related to the log entry.
     *
     * @return the repository id
     */
    String getRepositoryId();

    void setRepositoryId(String repositoryId);

    Map<String, ExtendedInfo> getExtendedInfos();

    void setExtendedInfos(Map<String, ExtendedInfo> infos);

    /**
     * Return the comment preprocessed to be ready for display. (extract info about linked documents) Only available
     * when accessed via the entry is fetched via the {@link AuditPageProvider}
     *
     * @return
     */
    UIAuditComment getPreprocessedComment();

    void setPreprocessedComment(UIAuditComment uiComment);
}
