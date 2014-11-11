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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Because {@link EventBundle} can be processed asynchronously, they can be
 * executed:
 * <ul>
 * <li>in a different security context
 * <li>with a different {@link CoreSession}
 * </ul>
 * This interface is used to mark Bundles that supports this kind of
 * processing. This basically means:
 * <ul>
 * <li>Create a JAAS session via
 * {@link org.nuxeo.runtime.api.Framework#login()}
 * <li>Create a new usage {@link CoreSession}
 * <li>refetch any {@link EventContext} args / properties according to new
 * session
 * <li>provide cleanup method
 * </ul>
 *
 * @author tiry
 */
public interface ReconnectedEventBundle extends EventBundle {

    /**
     * Marker to pass and set to true in document models context data when
     * passing it in event properties, to avoid refetching it when
     * reconnecting.
     */
    public static final String SKIP_REFETCH_DOCUMENT_CONTEXT_KEY = "skipRefetchDocument";

    /**
     * Manage cleanup after processing.
     */
    void disconnect();

    /**
     * Marker for Bundles coming from JMS.
     */
    boolean comesFromJMS();

}
