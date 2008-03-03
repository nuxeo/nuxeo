/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id: LifeCycleEventTypes.java 19491 2007-05-27 13:51:18Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

/**
 * Life cycle event types.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class LifeCycleEventTypes {

    public static final String LIFECYCLE_TRANSITION_EVENT = "lifecycle_transition_event";

    public static final String OPTION_NAME_FROM = "from";

    public static final String OPTION_NAME_TO = "to";

    public static final String OPTION_NAME_TRANSITION = "transition";

    // Constant utility class
    private LifeCycleEventTypes() {
    }

}
