/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.listeners;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.actions.ProcessRetentionEventAction;
import org.nuxeo.retention.event.RetentionEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener processing events with a {@link org.nuxeo.retention.event.RetentionEventContext}). The
 * listener schedules a {@link org.nuxeo.retention.actions.ProcessRetentionEventAction} on a query retrieving all
 * the retention rules targeting the listened event.
 *
 * @since 11.1
 */
public class RetentionBusinessEventListener implements EventListener {

    private static final Logger log = LogManager.getLogger(RetentionBusinessEventListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext evtCtx = event.getContext();
        if (evtCtx instanceof RetentionEventContext) {
            String eventName = event.getName();
            log.trace("Proceeding event {}", eventName);
            String eventInput = ((RetentionEventContext) evtCtx).getInput();
            BulkService bulkService = Framework.getService(BulkService.class);
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            StringBuilder query = new StringBuilder(RetentionConstants.ACTIVE_EVENT_BASED_RETENTION_RULES_QUERY);
            query.append(" AND ") //
                 // Only with event name
                 .append(RetentionConstants.STARTING_POINT_EVENT_PROP)
                 .append(" = ")
                 .append(NXQL.escapeString(eventName));
            if (StringUtils.isBlank(eventInput)) {
                query.append(" AND ") //
                     .append(RetentionConstants.STARTING_POINT_EXPRESSION_PROP)
                     .append(" IS NULL");
            } else {
                query.append(" AND ") //
                     .append(RetentionConstants.STARTING_POINT_EXPRESSION_PROP)
                     .append(" = ")
                     .append(NXQL.escapeString(eventInput));
            }
            for (String repositoryName : repositoryService.getRepositoryNames()) {
                BulkCommand command = new BulkCommand.Builder(ProcessRetentionEventAction.ACTION_NAME,
                        query.toString()).user(SecurityConstants.SYSTEM_USERNAME).repository(repositoryName).build();
                bulkService.submit(command);
            }
        }
    }

}
