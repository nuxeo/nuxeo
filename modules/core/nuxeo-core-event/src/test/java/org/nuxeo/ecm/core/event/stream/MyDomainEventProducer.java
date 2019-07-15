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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.lib.stream.computation.Record;

/**
 * @since 11.4
 */
public class MyDomainEventProducer extends DomainEventProducer {
    private static final Logger log = LogManager.getLogger(MyDomainEventProducer.class);

    List<Record> records = new ArrayList<>();

    public MyDomainEventProducer(String name, String stream) {
        super(name, stream);
        log.info("Create {} on {}", name, stream);
    }

    @Override
    public void addEvent(Event event) {
        log.info("Receive {}", event.getName());
        records.add(Record.of(event.getName(), event.toString().getBytes(UTF_8)));
    }

    @Override
    public List<Record> getDomainEvents() {
        log.info("Returns {} records", records.size());
        return records;
    }
}
