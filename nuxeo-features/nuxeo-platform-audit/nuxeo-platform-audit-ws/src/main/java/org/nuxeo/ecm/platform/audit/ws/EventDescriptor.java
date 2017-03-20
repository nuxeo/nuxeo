/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;
import java.util.Date;

import org.nuxeo.ecm.platform.audit.api.LogEntry;

public class EventDescriptor implements Serializable {

    private static final long serialVersionUID = 987698679871L;

    private String eventId;

    private String eventDate;

    private String docPath;

    private String docUUID;

    private String lifeCycle;

    private String repoId;

    public EventDescriptor() {
    }

    public EventDescriptor(LogEntry logEntry) {
        this.eventDate = logEntry.getEventDate().toString();
        this.eventId = logEntry.getEventId();
        this.docPath = logEntry.getDocPath();
        this.docUUID = logEntry.getDocUUID();
        this.lifeCycle = logEntry.getDocLifeCycle();
        this.repoId = logEntry.getRepositoryId();
    }

    /**
     * @deprecated since 5.4.2, use the other constructor
     */
    @Deprecated
    public EventDescriptor(String eventId, Date eventDate, String docPath, String docUUID, String lifeCycle) {
        this.eventDate = eventDate.toString();
        this.eventId = eventId;
        this.docPath = docPath;
        this.docUUID = docUUID;
        this.lifeCycle = lifeCycle;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getDocPath() {
        return docPath;
    }

    public String getDocUUID() {
        return docUUID;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    public void setDocUUID(String docUUID) {
        this.docUUID = docUUID;
    }

    public String getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(String lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public String getRepositoryId() {
        return repoId;
    }

    public void setRepositoryId(String repoId) {
        this.repoId = repoId;
    }

}
