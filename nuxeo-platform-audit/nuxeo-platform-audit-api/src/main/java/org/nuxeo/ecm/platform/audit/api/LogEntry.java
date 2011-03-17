/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: LogEntryImpl.java 30195 2008-02-14 21:53:04Z tdelprat $
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Log entry.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
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
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    Date getEventDate();

    void setEventDate(Date eventDate);

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the doc UUID related to the log entry.
     */
    String getDocUUID();

    void setDocUUID(String docUUID);

    void setDocUUID(DocumentRef docRef);

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the doc path related to the log entry.
     */
    String getDocPath();

    void setDocPath(String docPath);

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to
     * any document.
     *
     * @return the doc type related to the log entry.
     */
    String getDocType();

    void setDocType(String docType);

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any
     * ways.
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
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    String getDocLifeCycle();

    void setDocLifeCycle(String docLifeCycle);

    Map<String, ExtendedInfo> getExtendedInfos();

    void setExtendedInfos(Map<String, ExtendedInfo> infos);

}
