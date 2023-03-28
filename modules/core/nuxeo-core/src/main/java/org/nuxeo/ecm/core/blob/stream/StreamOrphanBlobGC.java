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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.blob.stream;

import static org.nuxeo.ecm.core.blob.DocumentBlobManagerComponent.BLOBS_CANDIDATE_FOR_DELETION_EVENT;
import static org.nuxeo.ecm.core.blob.stream.BlobDomainEventProducer.CODEC_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 2023
 */
public class StreamOrphanBlobGC implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(StreamOrphanBlobGC.class);

    public static final String COMPUTATION_NAME = "blob/gc";

    public static final String STREAM_NAME = "source/blob";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new BlobGCComputation(COMPUTATION_NAME),
                               Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public static class BlobGCComputation extends AbstractComputation {

        protected final Codec<BlobDomainEvent> codec;

        protected boolean canPerformDelete;

        public BlobGCComputation(String name) {
            super(name, 1, 0);
            codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, BlobDomainEvent.class);
        }

        protected DocumentBlobManager getDocumentBlobManager() {
            return Framework.getService(DocumentBlobManager.class);
        }

        @Override
        public void init(ComputationContext context) {
            RepositoryService rs = Framework.getService(RepositoryService.class);
            boolean allReposWithBlobKeys = rs.getRepositoryNames()
                                             .stream()
                                             .allMatch(repo -> rs.getRepository(repo)
                                                                 .hasCapability(Repository.CAPABILITY_QUERY_BLOB_KEYS));
            boolean hasSharedStorage = getDocumentBlobManager().hasSharedStorage();
            if (allReposWithBlobKeys && hasSharedStorage) {
                log.warn("Cannot delete blob because a shared storage has been detected.");
            }
            canPerformDelete = allReposWithBlobKeys && !hasSharedStorage;
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            BlobDomainEvent bde = codec.decode(record.getData());
            log.trace("Processsing blob domain event: {} for repository: {}, docId: {}, blobKey: {}", bde.event,
                    bde.repository, bde.docId, bde.blobKey);
            if (canPerformDelete && BLOBS_CANDIDATE_FOR_DELETION_EVENT.equals(bde.event)) {
                try {
                    getDocumentBlobManager().deleteBlob(bde.repository, bde.blobKey, false);
                } catch (IllegalArgumentException e) {
                    log.warn("Cannot delete blob: {}, repository: {}, with error: {}", bde.blobKey, bde.repository, e);
                } catch (IOException e) {
                    // Worth trying again on IOException, could be network or service disruption.
                    throw new NuxeoException(e);
                }
            }
            context.askForCheckpoint();
        }

    }

}
