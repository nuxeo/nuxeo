/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: LogEntryImpl.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.platform.audit.api;

import java.util.Date;

import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * Base log entry implementation
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LogEntryBase implements LogEntry {

    private static final long serialVersionUID = 0L;

    protected long id;

    protected String eventId;

    protected Date eventDate;

    protected String docUUID;

    protected String docPath;

    protected String docType;

    protected String principalName;

    protected String comment;

    protected String category;

    protected String docLifeCycle;

    public LogEntryBase() {
    }

    public LogEntryBase(DocumentMessage doc) {
        setEventId(doc.getEventId());
        setDocUUID(doc.getId());
        setDocPath(doc.getPathAsString());
        setDocType(doc.getType());
        setEventDate(doc.getEventDate());
        setPrincipalName(doc.getPrincipalName());
        setCategory(doc.getCategory());
        setComment(doc.getComment());
        setDocLifeCycle(doc.getDocCurrentLifeCycle());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;

    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date creationDate) {
        this.eventDate = creationDate;
    }

    public String getDocUUID() {
        return docUUID;
    }

    public void setDocUUID(String docUUID) {
        this.docUUID = docUUID;
    }

    public String getDocPath() {
        return docPath;
    }

    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDocLifeCycle() {
        return docLifeCycle;
    }

    public void setDocLifeCycle(String docLifeCycle) {
        this.docLifeCycle = docLifeCycle;
    }

}
