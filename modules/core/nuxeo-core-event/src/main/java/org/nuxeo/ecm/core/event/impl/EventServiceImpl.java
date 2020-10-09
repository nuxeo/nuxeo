/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 *     Andrei Nechaev
 */
package org.nuxeo.ecm.core.event.impl;

import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.EventServiceComponent;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.pipe.EventPipeDescriptor;
import org.nuxeo.ecm.core.event.pipe.EventPipeRegistry;
import org.nuxeo.ecm.core.event.pipe.dispatch.EventBundleDispatcher;
import org.nuxeo.ecm.core.event.pipe.dispatch.EventDispatcherDescriptor;
import org.nuxeo.ecm.core.event.pipe.dispatch.EventDispatcherRegistry;
import org.nuxeo.ecm.core.event.stream.EventDomainProducer;
import org.nuxeo.ecm.core.event.stream.EventDomainProducerDescriptor;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.model.DescriptorRegistry;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;

/**
 * Implementation of the event service.
 */
public class EventServiceImpl implements EventService, EventServiceAdmin, Synchronization {

    public static final VMID VMID = new VMID();

    private static final Log log = LogFactory.getLog(EventServiceImpl.class);

    protected static final ThreadLocal<CompositeEventBundle> threadBundles = new ThreadLocal<CompositeEventBundle>() {
        @Override
        protected CompositeEventBundle initialValue() {
            return new CompositeEventBundle();
        }
    };

    private static class CompositeEventBundle {

        boolean registeredSynchronization;

        final Map<String, EventBundle> byRepository = new HashMap<>();

        void push(Event event) {
            String repositoryName = event.getContext().getRepositoryName();
            if (!byRepository.containsKey(repositoryName)) {
                byRepository.put(repositoryName, new EventBundleImpl());
            }
            byRepository.get(repositoryName).push(event);
        }

    }

    protected final EventListenerList listenerDescriptors;

    protected PostCommitEventExecutor postCommitExec;

    protected volatile AsyncEventExecutor asyncExec;

    protected final List<AsyncWaitHook> asyncWaitHooks = new CopyOnWriteArrayList<>();

    protected boolean blockAsyncProcessing = false;

    protected boolean blockSyncPostCommitProcessing = false;

    protected boolean bulkModeEnabled = false;

    protected EventPipeRegistry registeredPipes = new EventPipeRegistry();

    protected EventDispatcherRegistry dispatchers = new EventDispatcherRegistry();

    protected EventBundleDispatcher pipeDispatcher;

    // @since 11.1
    protected DescriptorRegistry eventDomainProducers = new DescriptorRegistry();

    // @since 11.1
    protected static final String REGISTRY_TARGET_NAME = "EventService";

    public EventServiceImpl() {
        listenerDescriptors = new EventListenerList();
        postCommitExec = new PostCommitEventExecutor();
        asyncExec = new AsyncEventExecutor();
    }

    public void init() {
        asyncExec.init();

        EventDispatcherDescriptor dispatcherDescriptor = dispatchers.getDispatcherDescriptor();
        if (dispatcherDescriptor != null) {
            List<EventPipeDescriptor> pipes = registeredPipes.getPipes();
            if (!pipes.isEmpty()) {
                pipeDispatcher = dispatcherDescriptor.getInstance();
                pipeDispatcher.init(pipes, dispatcherDescriptor.getParameters());
            }
        }
        initEventDomainStreams();
    }

    public EventBundleDispatcher getEventBundleDispatcher() {
        return pipeDispatcher;
    }

    public void addEventDomainProducer(EventDomainProducerDescriptor descriptor) {
        if (descriptor.isEnabled()) {
            eventDomainProducers.register(REGISTRY_TARGET_NAME, EventServiceComponent.EVENT_DOMAIN_PRODUCER_XP,
                    descriptor);
            log.error("Registered event domain producer: " + descriptor.getName());
        } else {
            eventDomainProducers.unregister(REGISTRY_TARGET_NAME, EventServiceComponent.EVENT_DOMAIN_PRODUCER_XP,
                    descriptor);
            log.debug("Unregistered event domain producer (disabled): " + descriptor.getName());
        }
    }

    public void removeEventDomainProducer(EventDomainProducerDescriptor descriptor) {
        eventDomainProducers.unregister(REGISTRY_TARGET_NAME, EventServiceComponent.EVENT_DOMAIN_PRODUCER_XP,
                descriptor);
        log.debug("Unregistered event domain producer: " + descriptor.getName());
    }

    public void shutdown(long timeoutMillis) throws InterruptedException {
        postCommitExec.shutdown(timeoutMillis);
        Set<AsyncWaitHook> notTerminated = asyncWaitHooks.stream().filter(hook -> !hook.shutdown()).collect(
                Collectors.toSet());
        if (!notTerminated.isEmpty()) {
            throw new RuntimeException("Asynch services are still running : " + notTerminated);
        }

        if (!asyncExec.shutdown(timeoutMillis)) {
            throw new RuntimeException("Async executor is still running, timeout expired");
        }
        if (pipeDispatcher != null) {
            pipeDispatcher.shutdown();
        }
    }

    public void registerForAsyncWait(AsyncWaitHook callback) {
        asyncWaitHooks.add(callback);
    }

    public void unregisterForAsyncWait(AsyncWaitHook callback) {
        asyncWaitHooks.remove(callback);
    }

    @Override
    public void waitForAsyncCompletion() {
        waitForAsyncCompletion(Long.MAX_VALUE);
    }

    @Override
    public void waitForAsyncCompletion(long timeout) {
        Set<AsyncWaitHook> notCompleted = asyncWaitHooks.stream()
                                                        .filter(hook -> !hook.waitForAsyncCompletion())
                                                        .collect(Collectors.toSet());
        if (!notCompleted.isEmpty()) {
            throw new RuntimeException("Async tasks are still running : " + notCompleted);
        }
        try {
            if (!asyncExec.waitForCompletion(timeout)) {
                throw new RuntimeException("Async event listeners thread pool is not terminated");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // TODO change signature
            throw new RuntimeException(e);
        }
        if (pipeDispatcher != null) {
            try {
                pipeDispatcher.waitForCompletion(timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void addEventListener(EventListenerDescriptor listener) {
        listenerDescriptors.add(listener);
        log.debug("Registered event listener: " + listener.getName());
    }

    public void addEventPipe(EventPipeDescriptor pipeDescriptor) {
        registeredPipes.addContribution(pipeDescriptor);
        log.debug("Registered event pipe: " + pipeDescriptor.getName());
    }

    public void addEventDispatcher(EventDispatcherDescriptor dispatcherDescriptor) {
        dispatchers.addContrib(dispatcherDescriptor);
        log.debug("Registered event dispatcher: " + dispatcherDescriptor.getName());
    }

    @Override
    public void removeEventListener(EventListenerDescriptor listener) {
        listenerDescriptors.removeDescriptor(listener);
        log.debug("Unregistered event listener: " + listener.getName());
    }

    public void removeEventPipe(EventPipeDescriptor pipeDescriptor) {
        registeredPipes.removeContribution(pipeDescriptor);
        log.debug("Unregistered event pipe: " + pipeDescriptor.getName());
    }

    public void removeEventDispatcher(EventDispatcherDescriptor dispatcherDescriptor) {
        dispatchers.removeContrib(dispatcherDescriptor);
        log.debug("Unregistered event dispatcher: " + dispatcherDescriptor.getName());
    }

    @Override
    public void fireEvent(String name, EventContext context) {
        fireEvent(new EventImpl(name, context));
    }

    @Override
    public void fireEvent(Event event) {

        String ename = event.getName();
        EventStats stats = Framework.getService(EventStats.class);
        Tracer tracer = Tracing.getTracer();
        for (EventListenerDescriptor desc : listenerDescriptors.getEnabledInlineListenersDescriptors()) {
            if (!desc.acceptEvent(ename)) {
                continue;
            }
            try {
                long t0 = System.currentTimeMillis();
                desc.asEventListener().handleEvent(event);
                long elapsed = System.currentTimeMillis() - t0;
                traceAddAnnotation(event, tracer, elapsed, desc.getName());
                if (stats != null) {
                    stats.logSyncExec(desc, elapsed);
                }
                if (event.isCanceled()) {
                    // break loop
                    return;
                }
            } catch (ConcurrentUpdateException e) {
                // never swallow ConcurrentUpdateException
                throw e;
            } catch (RuntimeException e) {
                // get message
                String message = "Exception during " + desc.getName() + " sync listener execution, ";
                if (event.isBubbleException()) {
                    message += "other listeners will be ignored";
                } else if (event.isMarkedForRollBack()) {
                    message += "transaction will be rolled back";
                    if (event.getRollbackMessage() != null) {
                        message += " (" + event.getRollbackMessage() + ")";
                    }
                } else {
                    message += "continuing to run other listeners";
                }
                // log
                tracer.getCurrentSpan().addAnnotation("EventService#fireEvent " + event.getName() + ": " + message);
                if (e instanceof RecoverableClientException) {
                    log.info(message + "\n" + e.getMessage());
                    log.debug(message, e);
                } else {
                    log.error(message, e);
                }
                // rethrow or swallow
                if (TransactionHelper.isTransactionMarkedRollback()) {
                    throw e;
                } else if (event.isBubbleException()) {
                    throw e;
                } else if (event.isMarkedForRollBack()) {
                    Exception ee;
                    if (event.getRollbackException() != null) {
                        ee = event.getRollbackException();
                    } else {
                        ee = e;
                    }
                    // when marked for rollback, throw a generic
                    // RuntimeException to make sure nobody catches it
                    throw new RuntimeException(message, ee);
                } else {
                    // swallow exception
                }
            }
        }

        if (!event.isInline()) { // record the event
            // don't record the complete event, only a shallow copy
            ShallowEvent shallowEvent = ShallowEvent.create(event);
            if (event.isImmediate()) {
                EventBundleImpl b = new EventBundleImpl();
                b.push(shallowEvent);
                tracer.getCurrentSpan().addAnnotation("EventService#fireEvent firing immediate: " + event.getName());
                fireEventBundle(b);
            } else {
                recordEvent(shallowEvent);
            }
        }
    }

    protected void traceAddAnnotation(Event event, Tracer tracer, long elapsed, String listener) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("event", AttributeValue.stringAttributeValue(event.getName()));
        attributes.put("listener", AttributeValue.stringAttributeValue(listener));
        attributes.put("duration_ms", AttributeValue.longAttributeValue(elapsed));
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            DocumentEventContext docContext = (DocumentEventContext) eventContext;
            if (docContext.getSourceDocument() != null) {
                Path docPath = docContext.getSourceDocument().getPath();
                if (docPath != null) {
                    attributes.put("doc", AttributeValue.stringAttributeValue(docPath.toString()));
                }
                String id = docContext.getSourceDocument().getId();
                if (id != null) {
                    attributes.put("doc_id", AttributeValue.stringAttributeValue(id));
                }
            }
        }
        tracer.getCurrentSpan().addAnnotation("EventService#fireEvent Event fired", attributes);
    }

    @Override
    public void fireEventBundle(EventBundle event) {
        Span span = Tracing.getTracer().getCurrentSpan();
        span.addAnnotation("EventService#fireEventBundle");
        try {
            List<EventListenerDescriptor> postCommitSync = listenerDescriptors.getEnabledSyncPostCommitListenersDescriptors();
            List<EventListenerDescriptor> postCommitAsync = listenerDescriptors.getEnabledAsyncPostCommitListenersDescriptors();

            if (bulkModeEnabled) {
                // run all listeners synchronously in one transaction
                List<EventListenerDescriptor> listeners = new ArrayList<>();
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
            if (pipeDispatcher == null) {
                asyncExec.run(postCommitAsync, event);
            } else {
                // rather than sending to the WorkManager: send to the Pipe
                pipeDispatcher.sendEventBundle(event);
            }
        } finally {
            span.addAnnotation("EventService#fireEventBundle.done");
        }
    }

    @Override
    public void fireEventBundleSync(EventBundle event) {
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
        List<PostCommitEventListener> result = new ArrayList<>();

        result.addAll(listenerDescriptors.getSyncPostCommitListeners());
        result.addAll(listenerDescriptors.getAsyncPostCommitListeners());

        return result;
    }

    public EventListenerList getEventListenerList() {
        return listenerDescriptors;
    }

    @Override
    public EventListenerDescriptor getEventListener(String name) {
        return listenerDescriptors.getDescriptor(name);
    }

    // methods for monitoring

    @Override
    public EventListenerList getListenerList() {
        return listenerDescriptors;
    }

    @Override
    public void setListenerEnabledFlag(String listenerName, boolean enabled) {
        if (!listenerDescriptors.hasListener(listenerName)) {
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
    public void setBlockSyncPostCommitHandlers(boolean blockSyncPostComitHandlers) {
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

    protected void recordEvent(Event event) {
        CompositeEventBundle b = threadBundles.get();
        b.push(event);
        if (TransactionHelper.isTransactionActive()) {
            if (!b.registeredSynchronization) {
                // register as synchronization
                try {
                    TransactionHelper.lookupTransactionManager().getTransaction().registerSynchronization(this);
                } catch (NamingException | SystemException | RollbackException e) {
                    throw new RuntimeException("Cannot register Synchronization", e);
                }
                b.registeredSynchronization = true;
            }
        } else if (event.isCommitEvent()) {
            handleTxCommited();
        }
    }

    @Override
    public void beforeCompletion() {
        Span span = Tracing.getTracer().getCurrentSpan();
        span.addAnnotation("EventService#beforeCompletion");
    }

    @Override
    public void afterCompletion(int status) {
        Span span = Tracing.getTracer().getCurrentSpan();
        if (status == Status.STATUS_COMMITTED) {
            span.addAnnotation("EventService#afterCompletion committed");
            handleTxCommited();
        } else if (status == Status.STATUS_ROLLEDBACK) {
            span.addAnnotation("EventService#afterCompletion ROLLBACK");
            handleTxRollbacked();
        } else {
            log.error("Unexpected afterCompletion status: " + status);
        }
        span.addAnnotation("EventService#afterCompletion.done");
    }

    protected void handleTxRollbacked() {
        threadBundles.remove();
    }

    protected void handleTxCommited() {
        CompositeEventBundle b = threadBundles.get();
        threadBundles.remove();

        // notify post commit event listeners
        for (EventBundle bundle : b.byRepository.values()) {
            try {
                fireEventBundle(bundle);
            } catch (NuxeoException e) {
                log.error("Error while processing " + bundle, e);
            }
        }
    }

    @Override
    public List<EventDomainProducer> createEventProducers() {
        // TODO: optmize this by keeping an immutable list
        List<EventDomainProducerDescriptor> descriptors = eventDomainProducers.getDescriptors(REGISTRY_TARGET_NAME,
                EventServiceComponent.EVENT_DOMAIN_PRODUCER_XP);
        List<EventDomainProducer> ret = new ArrayList<>(descriptors.size());
        descriptors.forEach(descriptor -> {
            EventDomainProducer producer = descriptor.newInstance();
            ret.add(producer);
        });
        return ret;
    }

    protected void initEventDomainStreams() {
        List<EventDomainProducerDescriptor> descriptors = eventDomainProducers.getDescriptors(REGISTRY_TARGET_NAME,
                EventServiceComponent.EVENT_DOMAIN_PRODUCER_XP);
        Settings settings = new Settings(1, 1);
        List<String> streams = new ArrayList<>();
        CodecService codecService = Framework.getService(CodecService.class);
        descriptors.forEach(descriptor -> {
            String streamName = descriptor.getStream().name;
            streams.add(streamName);
            settings.setPartitions(streamName, descriptor.getStream().partitions);
            String codec = descriptor.getStream().codec;
            if (codec != null) {
                settings.setCodec(streamName, codecService.getCodec(codec, Record.class));
            }
            descriptor.getStream().filters.forEach(filter -> settings.addFilter(streamName, filter.getFilter()));
        });
        StreamService streamService = Framework.getService(StreamService.class);
        // streamService.getStreamManager().register(streams, settings);
    }

}
