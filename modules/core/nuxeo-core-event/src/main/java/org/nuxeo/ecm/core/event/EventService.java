/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event;

import java.util.List;

import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.stream.DomainEventProducer;

/**
 * The event service manages listener registries and notifies listeners about core events.
 * <p>
 * The service is able to run in a transactional mode where all events are recorded and fired after the transaction
 * commits in one step as an event bundle.
 * <p>
 * To start a transaction, the framework calls the {@code transactionStarted()} method, and at transaction commit the
 * framework calls {@code transactionCommitted()} to fire the event bundle. Upon rollback the framework calls
 * {@code transactionRolledback()} to clean up recorded events.
 * <p>
 * Events are recorded in a thread variable so they are valid only in the current thread.
 * <p>
 * An event marked {@link Event#isInline()} is dispatched immediately, otherwise it is recorded in a thread-based bundle
 * of current events. If no transaction was started, an event marked {@link Event#isCommitEvent()} is used to flush the
 * event bundle to its listeners, otherwise the transaction commit does the flush.
 * <p>
 * Listeners are of two types: {@link EventListener} notified as the event is raised and {@link PostCommitEventListener}
 * notified after the transaction was committed.
 */
public interface EventService extends EventProducer {

    /**
     * Adds a new event listener. Used by the framework.
     * <p>
     * The event listener is described by a {@link EventListenerDescriptor} that may specify a priority. Both types of
     * listeners (immediate and post-commit) are registered.
     *
     * @param listener the listener to add
     */
    void addEventListener(EventListenerDescriptor listener);

    /**
     * Removes an event listener. Used by the framework.
     *
     * @param listener the listener to remove
     */
    void removeEventListener(EventListenerDescriptor listener);

    /**
     * Fires an event given its name and a context.
     *
     * @param name the event name
     * @param context the event context
     */
    void fireEvent(String name, EventContext context);

    /**
     * Fires an event.
     * <p>
     * If a transaction was started, the event is registered if needed to be sent after the transaction commit.
     *
     * @param event the event to fire
     */
    @Override
    void fireEvent(Event event);

    /**
     * Fires all recorded events in a transaction. Used by the framework.
     * <p>
     * The events are fired to {@link PostCommitEventListener} listeners. Events are fired in the form of an event
     * bundle.
     *
     * @param event the event bundle
     */
    @Override
    void fireEventBundle(EventBundle event);

    /**
     * Fires an event bundle in synchronous mode. Used by the framework.
     * <p>
     * This means that asynchronous listeners will be run synchronously.
     */
    void fireEventBundleSync(EventBundle event);

    /**
     * Gets the list of the registered event listeners.
     * <p>
     * Modification on this list will not modify the internal lists in this {@link EventService}.
     *
     * @return the event listeners
     */
    List<EventListener> getEventListeners();

    /**
     * Get the list of the registered post commit event listeners.
     * <p>
     * Modification on this list will not modify the internal lists in this {@link EventService}.
     *
     * @return the post commit event listeners
     */
    List<PostCommitEventListener> getPostCommitEventListeners();

    /**
     * Gets the event listener descriptor corresponding to the give name.
     *
     * @since 5.8
     * @param name the event listener name
     * @return the descriptor, or {@code null} if not found
     */
    EventListenerDescriptor getEventListener(String name);

    /**
     * Waits until all asynchronous tasks are finished.
     */
    void waitForAsyncCompletion();

    /**
     * Waits until all asynchronous tasks are finished, but waits no longer than the given number of milliseconds.
     *
     * @param timeout the maximum time to wait for, in milliseconds
     */
    void waitForAsyncCompletion(long timeout);

    /**
     * Creates the registered domain event producers.
     *
     * @since 11.4
     */
    List<DomainEventProducer> createDomainEventProducers();

}
