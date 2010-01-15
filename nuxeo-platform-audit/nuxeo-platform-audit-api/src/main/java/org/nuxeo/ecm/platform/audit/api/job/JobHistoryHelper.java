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
 * Simple helper class to trace Job execution using Audit Service
 *
 * @author Thierry Delprat
 *
 */
public class JobHistoryHelper {

    protected AuditReader reader = null;
    protected AuditLogger logger = null;

    protected String jobName = null;

    public static final String JOB_START = "exportStarted";
    public static final String JOB_END = "exportCompleted";
    public static final String JOB_FAIL = "exportFailed";

    public JobHistoryHelper(String jobName) {
        this.jobName=jobName;
    }

    protected LogEntry getNewLogEntry() {
        LogEntry entry = new LogEntry();
        entry.setCategory(jobName);
        entry.setPrincipalName(SecurityConstants.SYSTEM_USERNAME);
        entry.setEventDate(new Date());
        return entry;
    }

    protected AuditLogger getLogger() throws Exception {
        if (logger==null) {
            logger = Framework.getService(AuditLogger.class);
        }
        return logger;
    }

    /**
     * Log an event for Job startup
     *
     * @throws Exception
     */
    public void logJobStarted() throws Exception {

        LogEntry entry = getNewLogEntry();
        entry.setEventId(JOB_START);
        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        getLogger().addLogEntries(entries);
    }

    /**
     * Log an event for a successful Job completion
     * @throws Exception
     */
    public void logJobEnded() throws Exception {
        LogEntry entry = getNewLogEntry();
        entry.setEventId(JOB_END);
        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        getLogger().addLogEntries(entries);
    }

    /**
     * Log an event for a failed Job execution
     *
     * @param errMessage
     * @throws Exception
     */
    public void logJobFailed(String errMessage) throws Exception {
        LogEntry entry = getNewLogEntry();
        entry.setEventId(JOB_FAIL);
        entry.setComment(errMessage);
        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        getLogger().addLogEntries(entries);
    }

    protected Date getLastRunWithStatus(String status) throws Exception {

        AuditReader reader = Framework.getService(AuditReader.class);

        StringBuffer query = new StringBuffer("from LogEntry log where log.eventId=");
        query.append("'");
        query.append(status);
        query.append("' AND log.category='");
        query.append(jobName);
        query.append("'  ORDER BY log.eventDate DESC");

        List result = reader.nativeQuery(query.toString(), 1, 1);

        if (result.size()!=0) {
            LogEntry entry = (LogEntry) result.get(0);
            return entry.getEventDate();
        }

        return null;
    }

    /**
     * Get last date the Job was successfully run
     *
     * @return
     * @throws Exception
     */
    public Date getLastSucessfulRun() throws Exception {
        return getLastRunWithStatus(JOB_END);
    }

    /**
     * Get last date the Job was failed
     *
     * @return
     * @throws Exception
     */
    public Date getLastFailedRun() throws Exception {
        return getLastRunWithStatus(JOB_FAIL);
    }

    /**
     * Get last date the Job was started
     *
     * @return
     * @throws Exception
     */
    public Date getLastStarted() throws Exception {
        return getLastRunWithStatus(JOB_START);
    }

}
