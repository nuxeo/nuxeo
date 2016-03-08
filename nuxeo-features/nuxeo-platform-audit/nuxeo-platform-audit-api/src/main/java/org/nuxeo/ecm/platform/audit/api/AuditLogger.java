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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;

/**
 * Interface for adding audit logs.
 *
 * @author tiry
 */
public interface AuditLogger {

    /**
     * Returns the list of auditable event names.
     *
     * @return list of String representing event names.
     */
    Set<String> getAuditableEventNames();

    /**
     * Create a new LogEntry instance.
     *
     * @return
     */
    LogEntry newLogEntry();

    /**
     * Create a new ExtendedInfo instance
     *
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
    void logEvent(Event event);

    /**
     * Logs a bundle of events
     */
    void logEvents(EventBundle eventBundle);

    /**
     *
     * @since 8.2
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;
}
