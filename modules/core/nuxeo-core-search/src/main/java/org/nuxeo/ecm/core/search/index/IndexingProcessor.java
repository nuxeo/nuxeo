/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search.index;

import static org.nuxeo.ecm.core.search.SearchClient.Capability.INDEXING;
import static org.nuxeo.ecm.core.search.index.IndexingAction.ACTION_NAME;
import static org.nuxeo.ecm.core.search.index.IndexingDomainEventProducer.CODEC_NAME;
import static org.nuxeo.ecm.core.search.index.commands.IndexingCommand.Type.DELETE;
import static org.nuxeo.ecm.core.search.index.commands.IndexingCommand.Type.UPDATE_DIRECT_CHILDREN;
import static org.nuxeo.runtime.api.login.LoginComponent.SYSTEM_USERNAME;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.search.BulkIndexingRequest;
import org.nuxeo.ecm.core.search.IndexingRequest;
import org.nuxeo.ecm.core.search.SearchClient;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractBatchComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 2025.0
 */
public class IndexingProcessor implements StreamProcessorTopology {
    private static final Logger log = LogManager.getLogger(IndexingProcessor.class);

    public static final String SYNC_COMPUTATION_NAME = "indexing/synchronous";

    public static final String ASYNC_COMPUTATION_NAME = "indexing/asynchronous";

    public static final String STREAM_NAME = "source/indexing";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new SynchronousIndexingComputation(SYNC_COMPUTATION_NAME),
                               List.of("i1:" + STREAM_NAME))
                       .addComputation(() -> new AsynchronousIndexingComputation(ASYNC_COMPUTATION_NAME),
                               List.of("i1:" + STREAM_NAME))
                       .build();
    }

    public static abstract class AbstractIndexingComputation extends AbstractBatchComputation {

        protected final Codec<IndexingDomainEvent> codec;

        public AbstractIndexingComputation(String name) {
            super(name, 1, 0);
            codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, IndexingDomainEvent.class);
        }

        protected int indexSimpleEvents(List<IndexingDomainEvent> events, boolean refresh) {
            if (events.isEmpty()) {
                log.debug("No indexing events");
                return 0;
            }
            return events.stream()
                         .map(IndexingDomainEvent::getRepository)
                         .distinct()
                         .mapToInt(repo -> indexSimpleEvents(repo, events, refresh))
                         .sum();
        }

        protected int indexSimpleEvents(String repository, List<IndexingDomainEvent> allEvents, boolean refresh) {
            var events = allEvents.stream().filter(event -> repository.equals(event.getRepository())).toList();
            if (events.isEmpty()) {
                log.debug("No indexing events");
                return 0;
            }
            SearchService searchService = Framework.getService(SearchService.class);
            SearchIndexingService indexingService = Framework.getService(SearchIndexingService.class);
            var indexes = searchService.getIndexNames(repository);
            if (indexes.isEmpty()) {
                log.warn("No SearchIndex found for repository: {}, skipping indexing of {} events", repository,
                        events.size());
                return 0;
            }
            var requestBuilder = BulkIndexingRequest.buildRequest(refresh);
            for (IndexingDomainEvent event : events) {
                IndexingRequest request;
                if (DELETE.name().equals(event.getEvent())) {
                    if (event.isRecurse()) {
                        request = IndexingRequest.deleteRecursive(event.getDocId(), event.getPath());
                    } else {
                        request = IndexingRequest.delete(event.getDocId());
                    }
                } else {
                    // a recurse event indexes its descendants with a bulk command, the document itself is always
                    // indexed here with a simple upsert
                    request = IndexingRequest.upsert(event.getDocId());
                }
                requestBuilder.add(request);
            }
            for (String index : indexes) {
                var searchIndex = searchService.getSearchIndex(index);
                SearchClient client = indexingService.getClient(searchIndex.client());
                if (!client.hasCapability(INDEXING)) {
                    continue;
                }
                indexingService.indexDocuments(requestBuilder.build(searchIndex));
            }
            return requestBuilder.size();
        }
    }

    public static class SynchronousIndexingComputation extends AbstractIndexingComputation {

        public SynchronousIndexingComputation(String name) {
            super(name);
        }

        @Override
        protected void batchProcess(ComputationContext context, String inputStreamName, List<Record> records) {
            List<IndexingDomainEvent> events = records.stream()
                                                      .map(r -> codec.decode(r.getData()))
                                                      .filter(IndexingDomainEvent::isSync)
                                                      .toList();
            if (!events.isEmpty()) {
                // noinspection deprecation
                log.info("Indexing sync events, up to offset: {}", context::getLastOffset);
                int count = indexSimpleEvents(events, true);
                log.info("Indexing of {}/{} completed", count, events.size());
            }
        }

        @Override
        public void batchFailure(ComputationContext context, String inputStreamName, List<Record> records) {

        }
    }

    public static class AsynchronousIndexingComputation extends AbstractIndexingComputation {

        public AsynchronousIndexingComputation(String name) {
            super(name);
        }

        @Override
        protected void batchProcess(ComputationContext context, String inputStreamName, List<Record> records) {
            List<IndexingDomainEvent> events = records.stream()
                                                      .map(Record::getData)
                                                      .map(codec::decode)
                                                      .filter(Predicate.not(IndexingDomainEvent::isSync))
                                                      .toList();
            if (events.isEmpty()) {
                log.debug("No async events to process");
                return;
            }
            // noinspection deprecation
            log.info("Indexing async simple events, up to offset: {}", context::getLastOffset);
            int count = indexSimpleEvents(events, false);
            log.info("Async indexing of {}/{} completed", count, events.size());

            // noinspection deprecation
            log.debug("Indexing async recurse events, up to offset: {}", context::getLastOffset);
            count = 0;
            for (IndexingDomainEvent event : events) {
                if (!event.isRecurse()) {
                    continue;
                }
                if (DELETE.toString().equals(event.getEvent())) {
                    // recurse delete is processed as simple events
                    continue;
                }
                String nxql;
                if (UPDATE_DIRECT_CHILDREN.toString().equals(event.getEvent())) {
                    nxql = String.format("SELECT ecm:uuid FROM Document WHERE ecm:parentId = '%s'", event.docId);
                } else {
                    nxql = String.format("SELECT ecm:uuid FROM Document WHERE ecm:ancestorId = '%s'", event.docId);
                }
                BulkService bs = Framework.getService(BulkService.class);
                BulkCommand command = new BulkCommand.Builder(ACTION_NAME, nxql, SYSTEM_USERNAME).build();
                String commandId = bs.submitTransactional(command);
                log.info("Scheduling bulk indexing: {} on: {}", commandId, nxql);
                count++;
            }
            log.debug("Async indexing: {} recurse events completed", count);
        }

        @Override
        public void batchFailure(ComputationContext context, String inputStreamName, List<Record> records) {

        }

    }

}
