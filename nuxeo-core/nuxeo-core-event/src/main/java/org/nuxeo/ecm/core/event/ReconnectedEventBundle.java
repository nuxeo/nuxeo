/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Because {@link EventBundle} can be processed asynchronously, they can be executed:
 * <ul>
 * <li>in a different security context
 * <li>with a different {@link CoreSession}
 * </ul>
 *
 * This interface is used to mark Bundles that supports this kind of processing.
 * This basically means:
 * <ul>
 * <li>Create a JAAS session via {@link org.nuxeo.runtime.api.Framework#login()}
 * <li>Create a new usage {@link CoreSession}
 * <li>refetch any {@link EventContext} args / properties according to new session
 * <li>provide cleanup method
 * </ul>
 *
 * @author tiry
 */
public interface ReconnectedEventBundle extends EventBundle {

    /**
     * Manage cleanup after processing.
     */
    void disconnect();

    /**
     * Marker for Bundles coming from JMS.
     */
    boolean comesFromJMS();

}
