/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.audit;

import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
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
     * Query the audit for an entry of the Routing category matching the given event and returns the time elapsed since
     * it is recorded.
     *
     * @since 7.4
     */
    public static long computeElapsedTime(DocumentRoutingConstants.Events event, String elementId) {
        Logs logs = Framework.getService(Logs.class);
        if (logs != null && StringUtils.isNotBlank(elementId)) {
            List<LogEntry> logEntries = logs.queryLogs(
                    new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, elementId))
                                           .and(Predicates.eq(LOG_CATEGORY, DocumentRoutingConstants.ROUTING_CATEGORY))
                                           .and(Predicates.eq(LOG_EVENT_ID, event.name()))
                                           .defaultOrder()
                                           .limit(1));
            if (logEntries.size() > 0) {
                LogEntry logEntry = logEntries.get(0);
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
        return RoutingAuditHelper.computeElapsedTime(DocumentRoutingConstants.Events.afterWorkflowStarted,
                workflowInstanceId);
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
