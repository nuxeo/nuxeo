/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api;

/**
 * Holds life cycle related constants that are generic enough to be available at
 * a core level.
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
     * Event for a document undeleted by the user. Triggers an async listener
     * that undeletes its children too.
     */
    static final String DOCUMENT_UNDELETED = "documentUndeleted";

    /**
     * Key in context map to specify initial lifecycle state on document
     * creation.
     */
    static final String INITIAL_LIFECYCLE_STATE_OPTION_NAME = "initialLifecycleState";

}
