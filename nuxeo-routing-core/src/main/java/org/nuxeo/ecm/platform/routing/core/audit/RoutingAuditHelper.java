/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.audit;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper method related to Routing Audit.
 *
 * @since 7.4
 */
public final class RoutingAuditHelper {

    public static final String TIME_SINCE_WF_STARTED = "timeSinceWfStarted";

    public static final String TIME_SINCE_TASK_STARTED = "timeSinceTaskStarted";

    public static final String TASK_ACTOR = "taskActor";

    public static final String WORKFLOW_INITATIOR = "workflowInitiator";

    public static final String WORKFLOW_VARIABLES = "workflowVariables";

    /**
     * Query the audit for an entry of the Routing category matching the given event and returns the time elapsed  since it is recorded.
     *
     * @param event
     * @param elementId
     * @return
     * @since 7.4
     */
    public static long computeElapsedTime(DocumentRoutingConstants.Events event, String elementId) {
        Logs logs = Framework.getService(Logs.class);
        if (logs != null && StringUtils.isNotBlank(elementId)) {
            Map<String, FilterMapEntry> filterMap = new HashMap<String, FilterMapEntry>();

            FilterMapEntry categoryFilterMapEntry = new FilterMapEntry();
            categoryFilterMapEntry.setColumnName(BuiltinLogEntryData.LOG_CATEGORY);
            categoryFilterMapEntry.setOperator("=");
            categoryFilterMapEntry.setQueryParameterName(BuiltinLogEntryData.LOG_CATEGORY);
            categoryFilterMapEntry.setObject(DocumentRoutingConstants.ROUTING_CATEGORY);
            filterMap.put(BuiltinLogEntryData.LOG_CATEGORY, categoryFilterMapEntry);

            FilterMapEntry eventIdFilterMapEntry = new FilterMapEntry();
            eventIdFilterMapEntry.setColumnName(BuiltinLogEntryData.LOG_EVENT_ID);
            eventIdFilterMapEntry.setOperator("=");
            eventIdFilterMapEntry.setQueryParameterName(BuiltinLogEntryData.LOG_EVENT_ID);
            eventIdFilterMapEntry.setObject(event.name());
            filterMap.put(BuiltinLogEntryData.LOG_EVENT_ID, eventIdFilterMapEntry);

            List<LogEntry> logEntries = logs.getLogEntriesFor(elementId, filterMap, true);
            for (LogEntry logEntry : logEntries) {
                Date start = logEntry.getEventDate();
                return new Date().getTime() - start.getTime();
            }
        }
        return -1;
    }

    /**
     * Return the elapsed time since a workflow had started.
     *
     * @param workflowInstanceId the workflowInstanceId
     * @return elapsed time in ms
     * @since 7.4
     */
    public static long computeDurationSinceWfStarted(String workflowInstanceId) {
        return RoutingAuditHelper.computeElapsedTime(DocumentRoutingConstants.Events.afterWorkflowStarted, workflowInstanceId);
    }

    /**
     * Return the elapsed time since a task had started.
     *
     * @param taskId the taskId
     * @return elapsed time in ms
     * @since 7.4
     */
    public static long computeDurationSinceTaskStarted(String taskId) {
        return RoutingAuditHelper.computeElapsedTime(DocumentRoutingConstants.Events.afterWorkflowTaskCreated, taskId);
    }
}
