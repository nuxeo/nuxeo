/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * @since 5.5
 */
public class TaskConstants {

    public static final String TASK_USERS_PROPERTY_NAME = "nt:actors";

    public static final String TASK_INITIATOR_PROPERTY_NAME = "nt:initiator";

    public static final String TASK_DESCRIPTION_PROPERTY_NAME = "dublincore:description";

    public static final String TASK_DIRECTIVE_PROPERTY_NAME = "nt:directive";

    public static final String TASK_COMMENTS_PROPERTY_NAME = "nt:taskComments";

    public static final String TASK_COMMENT_AUTHOR_KEY = "author";

    public static final String TASK_COMMENT_TEXT_KEY = "text";

    public static final String TASK_COMMENT_CREATION_DATE_KEY = "creationDate";

    public static final String TASK_DUE_DATE_PROPERTY_NAME = "nt:dueDate";

    public static final String TASK_CREATED_PROPERTY_NAME = "dublincore:created";

    public static final String TASK_VARIABLES_PROPERTY_NAME = "nt:task_variables";

    public static final String TASK_NAME_PROPERTY_NAME = "nt:name";

    /**
     * @since 5.6
     */
    public static final String TASK_TYPE_PROPERTY_NAME = "nt:type";

    /**
     * @since 5.6
     */
    public static final String TASK_PROCESS_ID_PROPERTY_NAME = "nt:processId";

    /**
     * @since 7.4
     */
    public static final String TASK_PROCESS_NAME_PROPERTY_NAME = "nt:processName";

    public static final String TASK_ACCEPTED_PROPERTY_NAME = "nt:accepted";

    public static final String TASK_CANCELLED_LIFE_CYCLE_STATE = "cancelled";

    public static final String TASK_OPENED_LIFE_CYCLE_STATE = "opened";

    public static final String TASK_ENDED_LIFE_CYCLE_STATE = "ended";

    public static final String TASK_CANCEL_LIFE_CYCLE_TRANSITION = "cancel";

    public static final String TASK_END_LIFE_CYCLE_TRANSITION = "end";

    public static final String TASK_TYPE_NAME = "TaskDoc";

    public static final String TASK_ROOT_TYPE_NAME = "TaskRoot";

    public static final String TASK_FACET_NAME = "Task";

    /**
     * @since 5.7.3
     */
    public static final String TASK_DELEGATED_ACTORS_PROPERTY_NAME = "nt:delegatedActors";

    /**
     * @since 5.8
     */
    public static final String TASK_TARGET_DOCUMENTS_IDS_PROPERTY_NAME = "nt:targetDocumentsIds";

}
