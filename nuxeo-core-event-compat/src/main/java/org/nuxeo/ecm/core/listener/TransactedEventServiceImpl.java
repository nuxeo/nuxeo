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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.listener;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventTransactionListener;
import org.nuxeo.runtime.api.Framework;

/**
 * This class handle the difference between operations (used by apogee) and core events.
 * <p>
 * When an event is raised within an operation it will be ignored
 * (since the operation contains all the information about the modifications that were done by the operation).
 * <p>
 * If there is any active operation then events will be stacked until commit is done.
 * <p>
 * When in an operation context the operations will be stacked until commit is done. (events are ignored)
 * Most of the time there will be any one operation stacked (the root one) - this depends on whether child
 * operation notifications are blocked or not. See {@link Operation#BLOCK_CHILD_NOTIFICATIONS}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TransactedEventServiceImpl implements TransactedEventService, EventTransactionListener {

    private static final Log log = LogFactory.getLog(TransactedEventServiceImpl.class);

    protected static final ThreadLocal<EventList> events = new ThreadLocal<EventList>() {
        @Override
        protected EventList initialValue() {
            return new EventList(false);
        }
    };

    protected final ListenerList postListeners = new ListenerList();
    protected final ListenerList preListeners = new ListenerList();
    protected final ListenerList saveListeners = new ListenerList();


    public TransactedEventServiceImpl() {
        EventService es = Framework.getLocalService(EventService.class);
        es.addTransactionListener(this);
    }
    
    
    public void addListener(TransactedListener listener) {
        if (listener instanceof OnSaveListener) {
            saveListeners.add(listener);
        } else if (listener instanceof PostCommitListener) {
            postListeners.add(listener);
        } else if (listener instanceof PreCommitListener) {
            preListeners.add(listener);
        } else {
            throw new UnsupportedOperationException(
                    "TransactionListener of type '"+listener.getClass().getName()+"' is not known");
        }
    }

    public void removeListener(TransactedListener listener) {
        if (listener instanceof OnSaveListener) {
            saveListeners.remove(listener);
        } else if (listener instanceof PostCommitListener) {
            postListeners.remove(listener);
        } else if (listener instanceof PreCommitListener) {
            preListeners.remove(listener);
        } else {
            throw new UnsupportedOperationException(
                    "TransactionListener of type '"+listener.getClass().getName()+"' is not known");
        }
    }

    public void record(Object event) {
        if (event instanceof CoreEvent) {
            recordEvent((CoreEvent)event);
        } else if (event instanceof Operation) {
            recordOperation((Operation<?>)event);
        } else if (event != null) {
            throw new IllegalArgumentException("Unsupported event type: "+event.getClass().getName());
        }
    }

    public void recordEvent(CoreEvent event) {
        EventList list = events.get();
        if (list.operations != null) {
            if (log.isDebugEnabled()) {
                log.debug("" +
                        "Ignoring post commit recording for core event "+event.getEventId()+" since there is an operation context");
            }
            return;
        }
        if (isSaveEvent(event)) {
            list.add(event);
            fireSaveEvents(list);
            if (!list.transactionStarted) {
                commitSave(list);
            }
        } else {
            list.add(event);
        }
    }

    public void recordOperation(Operation<?> event) {
        EventList list = events.get();
        list.addOperation(event);
    }

    //TODO: transaction support is disabled because it is not working as expected in nuxeo
    // since there are tens of tx opened on every user level operation ...
    //TODO XXX fix tx support
    public void transactionStarted() {
        events.set(new EventList(true));
    }

//    public void transactionAboutToCommit() {
//        fireAboutToCommitEvents(events.get());
//    }

    public void transactionCommitted() {
        try {
            fireAboutToCommitEvents(events.get()); // about to commit was merged with commit
            fireCommitEvents(events.get());
        } finally {
            events.remove();
        }
    }

    public void transactionRollbacked() {
        events.remove();
    }

    protected boolean isSaveEvent(Object event) {
        if (event instanceof CoreEvent) {
            return DocumentEventTypes.SESSION_SAVED.equals(((CoreEvent)event).getEventId());
        }
        return false;
    }

    protected void commitSave(EventList list) {
        try {
            fireAboutToCommitEvents(list);
            fireCommitEvents(list);
        } finally {
            events.remove();
        }
    }

    protected void fireSaveEvents(EventList list) {
        if (list.operations == null) {
            if (!list.isEmpty()) {
                CoreEvent[] events = list.toArray(new CoreEvent[list.size()]);
                for (Object listener : saveListeners.getListeners()) {
                    ((OnSaveListener)listener).onSave(events);
                }
            }
        } else if (!list.operations.isEmpty()) {
            Operation<?>[] events = list.operations.toArray(new Operation[list.operations.size()]);
            for (Object listener : saveListeners.getListeners()) {
                ((OnSaveListener)listener).onSave(events);
            }
        }
    }

    protected void fireAboutToCommitEvents(EventList list) {
        if (list.operations == null) {
            if (!list.isEmpty()) {
                CoreEvent[] events = list.toArray(new CoreEvent[list.size()]);
                for (Object listener : preListeners.getListeners()) {
                    ((PreCommitListener) listener).aboutToCommit(events);
                }
            }
        } else if (!list.operations.isEmpty()) {
            Operation<?>[] events = list.operations.toArray(new Operation[list.operations.size()]);
            for (Object listener : preListeners.getListeners()) {
                ((PreCommitListener) listener).aboutToCommit(events);
            }
        }
    }

    protected void fireCommitEvents(EventList list) {
        if (list.operations == null) {
            if (!list.isEmpty()) {
                CoreEvent[] events = list.toArray(new CoreEvent[list.size()]);
                for (Object listener : postListeners.getListeners()) {
                    ((PostCommitListener)listener).onCommit(events);
                }
            }
        } else if (!list.operations.isEmpty()) {
            Operation<?>[] events = list.operations.toArray(new Operation[list.operations.size()]);
            for (Object listener : postListeners.getListeners()) {
                ((PostCommitListener)listener).onCommit(events);
            }
        }
    }

    /**
     * Can hold both operations and CoreEvents
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     *
     */
    static class EventList extends ArrayList<CoreEvent> {
        private static final long serialVersionUID = 1L;
        protected boolean transactionStarted = false;
        protected List<Operation<?>> operations;

        EventList(boolean transactionStarted) {
            this.transactionStarted = transactionStarted;
        }

        void addOperation(Operation<?> operation) {
            if (operations == null) {
                operations = new ArrayList<Operation<?>>();
            }
            operations.add(operation);
        }

        boolean hasOperations() {
            return operations != null;
        }
    }

    
    
}
