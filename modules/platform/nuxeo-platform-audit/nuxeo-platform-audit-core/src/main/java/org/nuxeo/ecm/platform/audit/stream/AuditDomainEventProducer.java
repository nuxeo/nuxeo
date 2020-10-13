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
package org.nuxeo.ecm.platform.audit.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.stream.DomainEventProducer;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Audit domain event producer.
 *
 * @since 11.4
 */
public class AuditDomainEventProducer extends DomainEventProducer {

    protected static final Logger log = LogManager.getLogger(AuditDomainEventProducer.class);

    protected static final String SOURCE_NAME = "ADEP"; // Audit Domain Event Producer

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String CODEC_NAME = "avroBinary";

    protected final List<Record> records = new ArrayList<>();

    protected final Codec<AuditDomainEvent> codec;

    public AuditDomainEventProducer(String name, String stream) {
        super(name, stream);
        codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, AuditDomainEvent.class);
    }

    @Override
    public void addEvent(Event event) {
        AuditLogger logger = Framework.getService(AuditLogger.class);
        if (logger == null || event == null) {
            return;
        }
        if (logger.getAuditableEventNames().contains(event.getName())) {
            LogEntry entry = logger.buildEntryFromEvent(event);
            if (entry != null) {
                records.add(buildRecordFromEvent(entry));
            }
        }
    }

    protected Record buildRecordFromEvent(LogEntry entry) {
        AuditDomainEvent event = new AuditDomainEvent();
        // entry.getId() is always null at this stage
        // entry.logDate is always null at this stage
        event.source = SOURCE_NAME;
        event.name = entry.getEventId();
        event.category = entry.getCategory();
        event.docId = entry.getDocUUID();
        event.docRepository = entry.getRepositoryId();
        event.principalName = entry.getPrincipalName();
        event.docLifeCycle = entry.getDocLifeCycle();
        event.docType = entry.getDocType();
        event.docPath = entry.getDocPath();
        event.comment = entry.getComment();
        if (entry.getEventDate() != null) {
            event.date = entry.getEventDate().getTime();
        }
        Map<String, ExtendedInfo> extended = entry.getExtendedInfos();
        if (extended != null && !extended.isEmpty()) {
            try {
                event.extendedInfoAsJson = MAPPER.writeValueAsString(extended);
            } catch (JsonProcessingException e) {
                log.error("Skip invalid extended info: {}", event, e);
            }
        }
        log.debug("event {} -> {}", entry, event);
        return Record.of(event.name, codec.encode(event));
    }

    @Override
    public List<Record> getDomainEvents() {
        return records;
    }
}
