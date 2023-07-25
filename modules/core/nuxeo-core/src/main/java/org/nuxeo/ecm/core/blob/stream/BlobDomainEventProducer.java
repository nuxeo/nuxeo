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
 *     Guillaume RENARD
 */
package org.nuxeo.ecm.core.blob.stream;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.BlobEventContext;
import org.nuxeo.ecm.core.event.stream.DomainEventProducer;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * @since 2023
 */
public class BlobDomainEventProducer extends DomainEventProducer {

    protected static final Logger log = LogManager.getLogger(BlobDomainEventProducer.class);

    protected static final String CODEC_NAME = "avro";

    protected static final String SOURCE_NAME = "BLOB"; // Blob Domain Event Producer

    protected final Codec<BlobDomainEvent> codec;

    protected final List<Record> records = new ArrayList<>();

    public BlobDomainEventProducer(String name, String stream) {
        super(name, stream);
        codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, BlobDomainEvent.class);
    }

    @Override
    public void addEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof BlobEventContext)) {
            return;
        }
        records.add(buildRecordFromEvent(event.getName(), (BlobEventContext) ctx));
    }

    protected Record buildRecordFromEvent(String name, BlobEventContext ctx) {
        BlobDomainEvent event = new BlobDomainEvent();
        event.source = SOURCE_NAME;
        event.event = name;
        event.user = ctx.getPrincipal() != null ? ctx.getPrincipal().getName() : null;
        event.repository = ctx.getRepositoryName();
        event.docId = ctx.getDocId();
        event.xpath = ctx.getXpath();
        event.blobKey = ctx.getBlob().getKey();
        event.blobDigest = ctx.getBlob().getDigest();
        event.blobLength = ctx.getBlob().getLength();
        event.mimeType = ctx.getBlob().getMimeType();
        event.fileName = ctx.getBlob().getFilename();
        return Record.of(event.blobKey, codec.encode(event));
    }

    @Override
    public List<Record> getDomainEvents() {
        return records;
    }

}
