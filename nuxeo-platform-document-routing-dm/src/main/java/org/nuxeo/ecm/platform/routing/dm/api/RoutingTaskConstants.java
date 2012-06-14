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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.api;

public class RoutingTaskConstants {

    public static final String TASK_STEP_FACET_NAME = "TaskStep";

    public static final String TASK_STEP_ACTORS_PROPERTY_NAME = "tkst:actors";

    public static final String TASK_STEP_DUE_DATE_PROPERTY_NAME = "tkst:dueDate";

    public static final String TASK_STEP_AUTOMATIC_VALIDATION_PROPERTY_NAME = "tkst:automaticValidation";

    public static final String TASK_STEP_DIRECTIVE_PROPERTY_NAME = "tkst:directive";

    public static final String TASK_STEP_COMMENTS_PROPERTY_NAME = "tkst:comments";

    public static final String ROUTE_TASK_LOCAL_ACL = "routingTask";

    public static final String ROUTING_TASK_ACTORS_KEY = "document.routing.task.actors";

    public enum EvaluationOperators {
        equal, not_equal, less_than, less_or_equal_than, greater_than, greater_or_equal_than
    }

}