/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.EventTransactionListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the event service.
 */
public class EventServiceImpl implements EventService, EventServiceAdmin {

    public static final VMID VMID = new VMID();

    private static final Log log = LogFactory.getLog(EventServiceImpl.class);

    protected static final ThreadLocal<CompositeEventBundle> compositeBundle = new ThreadLocal<CompositeEventBundle>() {
        @Override
        protected CompositeEventBundle initialValue() {
            return new CompositeEventBundle();
        }
    };

    private static class CompositeEventBundle {

        boolean transacted;

        final Map<String, EventBundle> byRepository = new HashMap<String, EventBundle>();

        void push(Event event) {
            String repositoryName = event.getContext().getRepositoryName();
            if (!byRepository.containsKey(repositoryName)) {
                byRepository.put(repositoryName, new EventBundleImpl());
            }
            byRepository.get(repositoryName).push(event);
        }

    }

    protected final ListenerList txListeners;

    protected final EventListenerList listenerDescriptors;

    protected PostCommitEventExecutor postCommitExec;

    protected volatile AsyncEventExecutor asyncExec;

    protected final List<AsyncWaitHook> asyncWaitHooks = new CopyOnWriteArrayList<AsyncWaitHook>();

    protected boolean blockAsyncProcessing = false;

    protected boolean blockSyncPostCommitProcessing = false;

    protected boolean bulkModeEnabled = false;

    public EventServiceImpl() {
        txListeners = new ListenerList();
        listenerDescriptors = new EventListenerList();
        postCommitExec = new PostCommitEventExecutor();
        asyncExec = new AsyncEventExecutor();
        init();
    }

    public void init() {
        asyncExec.init();
    }

    public void shutdown(long timeoutMillis) throws InterruptedException {
        postCommitExec.shutdown(timeoutMillis);
        Set<AsyncWaitHook> notTerminated = new HashSet<AsyncWaitHook>();
        for (AsyncWaitHook hook : asyncWaitHooks) {
            if (hook.shutdown() == false) {
                notTerminated.add(hook);
            }
        }
        if (!notTerminated.isEmpty()) {
            throw new RuntimeException("Asynch services are still running : "
                    + notTerminated);
        }
        if (asyncExec.shutdown(timeoutMillis) == false) {
            throw new RuntimeException(
                    "Async executor is still running, timeout expired");
        }
    }

    public void registerForAsyncWait(AsyncWaitHook callback) {
        asyncWaitHooks.add(callback);
    }

    public void unregisterForAsyncWait(AsyncWaitHook callback) {
        asyncWaitHooks.remove(callback);
    }

    /**
     * @deprecated use {@link #waitForAsyncCompletion()} instead.
     */
    @Deprecated
    public int getActiveAsyncTaskCount() {
        return asyncExec.getUnfinishedCount();
    }

    @Override
    public void waitForAsyncCompletion() {
        waitForAsyncCompletion(Long.MAX_VALUE);
    }

    @Override
    public void waitForAsyncCompletion(long timeout) {
        Set<AsyncWaitHook> notCompleted = new HashSet<AsyncWaitHook>();
        for (AsyncWaitHook hook : asyncWaitHooks) {
            if (!hook.waitForAsyncCompletion()) {
                notCompleted.add(hook);
            }
        }
        if (!notCompleted.isEmpty()) {
            throw new RuntimeException("Async tasks are still running : "
                    + notCompleted);
        }
        try {
            if (!asyncExec.waitForCompletion(timeout)) {
                throw new RuntimeException(
                        "Async event listeners thread pool is not terminated");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // TODO change signature
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addEventListener(EventListenerDescriptor listener) {
        try {
            listenerDescriptors.add(listener);
            log.debug("Registered event listener: " + listener.getName());
        } catch (Exception e) {
            log.error(
                    "Failed to register event listener: " + listener.getName(),
                    e);
        }
    }

    @Override
    public void removeEventListener(EventListenerDescriptor listener) {
        try {
            listenerDescriptors.removeDescriptor(listener);
            log.debug("Unregistered event listener: " + listener.getName());
        } catch (Exception e) {
            log.error(
                    "Failed to unregister event listener: "
                            + listener.getName(), e);
        }
    }

    protected EventStats getEventStats() {
        try {
            return Framework.getService(EventStats.class);
        } catch (Exception e) {
            log.warn("Failed to lookup event stats service", e);
        }
        return null;
    }

    @Override
    public void fireEvent(String name, EventContext context)
            throws ClientException {
        fireEvent(new EventImpl(name, context));
    }

    @Override
    public void fireEvent(Event event) throws ClientException {

        String ename = event.getName();
        EventStats stats = getEventStats();
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledInlineListenersDescriptors()) {
            if (desc.acceptEvent(ename)) {
                Throwable rollbackException = null;
                try {
                    long t0 = System.currentTimeMillis();
                    desc.asEventListener().handleEvent(event);
                    if (stats != null) {
                        stats.logSyncExec(desc, System.currentTimeMillis() - t0);
                    }
                } catch (Throwable t) {
                    String message;
                    if (event.isBubbleException() || event.isMarkedForRollBack()) {
                        message = "Error during "
                                + desc.getName()
                                + " sync listener execution, transaction will be rolled back";
                        rollbackException = t;
                    } else {
                        message = "Error during "
                                + desc.getName()
                                + " sync listener execution, transaction won't be rolled back "
                                + "since event.markRollBack() was not called by the Listener";
                    }
                    if (t instanceof RecoverableClientException) {
                        log.info(message + "\n" + t.getMessage());
                        log.debug(message, t);
                    } else {
                        log.error(message, t);
                    }
                } finally {
                    if (event.isBubbleException()) {
                        throw new RuntimeException(rollbackException);
                    } else if (event.isMarkedForRollBack()) {

                        String message = "Exception during " + desc.getName()
                                + " sync listener execution, rolling back";
                        if (event.getRollbackMessage() != null) {
                            message = message + " ("
                                    + event.getRollbackMessage() + ")";
                        }
                        if (event.getRollbackException() != null) {
                            rollbackException = event.getRollbackException();
                        }

                        if (rollbackException != null) {
                            throw new RuntimeException(message,
                                    rollbackException);
                        } else {
                            throw new RuntimeException(message);
                        }
                    }
                    if (event.isCanceled()) {
                        return;
                    }
                }
            }
        }

        if (!event.isInline()) { // record the event
            // don't record the complete event, only a shallow copy
            ShallowEvent shallowEvent = ShallowEvent.create(event);
            if (event.isImmediate()) {
                EventBundleImpl b = new EventBundleImpl();
                b.push(shallowEvent);
                fireEventBundle(b);
            } else {
                CompositeEventBundle b = compositeBundle.get();
                b.push(shallowEvent);
                // check for commit events to flush the event bundle
                if (!b.transacted && event.isCommitEvent()) {
                    handleTxCommited();
                }
            }
        }
    }

    @Override
    public void fireEventBundle(EventBundle event) throws ClientException {
        boolean comesFromJMS = false;

        if (event instanceof ReconnectedEventBundle) {
            if (((ReconnectedEventBundle) event).comesFromJMS()) {
                comesFromJMS = true;
            }
        }

        List<EventListenerDescriptor> postCommitSync = listenerDescriptors.getEnabledSyncPostCommitListenersDescriptors();
        List<EventListenerDescriptor> postCommitAsync = listenerDescriptors.getEnabledAsyncPostCommitListenersDescriptors();

        if (bulkModeEnabled) {
            // run all listeners synchronously in one transaction
            List<EventListenerDescriptor> listeners = new ArrayList<EventListenerDescriptor>();
            if (!blockSyncPostCommitProcessing) {
                listeners = postCommitSync;
            }
            if (!blockAsyncProcessing) {
                listeners.addAll(postCommitAsync);
            }
            if (!listeners.isEmpty()) {
                postCommitExec.runBulk(listeners, event);
            }
            return;
        }

        // run sync listeners
        if (blockSyncPostCommitProcessing) {
            log.debug("Dropping PostCommit handler execution");
        } else if (comesFromJMS) {
            // when called from JMS we must skip sync listeners
            // - postComit listeners should be on the core
            // - there is no transaction started by JMS listener
            log.debug("Deactivating sync post-commit listener since we are called from JMS");
        } else {
            if (!postCommitSync.isEmpty()) {
                postCommitExec.run(postCommitSync, event);
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
            asyncExec.run(postCommitAsync, event);
        }
    }

    @Override
    public void fireEventBundleSync(EventBundle event) throws ClientException {
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledSyncPostCommitListenersDescriptors()) {
            desc.asPostCommitListener().handleEvent(event);
        }
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledAsyncPostCommitListenersDescriptors()) {
            desc.asPostCommitListener().handleEvent(event);
        }
    }

    @Override
    public List<EventListener> getEventListeners() {
        return listenerDescriptors.getInLineListeners();
    }

    @Override
    public List<PostCommitEventListener> getPostCommitEventListeners() {
        List<PostCommitEventListener> result = new ArrayList<PostCommitEventListener>();

        result.addAll(listenerDescriptors.getSyncPostCommitListeners());
        result.addAll(listenerDescriptors.getAsyncPostCommitListeners());

        return result;
    }

    @Override
    public void transactionStarted() {
        handleTxStarted();
    }

    @Override
    public void transactionCommitted() throws ClientException {
        handleTxCommited();
    }

    @Override
    public void transactionRolledback() {
        handleTxRollbacked();
    }

    @Override
    public boolean isTransactionStarted() {
        return compositeBundle.get().transacted;
    }

    public EventListenerList getEventListenerList() {
        return listenerDescriptors;
    }

    // methods for monitoring

    @Override
    public EventListenerList getListenerList() {
        return listenerDescriptors;
    }

    @Override
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

    @Override
    public int getActiveThreadsCount() {
        return asyncExec.getActiveCount();
    }

    @Override
    public int getEventsInQueueCount() {
        return asyncExec.getUnfinishedCount();
    }

    @Override
    public boolean isBlockAsyncHandlers() {
        return blockAsyncProcessing;
    }

    @Override
    public boolean isBlockSyncPostCommitHandlers() {
        return blockSyncPostCommitProcessing;
    }

    @Override
    public void setBlockAsyncHandlers(boolean blockAsyncHandlers) {
        blockAsyncProcessing = blockAsyncHandlers;
    }

    @Override
    public void setBlockSyncPostCommitHandlers(
            boolean blockSyncPostComitHandlers) {
        blockSyncPostCommitProcessing = blockSyncPostComitHandlers;
    }

    @Override
    public boolean isBulkModeEnabled() {
        return bulkModeEnabled;
    }

    @Override
    public void setBulkModeEnabled(boolean bulkModeEnabled) {
        this.bulkModeEnabled = bulkModeEnabled;
    }

    @Override
    public void addTransactionListener(EventTransactionListener listener) {
        txListeners.add(listener);
    }

    @Override
    public void removeTransactionListener(EventTransactionListener listener) {
        txListeners.remove(listener);
    }

    protected void handleTxStarted() {
        compositeBundle.get().transacted = true;
        for (Object listener : txListeners.getListeners()) {
            ((EventTransactionListener) listener).transactionStarted();
        }
    }

    protected void handleTxRollbacked() {
        compositeBundle.remove();
        for (Object listener : txListeners.getListeners()) {
            ((EventTransactionListener) listener).transactionRollbacked();
        }
    }

    protected void handleTxCommited() {
        CompositeEventBundle b = compositeBundle.get();
        compositeBundle.remove();

        // notify post commit event listeners
        for (EventBundle bundle : b.byRepository.values()) {
            try {
                fireEventBundle(bundle);
            } catch (ClientException e) {
                log.error("Error while processing " + bundle, e);
            }
        }
        // notify post commit tx listeners
        for (Object listener : txListeners.getListeners()) {
            ((EventTransactionListener) listener).transactionCommitted();
        }
    }

}
