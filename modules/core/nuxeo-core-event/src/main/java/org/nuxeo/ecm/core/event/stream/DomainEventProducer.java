/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.lib.stream.computation.Record;

/**
 * Collects Core Events and produces Domain Event Records.
 *
 * @since 11.4
 */
public abstract class DomainEventProducer {
    protected final String name;

    protected final String stream;

    public DomainEventProducer(String name, String stream) {
        this.name = name;
        this.stream = stream;
    }

    /**
     * The name of the domain event.
     */
    public String getName() {
        return name;
    }

    public String getStream() {
        return stream;
    }

    /**
     * Receives Nuxeo Core events from the synchronous listener DomainEventProducerListener.
     */
    public abstract void addEvent(Event event);

    /**
     * Produces Domain Event Records from the accumulated Nuxeo Core events.
     */
    public abstract List<Record> getDomainEvents();

}
