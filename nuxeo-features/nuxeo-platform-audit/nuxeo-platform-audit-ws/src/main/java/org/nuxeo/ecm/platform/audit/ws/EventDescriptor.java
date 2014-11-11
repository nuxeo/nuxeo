/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;
import java.util.Date;

public class EventDescriptor implements Serializable {

    private static final long serialVersionUID = 987698679871L;

    private  String eventId;
    private  String eventDate;
    private  String docPath;
    private  String docUUID;
    private  String lifeCycle;

    public EventDescriptor() {
    }

    public EventDescriptor(String eventId, Date eventDate, String docPath, String docUUID, String lifeCycle) {
        this.eventDate = eventDate.toString();
        this.eventId = eventId;
        this.docPath = docPath;
        this.docUUID = docUUID;
        this.lifeCycle=lifeCycle;
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

}
