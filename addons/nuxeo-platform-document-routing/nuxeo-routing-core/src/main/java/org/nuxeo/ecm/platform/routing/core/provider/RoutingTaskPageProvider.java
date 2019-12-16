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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.routing.core.provider;

import static java.util.stream.Collectors.toList;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider to retrieve workflow tasks. Two parameters are taken into account:
 *
 * <pre>
 * - the actorId at index 0 of {@link RoutingTaskPageProvider#getParameters()}
 * - workflowInstanceId at index 1 of {@link RoutingTaskPageProvider#getParameters()}
 * </pre>
 *
 * Results are ordered by due date ascending.
 *
 * @since 11.1
 */
public class RoutingTaskPageProvider extends AbstractPageProvider<Task> {

    private static final long serialVersionUID = 1L;

    @Override
    public List<Task> getCurrentPage() {
        Object[] parameters = getParameters();
        if (parameters == null || parameters.length != 2) {
            throw new IllegalStateException("Invalid parameters: " + Arrays.toString(parameters));
        }
        String actorId = (String) parameters[0];
        String workflowInstanceId = (String) parameters[1];
        CoreSession session = getCoreSession();
        StringBuilder query = new StringBuilder(
                String.format("SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s'",
                        TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE));
        if (StringUtils.isNotBlank(actorId)) {
            List<String> actors = new ArrayList<>();
            UserManager userManager = Framework.getService(UserManager.class);
            NuxeoPrincipal principal = userManager.getPrincipal(actorId);
            if (principal != null) {
                for (String actor : TaskActorsHelper.getTaskActors(principal)) {
                    actors.add(NXQL.escapeString(actor));
                }
            } else {
                actors.add(NXQL.escapeString(actorId));
            }
            String actorsParam = String.join(", ", actors);
            query.append(String.format(" AND (nt:actors/* IN (%s) OR nt:delegatedActors/* IN (%s))", actorsParam,
                    actorsParam));
        }
        if (StringUtils.isNotBlank(workflowInstanceId)) {
            query.append(String.format(" AND nt:processId = %s", NXQL.escapeString(workflowInstanceId)));
        }
        query.append(String.format(" ORDER BY %s ASC", TaskConstants.TASK_DUE_DATE_PROPERTY_NAME));

        PartialList<Map<String, Serializable>> results = session.queryProjection(query.toString(), getPageSize(),
                getCurrentPageOffset(), true);
        setResultsCount(results.totalSize());
        return results.stream()
                      .map(map -> session.getDocument(new IdRef((String) map.get(ECM_UUID))))
                      .map(doc -> doc.getAdapter(Task.class))
                      .collect(toList());
    }

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new NuxeoException("cannot find core session");
        }
        return coreSession;
    }

}
