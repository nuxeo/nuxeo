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

package org.nuxeo.ecm.platform.audit.api.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
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

    protected AuditLogger logger;

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

    protected LogEntry getNewLogEntry() {
        LogEntry entry = new LogEntry();
        entry.setCategory(jobName);
        entry.setPrincipalName(SecurityConstants.SYSTEM_USERNAME);
        entry.setEventDate(new Date());
        return entry;
    }

    protected AuditLogger getLogger() throws Exception {
        if (logger == null) {
            logger = Framework.getService(AuditLogger.class);
        }
        return logger;
    }

    /**
     * Logs an event for Job startup.
     */
    public void logJobStarted() throws Exception {
        LogEntry entry = getNewLogEntry();
        entry.setEventId(jobStartedEventId);
        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        getLogger().addLogEntries(entries);
    }

    /**
     * Logs an event for a successful Job completion.
     */
    public void logJobEnded() throws Exception {
        LogEntry entry = getNewLogEntry();
        entry.setEventId(jobEndedEventId);
        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        getLogger().addLogEntries(entries);
    }

    /**
     * Logs an event for a failed Job execution.
     */
    public void logJobFailed(String errMessage) throws Exception {
        LogEntry entry = getNewLogEntry();
        entry.setEventId(jobFailedEventId);
        entry.setComment(errMessage);
        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        getLogger().addLogEntries(entries);
    }

    protected Date getLastRunWithStatus(String status) throws Exception {
        AuditReader reader = Framework.getService(AuditReader.class);

        StringBuilder query = new StringBuilder("from LogEntry log where log.eventId=");
        query.append("'");
        query.append(status);
        query.append("' AND log.category='");
        query.append(jobName);
        query.append("'  ORDER BY log.eventDate DESC");

        List result = reader.nativeQuery(query.toString(), 1, 1);

        if (!result.isEmpty()) {
            LogEntry entry = (LogEntry) result.get(0);
            return entry.getEventDate();
        }

        return null;
    }

    /**
     * Gets the last date the Job was successfully run.
     */
    public Date getLastSuccessfulRun() throws Exception {
        return getLastRunWithStatus(jobEndedEventId);
    }

    /**
     * Gets the last date the Job was failed.
     */
    public Date getLastFailedRun() throws Exception {
        return getLastRunWithStatus(jobFailedEventId);
    }

    /**
     * Gets the last date the Job was started.
     */
    public Date getLastStarted() throws Exception {
        return getLastRunWithStatus(jobStartedEventId);
    }

}
