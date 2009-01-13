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
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * The event service manage listener registries and 
 * notify listeners about core events.
 * 
 * The service is able to run in a transactioned mode where all events are recorded and 
 * fired after the transaction commits in one step as an event bundle.
 * 
 *  To start a transaction you need to call {@link #startTransaction()} method and 
 *  after the transaction is commited you need to call {@link #commit()} to fire the 
 *  event bundle. If the transaction rollback then you need to call {@link #rollback()} to
 *  cleanup recorded events.
 *  
 *  Events are recorded in a thread variable so they are valid only in the current thread.
 * 
 * Listeners are of 2 types: {@link EventListener} notified as the event is raised and 
 * {@link PostCommitEventListener} notified after the transaction was commited.
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EventService {

    /**
     * Add a new event listener.
     * The event listener is described by a {@link EventListenerDescriptor} that may 
     * specify a priority.
     * Boith types of listeners are registered 
     * @param listener the listener to add
     */
    public void addEventListener(EventListenerDescriptor listener);
    
    /**
     * Remove a listener
     * @param listener the listener to remove
     */
    public void removeEventListener(EventListenerDescriptor listener);
    
    /**
     *  Fire an event given its name and a context 
     * @param name the event name
     * @param context the event context
     * @throws ClientException
     */
    public void fireEvent(String name, EventContext context) throws ClientException ;

    /**
     * Fire an event.
     * If a transaction was started the event is registered if needed to be send after the transaction commit. 
     * 
     * @param event the event to fire
     * @throws ClientException
     */
    public void fireEvent(Event event) throws ClientException;
    
    /**
     * Fire all recorded events in a transaction.
     * The events are fired to {@link PostCommitEventListener} listeners.
     *  Events are fired in the form of an event bundle
     * @param event the event bundle
     * @throws ClientException
     */
    public void fireEventBundle(EventBundle event) throws ClientException;
    
    /**
     * Notify that a transaction was started. Any fired events will be recorded until the transaction is 
     * terminated either by calling {@link #transactionRollbacked()} either {@link #transactionCommited()()} 
     */
    public void transactionStarted();
  
    /**
     * Notify that the transaction was commited. This will fire the events collected during the transaction
     * in the form of a {@link EventBundle}. 
     * After this the recording will stop and recorded events discarded.
     * @throws ClientException
     */
    public void transactionCommited() throws ClientException;
    
    /**
     * Notify that transaction was rollbacked. This will discard any recorded event.
     */
    public void transactionRollbacked();
    
    /**
     * Tests whether or not a transaction was started.
     * @return true if a transaction was started, fasle otherwise
     */
    public boolean isTransactionStarted();
    
}
