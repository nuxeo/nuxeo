/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api;

/**
 * Holds life cycle related constants that are generic enough to be available at
 * a core level.
 *
 * @author Anahide Tchertchian
 *
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
    public static final String DOCUMENT_UNDELETED = "documentUndeleted";

}
