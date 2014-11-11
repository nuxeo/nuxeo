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
 * $Id: CoreEventListenerService.java 4718 2006-10-24 20:16:29Z janguenot $
 */

package org.nuxeo.ecm.core.listener;

import java.util.Collection;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;

/**
 * Repository listener.
 *
 * @see EventListener
 * @see org.nuxeo.ecm.core.api.event.CoreEvent
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CoreEventListenerService {

    /**
     * Adds a repository event listener.
     *
     * @param listener
     *            the event listener to add
     */
    void addEventListener(EventListener listener);

    /**
     * Removes a repository event listener.
     *
     * @param listener
     *            the event listener to remove
     */
    void removeEventListener(EventListener listener);

    /**
     * Notifies a core event.
     * <p>
     * Event listeners are notified if they are configured to process the given
     * event type. They are notified in the order defined in their settings.
     *
     * @param coreEvent
     *            a coreEvent instance
     */
    void notifyEventListeners(CoreEvent coreEvent);

    /**
     * Fires a command starting event.
     *
     * @param command the command
     */
    void fireOperationStarted(Operation<?> command);

    /**
     * Fires a command termination event.
     *
     * @param command the command that terminated
     */
    void fireOperationTerminated(Operation<?> command);

    /**
     * Returns the collection of event listeners.
     *
     * @return the collection of event listeners
     */
    Collection<EventListener> getEventListeners();

    /**
     * Returns an event listener given its name.
     *
     * @param name
     *            the name of the event listener used a registration time
     * @return the EventListener instance, or null if none found.
     */
    EventListener getEventListenerByName(String name);


    /**
     * Notify post commit listeners about all events raised in the current transaction.
     *  Called by the core session after the current transaction was committed
     */
    void transactionCommited();

    /**
     *  Called by the core session after the current transaction was rollbacked.
     *  Post commit events will be removed
     */
    void transactionRollbacked();

    /**
     * A new transaction started.
     * This will start a new post commit session
     */
    void transactionStarted();

//    /**
//     * Transaction is about to be commited
//     */
//    void transactionAboutToCommit();

}
