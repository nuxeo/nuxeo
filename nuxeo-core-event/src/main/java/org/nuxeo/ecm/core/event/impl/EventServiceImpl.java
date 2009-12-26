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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.EventTransactionListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.core.event.jmx.EventStatsHolder;
import org.nuxeo.ecm.core.event.tx.BulkExecutor;
import org.nuxeo.ecm.core.event.tx.PostCommitSynchronousRunner;

/**
 * @author Bogdan Stefanescu
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class EventServiceImpl implements EventService, EventServiceAdmin{

    public static final VMID VMID = new VMID();

    private static final Log log = LogFactory.getLog(EventServiceImpl.class);

    protected static final ThreadLocal<EventBundleImpl> bundle = new ThreadLocal<EventBundleImpl>() {
        @Override
        protected EventBundleImpl initialValue() {
            return new EventBundleImpl();
        }
    };

    protected ListenerList txListeners;

    protected final EventListenerList listenerDescriptors;

    protected final AsyncEventExecutor asyncExec;

    protected boolean blockAsyncProcessing = false;

    protected boolean blockSyncPostCommitProcessing = false;

    protected boolean bulkModeEnabled = false;

    public EventServiceImpl() {
        txListeners = new ListenerList();
        listenerDescriptors = new EventListenerList();
        asyncExec = AsyncEventExecutor.create();
    }

    /**
     * @deprecated use {@link #waitForAsyncCompletion()} instead.
     */
    @Deprecated
    public int getActiveAsyncTaskCount() {
        return asyncExec.getUnfinishedCount();
    }

    public void waitForAsyncCompletion() {
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

        } while (asyncExec.getUnfinishedCount() > 0);
    }

    public void addEventListener(EventListenerDescriptor listener) {
        try {
            listenerDescriptors.add(listener);

        } catch (Exception e) {
            log.error("Failed to register event listener", e);
        }
    }

    public void removeEventListener(EventListenerDescriptor listener) {
        try {
            listenerDescriptors.removeDescriptor(listener);
        } catch (Exception e) {
            log.error("Failed to register event listener", e);
        }
    }

    public void fireEvent(String name, EventContext context)
            throws ClientException {
        fireEvent(new EventImpl(name, context));
    }

    @SuppressWarnings("unchecked")
    public void fireEvent(Event event) throws ClientException {

        if (!event.isInline()) { // record the event
            // don't record the complete event, only a shallow copy
            ShallowEvent shallowEvent = ShallowEvent.create(event);
            if (event.isImmediate()) {
                EventBundleImpl b = new EventBundleImpl();
                b.push(shallowEvent);
                fireEventBundle(b);
            }
            else {
                EventBundleImpl b = bundle.get();
                b.push(shallowEvent);
                // check for commit events to flush the event bundle
                if (!b.isTransacted() && event.isCommitEvent()) {
                    transactionCommitted();
                }
            }
        }
        String ename = event.getName();
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledInlineListenersDescriptors()) {
            if (desc.acceptEvent(ename)) {
                try {
                    long t0 = System.currentTimeMillis();
                    desc.asEventListener().handleEvent(event);
                    EventStatsHolder.logSyncExec(desc, System.currentTimeMillis()-t0);
                } catch (Throwable t) {
                    log.error("Error during sync listener execution", t);
                } finally {
                    if (event.isMarkedForRollBack()) {
                        throw new RuntimeException(
                                "Exception during sync listener execution, rollingback");
                    }
                    if (event.isCanceled()) {
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void fireEventBundle(EventBundle event) throws ClientException {
        boolean comesFromJMS = false;

        if (event instanceof ReconnectedEventBundle) {
            if (((ReconnectedEventBundle) event).comesFromJMS()) {
                comesFromJMS = true;
            }
        }

        if (bulkModeEnabled) {
            // run all listeners synchronously in one transaction
            List<EventListenerDescriptor> listeners = new ArrayList<EventListenerDescriptor>();
            if (!blockSyncPostCommitProcessing) {
                listeners = listenerDescriptors.getEnabledSyncPostCommitListenersDescriptors();
            }
            if (!blockAsyncProcessing) {
                listeners.addAll(listenerDescriptors.getEnabledAsyncPostCommitListenersDescriptors());
            }
            if (!listeners.isEmpty()) {
                BulkExecutor bulkExecutor = new BulkExecutor(listeners, event);
                bulkExecutor.run();
            }
            return;
        }

        // run sync listeners
        if (blockSyncPostCommitProcessing) {
            log.debug("Dropping PostCommit handler execution");
        }
        else if (comesFromJMS) {
            // when called from JMS we must skip sync listeners
            // - postComit listeners should be on the core
            // - there is no transaction started by JMS listener
            log.debug("Deactivating sync post-commit listener since we are called from JMS");
        } else {
            List<EventListenerDescriptor> syncPCDescs = listenerDescriptors.getEnabledSyncPostCommitListenersDescriptors();
            if (syncPCDescs!=null && !syncPCDescs.isEmpty()) {
                PostCommitSynchronousRunner syncRunner = new PostCommitSynchronousRunner(
                        syncPCDescs, event);
                syncRunner.run();
            }
        }

        if (blockAsyncProcessing) {
            log.debug("Dopping bundle");
            return;
        }

        // fire async listeners
        if (AsyncProcessorConfig.forceJMSUsage() && !comesFromJMS) {
            log.debug("Skipping async exec, this will be triggered via JMS");
        } else {
            asyncExec.run(listenerDescriptors.getEnabledAsyncPostCommitListenersDescriptors(), event);
        }
    }

    @SuppressWarnings("unchecked")
    public void fireEventBundleSync(EventBundle event) throws ClientException {
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledSyncPostCommitListenersDescriptors()) {
            desc.asPostCommitListener().handleEvent(event);
        }
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledAsyncPostCommitListenersDescriptors()) {
            desc.asPostCommitListener().handleEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    public List<EventListener> getEventListeners() {
         return listenerDescriptors.getInLineListeners();
    }

    @SuppressWarnings("unchecked")
    public List<PostCommitEventListener> getPostCommitEventListeners() {
        List<PostCommitEventListener> result = new ArrayList<PostCommitEventListener>();

        result.addAll(listenerDescriptors.getSyncPostCommitListeners());
        result.addAll(listenerDescriptors.getAsyncPostCommitListeners());

        return result;
    }

    public void transactionStarted() {
        bundle.get().setTransacted(true);
        fireTxStarted();
    }

    public void transactionCommitted() throws ClientException {
        EventBundleImpl b = bundle.get();
        bundle.remove();
        if (b != null && !b.isEmpty()) {
            fireEventBundle(b);
        }
        fireTxCommited();
    }

    public void transactionRolledback() {
        bundle.remove();
        fireTxRollbacked();
    }

    public boolean isTransactionStarted() {
        return bundle.get().isTransacted();
    }

    public EventListenerList getEventListenerList() {
        return listenerDescriptors;
    }

    // methods for monitoring

    public EventListenerList getListenerList() {
        return listenerDescriptors;
    }

    public void setListenerEnabledFlag(String listenerName, boolean enabled) {
        if (!listenerDescriptors.getListenerNames().contains(listenerName)) {
            return;
        }

        for (EventListenerDescriptor desc : listenerDescriptors.getAsyncPostCommitListenersDescriptors()) {
            if (desc.getName().equals(listenerName)) {
                desc.setEnabled(enabled);
                synchronized (this) {
                    listenerDescriptors.recomputeEnabledListeners();
                }
                return;
            }
        }

        for (EventListenerDescriptor desc : listenerDescriptors.getSyncPostCommitListenersDescriptors()) {
            if (desc.getName().equals(listenerName)) {
                desc.setEnabled(enabled);
                synchronized (this) {
                    listenerDescriptors.recomputeEnabledListeners();
                }
                return;
            }
        }

        for (EventListenerDescriptor desc : listenerDescriptors.getInlineListenersDescriptors()) {
            if (desc.getName().equals(listenerName)) {
                desc.setEnabled(enabled);
                synchronized (this) {
                    listenerDescriptors.recomputeEnabledListeners();
                }
                return;
            }
        }
    }

    public int getActiveThreadsCount() {
        return asyncExec.getActiveCount();
    }

    public int getEventsInQueueCount() {
        return asyncExec.getUnfinishedCount();
    }

    public boolean isBlockAsyncHandlers() {
        return blockAsyncProcessing;
    }

    public boolean isBlockSyncPostCommitHandlers() {
        return blockSyncPostCommitProcessing;
    }

    public void setBlockAsyncHandlers(boolean blockAsyncHandlers) {
        blockAsyncProcessing = blockAsyncHandlers;
    }

    public void setBlockSyncPostCommitHandlers(boolean blockSyncPostComitHandlers) {
        blockSyncPostCommitProcessing = blockSyncPostComitHandlers;
    }

    public boolean isBulkModeEnabled() {
        return bulkModeEnabled;
    }

    public void setBulkModeEnabled(boolean bulkModeEnabled) {
        this.bulkModeEnabled = bulkModeEnabled;
    }

    public void addTransactionListener(EventTransactionListener listener) {
        txListeners.add(listener);
    }

    public void removeTransactionListener(EventTransactionListener listener) {
        txListeners.remove(listener);
    }

    protected void fireTxStarted() {
        for (Object listener : txListeners.getListeners()) {
            ((EventTransactionListener)listener).transactionStarted();
        }
    }

    protected void fireTxRollbacked() {
        for (Object listener : txListeners.getListeners()) {
            ((EventTransactionListener)listener).transactionRollbacked();
        }
    }

    protected void fireTxCommited() {
        for (Object listener : txListeners.getListeners()) {
            ((EventTransactionListener)listener).transactionCommitted();
        }
    }

}
