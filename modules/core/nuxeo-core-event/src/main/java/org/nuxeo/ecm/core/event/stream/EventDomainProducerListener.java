/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.event.stream;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 11.1
 */
public class EventDomainProducerListener implements EventListener, Synchronization {
    private static final Logger log = LogManager.getLogger(EventDomainProducerListener.class);

    protected static final ThreadLocal<Boolean> isEnlisted = ThreadLocal.withInitial(() -> Boolean.FALSE);

    protected static final ThreadLocal<List<EventDomainProducer>> producers = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void handleEvent(Event event) {
        if (!isEnlisted.get()) {
            isEnlisted.set(registerSynchronization(this));
            log.debug("Enlisted to transaction");
            initEventDomainProducers();
        }

        producers.get().forEach(producer -> producer.addEvent(event));

        if (!isEnlisted.get()) {
            // there is no transaction so don't wait for a commit
            afterCompletion(Status.STATUS_COMMITTED);
        }
    }

    protected void initEventDomainProducers() {
        producers.set(Framework.getService(EventService.class).createEventProducers());
    }

    protected void cleanEventDomainProducers() {
        producers.remove();
    }

    @Override
    public void beforeCompletion() {
        log.debug("beforeCompletion");
    }

    @Override
    public void afterCompletion(int status) {
        try {
            log.debug("afterCompletion " + status);
            produceDomainEvents();
        } finally {
            isEnlisted.set(false);
            cleanEventDomainProducers();
        }
    }

    protected void produceDomainEvents() {
        StreamService streamService = Framework.getService(StreamService.class);
        for (EventDomainProducer producer : producers.get()) {
            List<Record> records = producer.getDomainEvents();
            if (records.isEmpty()) {
                continue;
            }
            log.debug("Writing domain events");
            LogAppender<Record> appender = streamService.getLogManager().getAppender(producer.getStream());
            records.forEach(record -> appender.append(record.getKey(), record));
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
