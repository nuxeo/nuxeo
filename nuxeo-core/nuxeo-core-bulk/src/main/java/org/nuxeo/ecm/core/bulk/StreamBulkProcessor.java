/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import static java.lang.Integer.max;
import static java.lang.Math.min;
import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DOCUMENTSET_ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.RUNNING;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.SCHEDULED;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.SCROLLING_RUNNING;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Computation that consumes a {@link BulkCommand} and produce document ids. This scroller takes a query to execute on
 * DB (by scrolling) and then produce document id to the appropriate stream.
 *
 * @since 10.2
 */
public class StreamBulkProcessor implements StreamProcessorTopology {

    private static final Log log = LogFactory.getLog(StreamBulkProcessor.class);

    public static final String COUNTER_ACTION_NAME = "counter";

    public static final String KVWRITER_ACTION_NAME = "keyValueWriter";

    public static final String SCROLL_BATCH_SIZE_OPT = "scrollBatchSize";

    public static final String SCROLL_KEEP_ALIVE_SECONDS_OPT = "scrollKeepAlive";

    public static final String BUCKET_SIZE_OPT = "bucketSize";

    public static final String COUNTER_THRESHOLD_MS_OPT = "counterThresholdMs";

    public static final int DEFAULT_SCROLL_BATCH_SIZE = 100;

    public static final int DEFAULT_SCROLL_KEEPALIVE_SECONDS = 60;

    public static final int DEFAULT_BUCKET_SIZE = 50;

    public static final int DEFAULT_COUNTER_THRESHOLD_MS = 30000;

    @Override
    public Topology getTopology(Map<String, String> options) {
        // retrieve options
        int scrollBatchSize = getOptionAsInteger(options, SCROLL_BATCH_SIZE_OPT, DEFAULT_SCROLL_BATCH_SIZE);
        int scrollKeepAliveSeconds = getOptionAsInteger(options, SCROLL_KEEP_ALIVE_SECONDS_OPT,
                DEFAULT_SCROLL_KEEPALIVE_SECONDS);
        int bucketSize = getOptionAsInteger(options, BUCKET_SIZE_OPT, DEFAULT_BUCKET_SIZE);
        int counterThresholdMs = getOptionAsInteger(options, COUNTER_THRESHOLD_MS_OPT, DEFAULT_COUNTER_THRESHOLD_MS);

        // retrieve bulk actions to deduce output streams
        BulkAdminService service = Framework.getService(BulkAdminService.class);
        List<String> actions = service.getActions();
        List<String> mapping = new ArrayList<>();
        mapping.add("i1:" + DOCUMENTSET_ACTION_NAME);
        int i = 1;
        for (String action : actions) {
            mapping.add(String.format("o%s:%s", i, action));
            i++;
        }
        mapping.add(String.format("o%s:%s", i, KVWRITER_ACTION_NAME));

        return Topology.builder()
                       .addComputation( //
                               () -> new BulkDocumentScrollerComputation(DOCUMENTSET_ACTION_NAME, mapping.size(),
                                       scrollBatchSize, scrollKeepAliveSeconds, bucketSize), //
                               mapping)
                       .addComputation(() -> new CounterComputation(COUNTER_ACTION_NAME, counterThresholdMs),
                               Arrays.asList("i1:" + COUNTER_ACTION_NAME, "o1:" + KVWRITER_ACTION_NAME))
                       .addComputation(() -> new KeyValueWriterComputation(KVWRITER_ACTION_NAME),
                               Collections.singletonList("i1:" + KVWRITER_ACTION_NAME))
                       .build();
    }

    public static class BulkDocumentScrollerComputation extends AbstractComputation {

        protected final int scrollBatchSize;

        protected final int scrollKeepAliveSeconds;

        protected final int bucketSize;

        protected final List<String> documentIds;

        /**
         * @param name the computation name
         * @param nbOutputStreams the number of registered bulk action streams
         * @param scrollBatchSize the batch size to scroll
         * @param scrollKeepAliveSeconds the scroll lifetime
         * @param bucketSize the number of document to send per bucket
         */
        public BulkDocumentScrollerComputation(String name, int nbOutputStreams, int scrollBatchSize,
                int scrollKeepAliveSeconds, int bucketSize) {
            super(name, 1, nbOutputStreams);
            this.scrollBatchSize = scrollBatchSize;
            this.scrollKeepAliveSeconds = scrollKeepAliveSeconds;
            this.bucketSize = bucketSize;
            documentIds = new ArrayList<>(max(scrollBatchSize, bucketSize));
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            TransactionHelper.runInTransaction(() -> processRecord(context, record));
        }

        protected void processRecord(ComputationContext context, Record record) {
            KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
            try {
                String commandId = record.getKey();
                BulkCommand command = BulkCodecs.getBulkCommandCodec().decode(record.getData());
                BulkStatus currentStatus = BulkCodecs.getBulkStatusCodec().decode(kvStore.get(commandId + STATUS));
                if (!SCHEDULED.equals(currentStatus.getState())) {
                    log.error("Discard record: " + record + " because it's already building");
                    context.askForCheckpoint();
                    return;
                }
                currentStatus.setState(SCROLLING_RUNNING);
                context.produceRecord(KVWRITER_ACTION_NAME, commandId,
                        BulkCodecs.getBulkStatusCodec().encode(currentStatus));

                LoginContext loginContext;
                try {
                    loginContext = Framework.loginAsUser(command.getUsername());
                    try (CloseableCoreSession session = CoreInstance.openCoreSession(command.getRepository())) {
                        // scroll documents
                        Instant scrollStartTime = Instant.now();
                        ScrollResult<String> scroll = session.scroll(command.getQuery(), scrollBatchSize,
                                scrollKeepAliveSeconds);
                        long documentCount = 0;
                        long bucketNumber = 0;
                        while (scroll.hasResults()) {
                            List<String> docIds = scroll.getResults();
                            documentIds.addAll(docIds);
                            while (documentIds.size() >= bucketSize) {
                                // we use number of sent document to make record key unique
                                // key are prefixed with bulkId:, suffix are:
                                // bucketSize / 2 * bucketSize / ... / total document count
                                bucketNumber++;
                                produceBucket(context, command.getAction(), commandId, bucketNumber * bucketSize);
                            }

                            documentCount += docIds.size();
                            // next batch
                            scroll = session.scroll(scroll.getScrollId());
                            TransactionHelper.commitOrRollbackTransaction();
                            TransactionHelper.startTransaction();
                        }

                        // send remaining document ids
                        // there's at most one record because we loop while scrolling
                        if (!documentIds.isEmpty()) {
                            produceBucket(context, command.getAction(), commandId, documentCount);
                        } else {
                            context.askForCheckpoint();
                        }

                        Instant scrollEndTime = Instant.now();

                        currentStatus.setScrollStartTime(scrollStartTime);
                        currentStatus.setScrollEndTime(scrollEndTime);
                        currentStatus.setState(documentCount == 0 ? COMPLETED : RUNNING);
                        currentStatus.setCount(documentCount);
                        context.produceRecord(KVWRITER_ACTION_NAME, commandId,
                                BulkCodecs.getBulkStatusCodec().encode(currentStatus));

                    } finally {
                        loginContext.logout();
                    }
                } catch (LoginException e) {
                    throw new NuxeoException(e);
                }
            } catch (NuxeoException e) {
                log.error("Discard invalid record: " + record, e);
            }
        }

        /**
         * Produces a bucket as a record to appropriate bulk action stream.
         */
        protected void produceBucket(ComputationContext context, String action, String commandId, long nbDocSent) {
            List<String> docIds = documentIds.subList(0, min(bucketSize, documentIds.size()));
            // send these ids as keys to the appropriate stream
            context.produceRecord(action, BulkRecords.of(commandId, nbDocSent, docIds));
            context.askForCheckpoint();
            docIds.clear();
        }
    }

    public static class CounterComputation extends AbstractComputation {

        protected final int counterThresholdMs;

        protected final Map<String, Long> counters;

        public CounterComputation(String counterComputationName, int counterThresholdMs) {
            super(counterComputationName, 1, 1);
            this.counterThresholdMs = counterThresholdMs;
            counters = new HashMap<>();
        }

        @Override
        public void init(ComputationContext context) {
            log.debug(String.format("Starting: %s, threshold: %dms", COUNTER_ACTION_NAME, counterThresholdMs));
        }

        @Override
        public void processTimer(ComputationContext context, String key, long timestamp) {
            if (!counters.isEmpty()) {
                KeyValueStore kvStore = Framework.getService(KeyValueService.class)
                                                 .getKeyValueStore(BULK_KV_STORE_NAME);
                counters.entrySet()
                        .stream()
                        .map(entry -> getStatusAndUpdate(kvStore, entry.getKey(), entry.getValue()))
                        .map(BulkCodecs.getBulkStatusCodec()::encode)
                        .forEach(status -> context.produceRecord(KVWRITER_ACTION_NAME, key, status));
                counters.clear();
                context.askForCheckpoint();
            }
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            BulkCounter counter = BulkCodecs.getBulkCounterCodec().decode(record.getData());
            String bulkId = counter.getBulkId();
            boolean startTimer = counters.isEmpty();
            counters.computeIfPresent(bulkId, (k, processedDocs) -> processedDocs + counter.getProcessedDocuments());
            counters.putIfAbsent(bulkId, counter.getProcessedDocuments());
            if (startTimer) {
                context.setTimer("counter", System.currentTimeMillis() + counterThresholdMs);
            }
        }

        protected BulkStatus getStatusAndUpdate(KeyValueStore kvStore, String commandId, long processedDocs) {
            BulkStatus status = BulkCodecs.getBulkStatusCodec().decode(kvStore.get(commandId + STATUS));
            long previousProcessedDocs = status.getProcessed();
            long currentProcessedDocs = previousProcessedDocs + processedDocs;
            if (currentProcessedDocs == status.getCount()) {
                status.setState(COMPLETED);
            }
            status.setProcessed(currentProcessedDocs);
            return status;
        }
    }

    public static class KeyValueWriterComputation extends AbstractComputation {

        public KeyValueWriterComputation(String name) {
            super(name, 1, 0);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
            BulkStatus status = BulkCodecs.getBulkStatusCodec().decode(record.getData());
            kvStore.put(status.getId() + STATUS, record.getData());
            context.askForCheckpoint();
        }
    }

    // TODO copied from StreamAuditWriter - where can we put that ?
    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }
}
