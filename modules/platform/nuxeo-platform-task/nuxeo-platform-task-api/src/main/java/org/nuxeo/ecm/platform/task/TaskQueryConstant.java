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
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_ACTORS_PP = "GET_TASKS_FOR_ACTORS";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_PP = "GET_TASKS_FOR_TARGET_DOCUMENT";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_PP = "GET_TASKS_FOR_TARGET_DOCUMENTS";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_PP = "GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP = "GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_PROCESS_PP = "GET_TASKS_FOR_PROCESS";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_PROCESS_AND_ACTORS_PP = "GET_TASKS_FOR_PROCESS_AND_ACTORS";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_PROCESS_AND_NODE_PP = "GET_TASKS_FOR_PROCESS_AND_NODE";

    /**
     * @since 6.0
     */
    public static final String GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_OR_DELEGATED_ACTORS_PP = "GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_OR_DELEGATED_ACTORS";

    /**
     * @since 7.4
     */
    public static final String GET_TASKS_FOR_ACTORS_OR_DELEGATED_ACTORS_PP = "GET_TASKS_FOR_ACTORS_OR_DELEGATED_ACTORS";

}
