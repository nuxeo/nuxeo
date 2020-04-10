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
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.RestStatus;
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

/**
 * A computation that submits elasticsearch requests using the bulk API.
 * <p>
 * Note that the retry policy is handled by the elasticsearch bulk processor.
 *
 * @since 10.3
 */
public class BulkIndexComputation extends AbstractComputation implements BulkProcessor.Listener {
    private static final Log log = LogFactory.getLog(BulkIndexComputation.class);

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
        if (abort) {
            throw new NuxeoException("Terminate computation due to previous error");
        }
        if (updates) {
            // flush is sync because bulkProcessor is initialized with setConcurrentRequests(0)
            bulkProcessor.flush();
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
            for (DocWriteRequest<?> request : bulkRequest.requests()) {
                bulkProcessor.add(request);
            }
            BulkStatus delta = BulkStatus.deltaOf(in.getCommandId());
            delta.setProcessed(in.getCount());
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
        BulkRequest ret = new BulkRequest();
        ByteBuffer buffer = ByteBuffer.wrap(bucket.getData());
        try (StreamInput in = new ByteBufferStreamInput(buffer)) {
            ret.readFrom(in);
            return ret;
        } catch (IOException e) {
            throw new NuxeoException("Cannot load elastic bulk request from: " + bucket);
        }
    }

    // -------------------------------------------------------------------------------------
    // Elasticsearch bulk processor listener
    // the following methods are called from a different thread than the computation
    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Creating elasticsearch bulk %s with %d action", executionId,
                    request.numberOfActions()));
        }
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("After bulk: %s, actions: %d, status: %s", executionId, request.numberOfActions(),
                    response.status()));
        }
        if (!response.hasFailures()) {
            return;
        }
        MutableBoolean inError = new MutableBoolean(false);
        Arrays.stream(response.getItems()).filter(BulkItemResponse::isFailed).forEach(item -> {
            if (item.getFailure().getStatus() != RestStatus.CONFLICT) {
                log.warn("Failure in bulk indexing: " + item.getFailureMessage());
                inError.setTrue();
            } else if (log.isDebugEnabled()) {
                log.debug("Skipping version conflict: " + item.getFailureMessage());
            }
        });
        if (inError.isTrue()) {
            log.error(String.format("Elasticsearch bulk %s returns with failures: %s", executionId,
                    response.buildFailureMessage()));
            if (!continueOnFailure) {
                abort = true;
            }
        }
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        log.error(String.format("Elasticsearch bulk %s fails, contains %d actions", executionId,
                request.numberOfActions()), failure);
        if (!continueOnFailure) {
            abort = true;
        }
    }
}
