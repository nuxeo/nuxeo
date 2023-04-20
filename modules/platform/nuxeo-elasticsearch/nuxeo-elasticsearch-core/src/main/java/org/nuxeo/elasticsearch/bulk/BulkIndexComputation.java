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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.bulk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamNoRetryException;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.bulk.BackoffPolicy;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkProcessor;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.common.io.stream.ByteBufferStreamInput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.unit.ByteSizeUnit;
import org.opensearch.common.unit.ByteSizeValue;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.rest.RestStatus;

/**
 * A computation that submits elasticsearch requests using the bulk API.
 * <p>
 * Note that the retry policy is handled by the elasticsearch bulk processor.
 *
 * @since 10.3
 */
public class BulkIndexComputation extends AbstractComputation implements BulkProcessor.Listener {

    private static final Logger log = LogManager.getLogger(BulkIndexComputation.class);

    public static final String NAME = "bulk/bulkIndex";

    protected final int esBulkSize;

    protected final int esBulkActions;

    protected final int flushIntervalMs;

    protected BulkProcessor bulkProcessor;

    protected Codec<DataBucket> codec;

    protected boolean updates;

    protected boolean continueOnFailure;

    protected volatile boolean abort;

    public BulkIndexComputation(int esBulkSize, int esBulkActions, int flushInterval) {
        super(NAME, 1, 1);
        this.esBulkSize = esBulkSize;
        this.esBulkActions = esBulkActions;
        this.flushIntervalMs = flushInterval * 1000;
    }

    @Override
    public void init(ComputationContext context) {
        super.init(context);
        // note that we don't use setFlushInterval because this is done by our timer
        continueOnFailure = context.getPolicy().continueOnFailure();
        long backoffDelayMs = context.getPolicy().getRetryPolicy().getDelay().toMillis();
        int retries = context.getPolicy().getRetryPolicy().getMaxRetries();

        bulkProcessor = getESClient().bulkProcessorBuilder(this)
                                     .setConcurrentRequests(0)
                                     .setBulkSize(new ByteSizeValue(esBulkSize, ByteSizeUnit.BYTES))
                                     .setBulkActions(esBulkActions)
                                     .setBackoffPolicy(BackoffPolicy.exponentialBackoff(
                                             TimeValue.timeValueMillis(backoffDelayMs), retries))
                                     .build();
        codec = BulkCodecs.getDataBucketCodec();
        context.setTimer("flush", System.currentTimeMillis() + flushIntervalMs);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        if (updates) {
            // flush is sync because bulkProcessor is initialized with setConcurrentRequests(0)
            bulkProcessor.flush();
            if (abort) {
                destroy();
                throw new StreamNoRetryException("Aborting computation " + metadata.name() + " due to previous error.");
            }
            context.askForCheckpoint();
            updates = false;
        }
        context.setTimer("flush", System.currentTimeMillis() + flushIntervalMs);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStream, Record record) {
        if (abort) {
            return;
        }
        DataBucket in = codec.decode(record.getData());
        if (in.getCount() > 0) {
            BulkRequest bulkRequest = decodeRequest(in);
            BulkStatus delta = BulkStatus.deltaOf(in.getCommandId());
            delta.setProcessingStartTime(Instant.now());
            for (DocWriteRequest<?> request : bulkRequest.requests()) {
                bulkProcessor.add(request);
            }
            delta.setProcessed(in.getCount());
            delta.setProcessingEndTime(Instant.now());
            if (bulkRequest.numberOfActions() == 0) {
                delta.inError(in.getCount(), "Some documents were not accessible", 0);
            }
            AbstractBulkComputation.updateStatus(context, delta);
        }
        updates = true;
    }

    @Override
    public void destroy() {
        if (bulkProcessor != null) {
            bulkProcessor.close();
            bulkProcessor = null;
        }
    }

    protected ESClient getESClient() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        return esa.getClient();
    }

    protected BulkRequest decodeRequest(DataBucket bucket) {
        ByteBuffer buffer = ByteBuffer.wrap(bucket.getData());
        try (StreamInput in = new ByteBufferStreamInput(buffer)) {
            return new BulkRequest(in);
        } catch (IOException e) {
            throw new NuxeoException("Cannot load elastic bulk request from: " + bucket);
        }
    }

    // -------------------------------------------------------------------------------------
    // Elasticsearch bulk processor listener
    // the following methods are called from a different thread than the computation
    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
        log.debug("Creating elasticsearch bulk {} with {} action", executionId, request.numberOfActions());
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        log.debug("After bulk: {}, actions: {}, status: {}", executionId, request.numberOfActions(), response.status());
        if (!response.hasFailures()) {
            return;
        }
        MutableBoolean inError = new MutableBoolean(false);
        Arrays.stream(response.getItems()).filter(BulkItemResponse::isFailed).forEach(item -> {
            if (item.getFailure().getStatus() != RestStatus.CONFLICT) {
                log.info("Failure in bulk indexing: {}", item::getFailureMessage);
                inError.setTrue();
            } else {
                log.debug("Skipping version conflict: {}", item::getFailureMessage);
            }
        });
        if (inError.isTrue()) {
            log.error("Elasticsearch bulk {} returns with failures: {}", executionId, response.buildFailureMessage());
            if (!continueOnFailure) {
                abort = true;
            }
        }
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        log.error("Elasticsearch bulk {} fails, contains {} actions", executionId, request.numberOfActions(), failure);
        if (!continueOnFailure) {
            abort = true;
        }
    }
}
