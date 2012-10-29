/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Representation of a document change.
 *
 * @author Antoine Taillefer
 */
public class DocumentChange implements Serializable {

    private static final long serialVersionUID = -5697869523880291618L;

    protected String eventId;

    protected String docLifeCycleState;

    protected Calendar eventDate;

    protected String docPath;

    protected String docUuid;

    public DocumentChange(String eventId, String docLifeCycleState,
            Calendar eventDate, String docPath, String docUuid) {
        this.eventId = eventId;
        this.docLifeCycleState = docLifeCycleState;
        this.eventDate = eventDate;
        this.docPath = docPath;
        this.docUuid = docUuid;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDocLifeCycleState() {
        return docLifeCycleState;
    }

    public void setDocLifeCycleState(String docLifeCycleState) {
        this.docLifeCycleState = docLifeCycleState;
    }

    public Calendar getEventDate() {
        return eventDate;
    }

    public void setEventDate(Calendar eventDate) {
        this.eventDate = eventDate;
    }

    public String getDocPath() {
        return docPath;
    }

    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    public String getDocUuid() {
        return docUuid;
    }

    public void setDocUuid(String docUuid) {
        this.docUuid = docUuid;
    }

}
