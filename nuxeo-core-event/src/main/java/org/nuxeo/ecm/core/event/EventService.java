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

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * The event service manage listener registries and notify listeners about core
 * events.
 * <p>
 * The service is able to run in a transactional mode where all events are
 * recorded and fired after the transaction commits in one step as an event
 * bundle.
 * <p>
 * To start a transaction you need to call {@link #startTransaction()} method
 * and after the transaction is committed you need to call {@link #commit()} to
 * fire the event bundle. If the transaction rollbacks, then you need to call
 * {@link #rollback()} to cleanup recorded events.
 * <p>
 * Events are recorded in a thread variable so they are valid only in the
 * current thread.
 * <p>
 * Listeners are of 2 types: {@link EventListener} notified as the event is
 * raised and {@link PostCommitEventListener} notified after the transaction was
 * committed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EventService extends EventProducer {

    /**
     * Add a new event listener.
     * <p>
     * The event listener is described by a {@link EventListenerDescriptor} that
     * may specify a priority. Both types of listeners are registered.
     *
     * @param listener the listener to add
     */
    void addEventListener(EventListenerDescriptor listener);

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove
     */
    void removeEventListener(EventListenerDescriptor listener);

    /**
     * Fires an event given its name and a context.
     *
     * @param name the event name
     * @param context the event context
     * @throws ClientException
     */
    void fireEvent(String name, EventContext context) throws ClientException;

    /**
     * Fires an event.
     * <p>
     * If a transaction was started the event is registered if needed to be send
     * after the transaction commit.
     *
     * @param event the event to fire
     * @throws ClientException
     */
    void fireEvent(Event event) throws ClientException;

    /**
     * Fires all recorded events in a transaction.
     * <p>
     * The events are fired to {@link PostCommitEventListener} listeners. Events
     * are fired in the form of an event bundle.
     *
     * @param event the event bundle
     * @throws ClientException
     */
    void fireEventBundle(EventBundle event) throws ClientException;

    /**
     * Fire an event bundle in asynchronous mode. That is asyncrhounous listeners will be
     * synchronously ru.
     * @param event
     * @throws ClientException
     */
    void fireEventBundleSync(EventBundle event) throws ClientException;


    /**
     * Get the list of the registered event listeners
     * Modification on this list will not modify the internal lists in that {@link EventService}
     * @return the event listeners
     */
    List<EventListener> getEventListeners();

    /**
     * Get the list of the registered post commit event listeners
     * Modification on this list will not modify the internal lists in that {@link EventService}
     * @return the post commit event listeners
     */
    List<PostCommitEventListener> getPostCommitEventListeners();

    /**
     * Notifies that a transaction was started.
     * <p>
     * Any fired events will be recorded until the transaction is terminated
     * either by calling {@link #transactionRollbacked()} either
     * {@link #transactionCommited()()}
     */
    void transactionStarted();

    /**
     * Notifies that the transaction was committed.
     * <p>
     * This will fire the events collected during the transaction in the form of
     * a {@link EventBundle}. After this the recording will stop and recorded
     * events discarded.
     *
     * @throws ClientException
     */
    void transactionCommited() throws ClientException;

    /**
     * Notifies that transaction was rollbacked. This will discard any recorded
     * event.
     */
    void transactionRollbacked();

    /**
     * Tests whether or not a transaction was started.
     *
     * @return true if a transaction was started, false otherwise
     */
    boolean isTransactionStarted();

}
