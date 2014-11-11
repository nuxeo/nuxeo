/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 */
package org.nuxeo.ecm.platform.task;

import java.util.Iterator;
import java.util.List;

/**
 * @since 5.5
 */
public class TaskQueryConstant {

    public static final String TASK_VARIABLES_WHERE_CLAUSE = "nt:task_variables/*/key = '%s' AND nt:task_variables/*/value = '%s'";

    public static final String TASK_ACTORS_WHERE_CLAUSE = " nt:actors IN (%s) ";

    public static final String GET_TASKS_QUERY = "SELECT * FROM "
            + TaskConstants.TASK_TYPE_NAME
            + " WHERE (ecm:currentLifeCycleState != 'ended' AND ecm:currentLifeCycleState != 'cancelled') ";

    public static final String GET_TASKS_FOR_ACTORS_QUERY = GET_TASKS_QUERY
            + " AND nt:actors IN (%s) ";

    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_QUERY = GET_TASKS_QUERY
            + " AND nt:targetDocumentId = '%s'";

    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY = GET_TASKS_QUERY
            + " AND nt:targetDocumentId = '%s' AND nt:actors IN (%s) ";

    public static String getVariableWhereClause(String key, String value) {
        return String.format(TASK_VARIABLES_WHERE_CLAUSE, key, value);
    }

    public static String getActorsWhereClause(List<String> actors) {
        return String.format(TASK_ACTORS_WHERE_CLAUSE, formatStringList(actors));
    }

    public static String formatStringList(List<String> actors) {
        StringBuffer sb = new StringBuffer();
        Iterator<String> actorIterator = actors.iterator();
        while (actorIterator.hasNext()) {
            String userName = actorIterator.next();
            sb.append('\'');
            sb.append(userName.replaceAll("'", "\\\\'"));
            sb.append('\'');
            if (actorIterator.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

}
