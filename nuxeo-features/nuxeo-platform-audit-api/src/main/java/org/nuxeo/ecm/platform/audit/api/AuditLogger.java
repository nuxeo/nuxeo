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
