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

    public static final String DELETED_STATE = "deleted";

    public static final String TRANSITION_EVENT = "lifecycle_transition_event";

    public static final String TRANSTION_EVENT_OPTION_FROM = "from";

    public static final String TRANSTION_EVENT_OPTION_TO = "to";

    public static final String TRANSTION_EVENT_OPTION_TRANSITION = "transition";

}
