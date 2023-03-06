/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.task.TaskConstants.TASK_PROCESS_ID_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.task.TaskConstants.TASK_TYPE_NAME;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener that deletes orphan Tasks.
 *
 * @since 11.1
 * @since deprecated since 2023, tasks clean up is done by @{link DocumentRouteOrphanedListener}
 */
@Deprecated
public class DocumentRouteDeletedListener implements PostCommitEventListener {

    protected static final String QUERY_GET_TASKS_RELATED_TO_DOCUMENT_ROUTE = "SELECT * FROM " + TASK_TYPE_NAME
            + " WHERE " + TASK_PROCESS_ID_PROPERTY_NAME + " = '%s'";

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel docModel = context.getSourceDocument();
            if (DOCUMENT_ROUTE_DOCUMENT_TYPE.equals(docModel.getType())) {
                deleteOrphanTasks(context.getCoreSession(), docModel.getId());
            }
        }
    }

    /**
     * Deletes all tasks whose process id matches the given DocumentRoute id.
     *
     * @since 11.1
     */
    protected void deleteOrphanTasks(CoreSession session, String id) {
        String query = String.format(QUERY_GET_TASKS_RELATED_TO_DOCUMENT_ROUTE, id);
        session.queryProjection(query, 0, 0)
               .stream()
               .map(m -> m.get(ECM_UUID))
               .map(taskId -> new IdRef((String) taskId))
               .forEach(session::removeDocument);
    }

}
