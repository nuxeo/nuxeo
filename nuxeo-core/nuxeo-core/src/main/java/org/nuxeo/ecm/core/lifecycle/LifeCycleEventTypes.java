/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @deprecated use {@link LifeCycleConstants} instead
 */
@Deprecated
public final class LifeCycleEventTypes {

    public static final String LIFECYCLE_TRANSITION_EVENT = "lifecycle_transition_event";

    public static final String OPTION_NAME_FROM = "from";

    public static final String OPTION_NAME_TO = "to";

    public static final String OPTION_NAME_TRANSITION = "transition";

    // Constant utility class
    private LifeCycleEventTypes() {
    }

}
