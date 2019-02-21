/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */
package org.nuxeo.ecm.core.event.async;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Asynchronous events listener that pushed all the contributed events into a Log to feed the Notification Topology.
 *
 * @since XXX
 */
public class EventsStreamListener implements EventListener, Synchronization {
    public static final String EVENT_STREAM = "EVENT"; // TODO

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EventsStreamListener.class);

    protected static final String DELIMITER = ":";

    protected static final ThreadLocal<Boolean> isEnlisted = ThreadLocal.withInitial(() -> Boolean.FALSE);

    protected static final ThreadLocal<Map<String, EventRecord>> entries = ThreadLocal.withInitial(HashMap::new);

    /**
     * Extract the event and push it into the Log configured.
     *
     * @param event The raised event.
     */
    @Override
    public void handleEvent(Event event) {
        if (!isEnlisted.get()) {
            isEnlisted.set(registerSynchronization(this));
            entries.get().clear();
            log.debug("EventsStreamListener collecting entries for the tx");
        }

        NuxeoPrincipal principal = event.getContext().getPrincipal();
        if (principal == null) {
            return;
        }

        String user = principal.getActingUser();
        if (StringUtils.isBlank(user) || SYSTEM_USERNAME.equals(user)) {
            return;
        }

        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withEventName(event.getName())
                                                            .withTime(event.getTime())
                                                            .withUsername(user);
        if (isDocumentEventContext(event.getContext())) {
            DocumentEventContext ctx = (DocumentEventContext) event.getContext();
            builder.withDocument(ctx.getSourceDocument());
        }

        // Loop on the EventTransformer to add context information to the EventRecord
        Framework.getService(EventService.class)
                 .getEventTransformers()
                 .stream()
                 .filter(t -> t.accept(event))
                 .forEach(t -> builder.withContext(t.buildEventRecordContext(event)));

        EventRecord eventRecord = builder.build();
        String key = generateKey(event);
        // Deduplicate events in the same thread.
        entries.get().putIfAbsent(key, eventRecord);

        if (!isEnlisted.get()) {
            // there is no transaction so don't wait for a commit
            afterCompletion(Status.STATUS_COMMITTED);
        }
    }

    protected LogAppender<Record> getAppender() {
        // Create a record in the stream in input of the notification processor
        LogManager logManager = Framework.getService(StreamService.class).getLogManager("event");
        return logManager.getAppender(EVENT_STREAM,
                Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Record.class));
    }

    protected void appendEntries() {
        if (entries.get().isEmpty()) {
            return;
        }

        LogAppender<Record> appender = getAppender();
        Codec<EventRecord> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, EventRecord.class);

        Map<String, EventRecord> filteredRecords = entries.get();
        filteredRecords.forEach((key, event) -> {
            // Append the record to the log
            byte[] encodedEvent = codec.encode(event);
            appender.append(key, Record.of(key, encodedEvent));
        });
    }

    // TODO probably not needed in the generic case:
    // - we may have different nodes with different configuration
    // - we may have external async listeners
    // protected Map<String, EventRecord> filterEntries(Map<String, EventRecord> entries) {
    // Loop on all the EventFilter to check if the event must be kept or not
    // return entries.entrySet().stream().filter(x -> {
    // for (EventFilter filter : Framework.getService(NotificationService.class).getEventFilters()) {
    // if (!filter.acceptEvent(entries, x.getValue())) {
    // return false;
    // }
    // }
    // return true;
    // }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    // }

    /**
     * Generates the key for the record that will be appended to the input stream of the topology.
     * 
     * @return A String representing the key.
     */
    protected String generateKey(Event event) {
        if (isDocumentEventContext(event.getContext())) {
            DocumentEventContext ctx = (DocumentEventContext) event.getContext();
            return String.join(DELIMITER, event.getName(), ctx.getSourceDocument().getId());
        } else {
            NuxeoPrincipal principal = event.getContext().getPrincipal();
            return String.join(DELIMITER, event.getName(), principal.getTenantId(), principal.getName());
        }
    }

    protected boolean isDocumentEventContext(EventContext ctx) {
        return (ctx instanceof DocumentEventContext) && ((DocumentEventContext) ctx).getSourceDocument() != null;
    }

    @Override
    public void beforeCompletion() {
        log.debug("EventsStreamListener going to write {} entries.", entries.get().size());
    }

    @Override
    public void afterCompletion(int status) {
        try {
            if (entries.get().isEmpty()
                    || (Status.STATUS_MARKED_ROLLBACK == status || Status.STATUS_ROLLEDBACK == status)) {
                // This means that in case of rollback there is no event logged
                return;
            }
            appendEntries();
            log.debug("EventsStreamListener writes {} entries.", entries.get().size());
        } finally {
            isEnlisted.set(false);
            entries.get().clear();
        }
    }

    protected boolean registerSynchronization(Synchronization sync) {
        try {
            TransactionManager tm = TransactionHelper.lookupTransactionManager();
            if (tm != null) {
                if (tm.getTransaction() != null) {
                    tm.getTransaction().registerSynchronization(sync);
                    return true;
                }
                return false;
            } else {
                log.error("Unable to register synchronization : no TransactionManager");
                return false;
            }
        } catch (NamingException | IllegalStateException | SystemException | RollbackException e) {
            log.error("Unable to register synchronization", e);
            return false;
        }
    }
}
