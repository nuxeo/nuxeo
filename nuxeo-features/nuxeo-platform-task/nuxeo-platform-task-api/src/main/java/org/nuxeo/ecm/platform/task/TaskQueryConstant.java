/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Ulrich
 */
package org.nuxeo.ecm.platform.task;

import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * @since 5.5
 */
public class TaskQueryConstant {

    /**
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String TASK_VARIABLES_WHERE_CLAUSE = "nt:task_variables/*/key = '%s' AND nt:task_variables/*/value = '%s'";

    /**
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String TASK_ACTORS_WHERE_CLAUSE = " nt:actors/* IN (%s) ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_ACTORS_PP = "GET_TASKS_FOR_ACTORS";

    /**
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = '"
            + TaskConstants.TASK_FACET_NAME + "'" + " AND ecm:currentLifeCycleState NOT IN ('ended', 'cancelled')"
            + " AND ecm:isProxy = 0";

    /**
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_ACTORS_QUERY = GET_TASKS_QUERY + " AND nt:actors/* IN (%s) ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_PP = "GET_TASKS_FOR_TARGET_DOCUMENT";

    /**
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_QUERY = GET_TASKS_QUERY
            + " AND nt:targetDocumentId = '%s'";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_PP = "GET_TASKS_FOR_TARGET_DOCUMENTS";

    /**
     * @since 5.8
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_QUERY = GET_TASKS_QUERY
            + " AND ( nt:targetDocumentId = '%s' OR nt:targetDocumentsIds/* IN ('%s') )";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_PP = "GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS";

    /**
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY = GET_TASKS_QUERY
            + " AND nt:targetDocumentId = '%s' AND nt:actors/* IN (%s) ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP = "GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS";

    /**
     * @since 5.8
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_QUERY = GET_TASKS_QUERY
            + " AND ( nt:targetDocumentId = '%s' OR nt:targetDocumentsIds/* IN ('%s') ) AND nt:actors/* IN (%s) ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_PROCESS_PP = "GET_TASKS_FOR_PROCESS";

    /**
     * @since 5.6
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_PROCESS_ID_QUERY = GET_TASKS_QUERY + " AND nt:processId = '%s' ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_PROCESS_AND_ACTORS_PP = "GET_TASKS_FOR_PROCESS_AND_ACTORS";

    /**
     * @since 5.6
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_PROCESS_ID_AND_ACTORS_QUERY = GET_TASKS_QUERY
            + " AND nt:processId = '%s' AND nt:actors/* IN (%s) ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_PROCESS_AND_NODE_PP = "GET_TASKS_FOR_PROCESS_AND_NODE";

    /**
     * @since 5.7
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_PROCESS_ID_AND_NODE_ID_QUERY = GET_TASKS_FOR_PROCESS_ID_QUERY
            + " AND nt:task_variables/*/key = 'nodeId' AND nt:task_variables/*/value =  '%s' ";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_OR_DELEGATED_ACTORS_PP = "GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_OR_DELEGATED_ACTORS";

    /**
     * @since 7.4
     */
    public static final String GET_TASKS_FOR_ACTORS_OR_DELEGATED_ACTORS_PP = "GET_TASKS_FOR_ACTORS_OR_DELEGATED_ACTORS";

    /**
     * @since 5.8
     * @deprecated since 6.0: use page providers instead.
     */
    @Deprecated
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY_OR_DELEGATED_ACTORS_QUERY = GET_TASKS_QUERY
            + " AND ( nt:targetDocumentId = '%s' OR nt:targetDocumentsIds/* IN ('%s') ) AND ( nt:actors/* IN (%s) OR nt:delegatedActors/* IN (%s) ) ";

    /**
     * @deprecated since 6.0: page providers implement this logic instead.
     */
    @Deprecated
    public static String getVariableWhereClause(String key, String value) {
        return String.format(TASK_VARIABLES_WHERE_CLAUSE, key, value);
    }

    /**
     * @deprecated since 6.0: page providers implement this logic instead.
     */
    @Deprecated
    public static String getActorsWhereClause(List<String> actors) {
        return String.format(TASK_ACTORS_WHERE_CLAUSE, formatStringList(actors));
    }

    /**
     * @deprecated since 6.0: page providers implement this logic instead.
     */
    @Deprecated
    public static String formatStringList(List<String> actors) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> actorIterator = actors.iterator();
        while (actorIterator.hasNext()) {
            String userName = actorIterator.next();
            sb.append(NXQL.escapeString(userName));
            if (actorIterator.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

}
