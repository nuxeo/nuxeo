/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.stream;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.model.stream.DocumentDomainEvent;
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
import org.nuxeo.user.preferences.api.UserPreferencesService;

/**
 * A Processor that deletes user preferences on document deletion.
 *
 * @since 2025.16
 */
public class StreamUserDocPreferencesGC implements StreamProcessorTopology {

    public static final String COMPUTATION_NAME = "userPreferences/gc";

    public static final String STREAM_NAME = "source/document";

    private static final Logger log = LogManager.getLogger(StreamUserDocPreferencesGC.class);

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new UserDocPreferencesGCComputation(COMPUTATION_NAME),
                               List.of("i1:" + STREAM_NAME))
                       .build();
    }

    public static class UserDocPreferencesGCComputation extends AbstractComputation {

        protected final Codec<DocumentDomainEvent> codec;

        public UserDocPreferencesGCComputation(String name) {
            super(name, 1, 0);
            codec = Framework.getService(CodecService.class).getCodec("avro", DocumentDomainEvent.class);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            DocumentDomainEvent dde = codec.decode(record.getData());
            log.trace("Processing document domain event: {} for repository: {}, docId: {}", dde.event, dde.repository,
                    dde.id);
            if (DocumentEventTypes.INTERNAL_DOCUMENT_DELETED.equals(dde.event)) {
                userPreferencesGC(dde);
            }
            context.askForCheckpoint();
        }

        protected void userPreferencesGC(DocumentDomainEvent dde) {
            TransactionHelper.runInTransaction(() -> {
                try (NuxeoLoginContext ignored = Framework.loginSystem(dde.user)) {
                    CoreSession session = CoreInstance.getCoreSession(dde.repository);
                    log.debug("Try to clean orphan user preferences for document: {}", dde.id);
                    Framework.getService(UserPreferencesService.class).deleteAllForDocument(session, new IdRef(dde.id));
                }
            });
        }

    }

}
