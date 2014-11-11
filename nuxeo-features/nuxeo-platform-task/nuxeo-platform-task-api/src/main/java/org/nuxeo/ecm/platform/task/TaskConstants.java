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

/**
 * @since 5.5
 */
public class TaskConstants {

    public static final String TASK_USERS_PROPERTY_NAME = "task:actors";

    public static final String TASK_INITIATOR_PROPERTY_NAME = "task:initiator";

    public static final String TASK_TARGET_DOCUMENT_ID_PROPERTY_NAME = "task:targetDocumentId";

    public static final String TASK_DESCRIPTION_PROPERTY_NAME = "dublincore:description";

    public static final String TASK_DIRECTIVE_PROPERTY_NAME = "task:directive";

    public static final String TASK_COMMENTS_PROPERTY_NAME = "task:taskComments";

    public static final String TASK_COMMENT_AUTHOR_KEY = "author";

    public static final String TASK_COMMENT_TEXT_KEY = "text";

    public static final String TASK_COMMENT_CREATION_DATE_KEY = "creationDate";

    public static final String TASK_DUE_DATE_PROPERTY_NAME = "task:dueDate";

    public static final String TASK_CREATED_PROPERTY_NAME = "dublincore:created";

    public static final String TASK_VARIABLES_PROPERTY_NAME = "task:task_variables";

    public static final String TASK_NAME_PROPERTY_NAME = "task:name";

    public static final String TASK_ACCEPTED_PROPERTY_NAME = "task:accepted";

    public static final String TASK_CANCELLED_LIFE_CYCLE_STATE = "cancelled";

    public static final String TASK_OPENED_LIFE_CYCLE_STATE = "opened";

    public static final String TASK_ENDED_LIFE_CYCLE_STATE = "ended";

    public static final String TASK_CANCEL_LIFE_CYCLE_TRANSITION = "cancel";

    public static final String TASK_END_LIFE_CYCLE_TRANSITION = "end";

    public static final String TASK_TYPE_NAME = "TaskDoc";

    public static final String TASK_ROOT_TYPE_NAME = "TaskRoot";

    public static final String TASK_FACET_NAME = "Task";

}
