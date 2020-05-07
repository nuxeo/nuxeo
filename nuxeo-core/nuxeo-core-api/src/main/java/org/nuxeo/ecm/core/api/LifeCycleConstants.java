/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api;

/**
 * Holds life cycle related constants that are generic enough to be available at a core level.
 *
 * @author Anahide Tchertchian
 */
public interface LifeCycleConstants {

    static final String DELETED_STATE = "deleted";

    static final String DELETE_TRANSITION = "delete";

    static final String UNDELETE_TRANSITION = "undelete";

    /**
     * Event for a lifecycle transition.
     */
    static final String TRANSITION_EVENT = "lifecycle_transition_event";

    static final String TRANSTION_EVENT_OPTION_FROM = "from";

    static final String TRANSTION_EVENT_OPTION_TO = "to";

    static final String TRANSTION_EVENT_OPTION_TRANSITION = "transition";

    /**
     * Event for a document undeleted by the user. Triggers an async listener that undeletes its children too.
     *
     * @deprecated since 11.1, technical event used by former trash mechanism
     */
    @Deprecated
    static final String DOCUMENT_UNDELETED = "documentUndeleted";

    /**
     * Key in context map to specify initial lifecycle state on document creation.
     */
    static final String INITIAL_LIFECYCLE_STATE_OPTION_NAME = "initialLifecycleState";

}
