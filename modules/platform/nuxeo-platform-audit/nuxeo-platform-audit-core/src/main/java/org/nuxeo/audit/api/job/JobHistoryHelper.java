/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.api.job;

import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryBuilder;
import org.nuxeo.audit.api.Route;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple helper class to trace job execution using the Audit Service.
 *
 * @author Thierry Delprat
 */
public class JobHistoryHelper {

    public static final String JOB_STARTED_SUFFIX = "Started";

    public static final String JOB_ENDED_SUFFIX = "Ended";

    public static final String JOB_FAILED_SUFFIX = "Failed";

    protected String jobName;

    protected final String jobStartedEventId;

    protected final String jobEndedEventId;

    protected final String jobFailedEventId;

    public JobHistoryHelper(String jobName) {
        this.jobName = jobName;

        jobStartedEventId = jobName + JOB_STARTED_SUFFIX;
        jobEndedEventId = jobName + JOB_ENDED_SUFFIX;
        jobFailedEventId = jobName + JOB_FAILED_SUFFIX;
    }

    protected LogEntry getNewLogEntry(String eventId) {
        return getNewLogEntryBuilder(eventId).build();
    }

    protected LogEntryBuilder getNewLogEntryBuilder(String eventId) {
        return LogEntry.builder(eventId, new Date()).category(jobName).principalName(SecurityConstants.SYSTEM_USERNAME);
    }

    protected AuditBackend getAuditBackend() {
        return Framework.getService(AuditService.class).getAuditBackend(DEFAULT_AUDIT_BACKEND);
    }

    /**
     * Logs an event for Job startup.
     */
    public void logJobStarted() {
        LogEntry entry = getNewLogEntry(jobStartedEventId);
        List<LogEntry> entries = new ArrayList<>();
        entries.add(entry);
        Framework.getService(AuditRouter.class)
                 .routeToBackends(entries, List.of(Route.allEventsTo(DEFAULT_AUDIT_BACKEND)));
    }

    /**
     * Logs an event for a successful Job completion.
     */
    public void logJobEnded() {
        LogEntry entry = getNewLogEntry(jobEndedEventId);
        List<LogEntry> entries = new ArrayList<>();
        entries.add(entry);
        Framework.getService(AuditRouter.class)
                 .routeToBackends(entries, List.of(Route.allEventsTo(DEFAULT_AUDIT_BACKEND)));
    }

    /**
     * Logs an event for a failed Job execution.
     */
    public void logJobFailed(String errMessage) {
        LogEntry entry = getNewLogEntryBuilder(jobFailedEventId).comment(errMessage).build();
        List<LogEntry> entries = new ArrayList<>();
        entries.add(entry);
        Framework.getService(AuditRouter.class)
                 .routeToBackends(entries, List.of(Route.allEventsTo(DEFAULT_AUDIT_BACKEND)));
    }

    protected Date getLastRunWithStatus(String status) {
        String query = "from LogEntry log where log.eventId='%s' AND log.category='%s' ORDER BY log.eventDate DESC".formatted(
                status, jobName);

        List<?> result = getAuditBackend().nativeQuery(query, 1, 1);

        if (!result.isEmpty()) {
            var entry = (LogEntry) result.getFirst();
            return entry.getEventDate();
        }

        return null;
    }

    /**
     * Gets the last date the Job was successfully run.
     */
    public Date getLastSuccessfulRun() {
        return getLastRunWithStatus(jobEndedEventId);
    }

    /**
     * Gets the last date the Job was failed.
     */
    public Date getLastFailedRun() {
        return getLastRunWithStatus(jobFailedEventId);
    }

    /**
     * Gets the last date the Job was started.
     */
    public Date getLastStarted() {
        return getLastRunWithStatus(jobStartedEventId);
    }

}
