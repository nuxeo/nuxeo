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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;

/**
 * Interface for adding audit logs.
 *
 * @author tiry
 */
public interface AuditLogger {

    /**
     * Create a new LogEntry instance.
     * @return
     */
    LogEntry newLogEntry();

    /**
     * Create a new ExtendedInfo instance
     * @return
     */
    ExtendedInfo newExtendedInfo(Serializable value);

    /**
     * Adds given log entries.
     *
     * @param entries the list of log entries.
     */
    void addLogEntries(List<LogEntry> entries);

    /**
     * Logs an Event.
     */
    void logEvent(Event event) throws AuditException;

    /**
     * Logs a bundle of events
     */
    void logEvents(EventBundle eventBundle) throws AuditException;

}
