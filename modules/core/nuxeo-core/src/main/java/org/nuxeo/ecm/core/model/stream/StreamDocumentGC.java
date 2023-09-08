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

import static org.nuxeo.ecm.core.model.stream.DocumentDomainEventProducer.CODEC_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * A Processor that clean repository on document deletion.
 *
 * @since 2021.44
 */
public class StreamDocumentGC implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(StreamDocumentGC.class);

    public static final String COMPUTATION_NAME = "document/gc";

    public static final String STREAM_NAME = "source/document";

    public static final String ENABLED_PROPERTY_NAME = "nuxeo.bulk.action.documentGC.enabled";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new DocumentGCComputation(COMPUTATION_NAME),
                               Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public static class DocumentGCComputation extends AbstractComputation {

        protected final Codec<DocumentDomainEvent> codec;

        protected boolean disabled;

        public DocumentGCComputation(String name) {
            super(name, 1, 0);
            codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, DocumentDomainEvent.class);
            disabled = Framework.isBooleanPropertyFalse(StreamDocumentGC.ENABLED_PROPERTY_NAME);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            if (!disabled) {
                DocumentDomainEvent dde = codec.decode(record.getData());
                log.trace("Processing document domain event: {} for repository: {}, docId: {}", dde.event, dde.repository, dde.id);
                if (DocumentEventTypes.INTERNAL_DOCUMENT_DELETED.equals(dde.event) && !dde.isVersion) {
                    documentGC(dde);
                }
            }
            context.askForCheckpoint();
        }

        protected void documentGC(DocumentDomainEvent dde) {
            TransactionHelper.runInTransaction(() -> {
                try (NuxeoLoginContext ignored = Framework.loginSystem()) {
                    CoreSession session = CoreInstance.getCoreSession(dde.repository);
                    String docId = dde.id;
                    if (dde.isProxy && dde.seriesId != null) {
                        // when removing a proxy, its live doc pointed by seriesId may have orphan versions
                        docId = dde.seriesId;
                    }
                    log.debug("Try to clean orphan versions for: {}, isProxy: {}", dde.id, dde.isProxy);
                    List<DocumentRef> deleted = session.removeOrphanVersions(new IdRef(docId));
                    if (!deleted.isEmpty()) {
                        log.debug("Removing orphan versions for: {} deleted: {}", docId, deleted);
                    }
                }
            });
        }

    }

}
