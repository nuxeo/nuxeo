/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.model.stream;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentDomainEventContext;
import org.nuxeo.ecm.core.event.stream.DomainEventProducer;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * Propagate internal document events to a document DomainEvent stream.
 *
 * @since 2021.44
 */
public class DocumentDomainEventProducer extends DomainEventProducer {

    protected static final Logger log = LogManager.getLogger(DocumentDomainEventProducer.class);

    protected static final String CODEC_NAME = "avro";

    protected static final String SOURCE_NAME = "DOC";

    protected final Codec<DocumentDomainEvent> codec;

    protected final List<Record> records = new ArrayList<>();

    public DocumentDomainEventProducer(String name, String stream) {
        super(name, stream);
        codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, DocumentDomainEvent.class);
    }

    @Override
    public void addEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentDomainEventContext)) {
            return;
        }
        DocumentDomainEventContext context = (DocumentDomainEventContext) ctx;
        records.add(buildRecordFromEvent(event.getName(), context));
    }

    public void addEvent(DocumentDomainEvent event) {
        records.add(Record.of(event.id, codec.encode(event)));
    }

    protected Record buildRecordFromEvent(String eventName, DocumentDomainEventContext ctx) {
        DocumentDomainEvent event = new DocumentDomainEvent();
        event.source = SOURCE_NAME;
        event.event = eventName;
        event.user = ctx.getPrincipal() != null ? ctx.getPrincipal().getName() : null;
        event.repository = ctx.getRepositoryName();
        Document doc = ctx.getDoc();
        event.id = doc.getUUID();
        event.name = doc.getName();
        event.type = doc.getType().getName();
        event.isVersion = doc.isVersion();
        event.isProxy = doc.isProxy();
        if (event.isVersion || event.isProxy) {
            event.seriesId = doc.getVersionSeriesId();
        }
        return Record.of(event.id, codec.encode(event));
    }

    @Override
    public List<Record> getDomainEvents() {
        return records;
    }

}
