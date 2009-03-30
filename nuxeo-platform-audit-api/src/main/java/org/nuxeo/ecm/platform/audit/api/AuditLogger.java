/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.audit.api;

import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;

/**
 * Interface for adding audit logs
 *
 * @author tiry
 *
 */
public interface AuditLogger {

    /**
     * Adds given log entries.
     *
     * @param entries the list of log entries.
     * @throws AuditException
     */
    void addLogEntries(List<LogEntry> entries) throws AuditException;


    /**
     * Logs an Event
     *
     * @param event
     * @throws AuditException
     */
    void logEvent(Event event) throws AuditException;

    /**
     * Logs a bundle of events
     *
     * @param eventBundle
     * @throws AuditException
     */
    void logEvents(EventBundle eventBundle) throws AuditException;

}
