/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * An event listener receives notifications from core components.
 * <p>
 * Notifications are expressed as Event objects. This type of listeners are
 * always invoked synchronously immediately after the event is raised.
 *
 * @see PostCommitEventListener for asynchronous listeners or post commit
 *      listeners
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface EventListener {

    /**
     * Handle the given event. The listener can cancel the event by calling
     * {@link Event#cancel()}
     *
     * @param event the event
     * @throws ClientException
     */
    void handleEvent(Event event) throws ClientException;

}
