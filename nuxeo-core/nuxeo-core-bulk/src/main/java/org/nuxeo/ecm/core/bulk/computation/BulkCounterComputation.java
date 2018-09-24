/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Funsho David
 */
package org.nuxeo.ecm.core.bulk.computation;

import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.message.BulkCounter;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.BulkProcessor;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * Aggregates action's counter message and output command status.
 *
 * @since 10.2
 */
public class BulkCounterComputation extends AbstractComputation {

    private static final Log log = LogFactory.getLog(BulkCounterComputation.class);

    protected final int counterThresholdMs;

    protected final Map<String, Long> counters;

    public BulkCounterComputation(String counterComputationName, int counterThresholdMs) {
        super(counterComputationName, 1, 1);
        this.counterThresholdMs = counterThresholdMs;
        counters = new HashMap<>();
    }

    @Override
    public void init(ComputationContext context) {
        log.debug(String.format("Starting computation: %s, threshold: %dms", BulkProcessor.COUNTER_ACTION_NAME,
                counterThresholdMs));
        context.setTimer("counter", System.currentTimeMillis() + counterThresholdMs);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        if (!counters.isEmpty()) {
            KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
            counters.entrySet()
                    .stream()
                    .map(entry -> getStatusAndUpdate(kvStore, entry.getKey(), entry.getValue()))
                    .map(BulkCodecs.getStatusCodec()::encode)
                    .forEach(status -> context.produceRecord(BulkProcessor.KVWRITER_ACTION_NAME, key, status));
            counters.clear();
            context.askForCheckpoint();
        }
        context.setTimer("counter", System.currentTimeMillis() + counterThresholdMs);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        BulkCounter counter = BulkCodecs.getCounterCodec().decode(record.getData());
        String bulkId = counter.getCommandId();

        counters.computeIfPresent(bulkId, (k, processedDocs) -> processedDocs + counter.getProcessedDocuments());
        counters.putIfAbsent(bulkId, counter.getProcessedDocuments());
    }

    protected BulkStatus getStatusAndUpdate(KeyValueStore kvStore, String commandId, long processedDocs) {
        BulkStatus status = BulkCodecs.getStatusCodec().decode(kvStore.get(commandId + STATUS));
        long previousProcessedDocs = status.getProcessed();
        long currentProcessedDocs = previousProcessedDocs + processedDocs;
        if (currentProcessedDocs == status.getCount()) {
            status.setState(COMPLETED);
        }
        status.setProcessed(currentProcessedDocs);
        return status;
    }
}
