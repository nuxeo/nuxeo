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
package org.nuxeo.lib.stream.computation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.jodah.failsafe.Failsafe;

/**
 * An abstract {@link Computation} that processes records by batch with a retry mechanism.<br/>
 *
 * @since 10.3
 */
public abstract class AbstractBatchComputation extends AbstractComputation {

    private static final Log log = LogFactory.getLog(AbstractBatchComputation.class);

    public static final String TIMER_BATCH = "batch";

    protected final ComputationPolicy policy;

    protected final List<Record> batchRecords;

    protected String currentInputStream;

    protected boolean newBatch = true;

    protected final long thresholdMillis;

    /**
     * Constructor
     *
     * @param name the name of the computation
     * @param nbInputStreams the number of input streams
     * @param nbOutputStreams the number of output streams
     * @param policy the policy to manage batch, retries and fallback
     */
    public AbstractBatchComputation(String name, int nbInputStreams, int nbOutputStreams, ComputationPolicy policy) {
        super(name, nbInputStreams, nbOutputStreams);
        this.policy = policy;
        thresholdMillis = policy.getBatchThreshold().toMillis();
        batchRecords = new ArrayList<>(policy.batchCapacity);
    }

    /**
     * Called when:<br>
     * - the batch capacity is reached<br/>
     * - the time threshold is reached<br/>
     * - the inputStreamName has changed<br/>
     * If this method raises an exception the retry policy is applied.
     *
     * @param context used to send records to output streams, note that the checkpoint is managed automatically.
     * @param inputStreamName the input streams where the records are coming from
     * @param records the batch of records
     */
    public abstract void batchProcess(ComputationContext context, String inputStreamName, List<Record> records);

    /**
     * Called when the retry policy has failed.
     */
    public abstract void batchFailure(ComputationContext context, String inputStreamName, List<Record> records);

    @Override
    public void init(ComputationContext context) {
        context.setTimer(TIMER_BATCH, System.currentTimeMillis() + thresholdMillis);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        if (!TIMER_BATCH.equals(key)) {
            return;
        }
        if (!batchRecords.isEmpty()) {
            processBatch(context);
        }
        context.setTimer(TIMER_BATCH, System.currentTimeMillis() + thresholdMillis);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        if (!inputStreamName.equals(currentInputStream) && !batchRecords.isEmpty()) {
            processBatch(context);
        }
        if (newBatch) {
            currentInputStream = inputStreamName;
            newBatch = false;
        }
        batchRecords.add(record);
        if (batchRecords.size() >= policy.getBatchCapacity()) {
            processBatch(context);
        }
    }

    protected void processBatch(ComputationContext context) {
        Failsafe.with(policy.getRetryPolicy())
                .onSuccess(ret -> checkpointBatch(context))
                .onFailure(failure -> processFailure(context, failure))
                .onRetry(failure -> processRetry(context, failure))
                .withFallback(() -> processFallback(context))
                .run(() -> batchProcess(context, currentInputStream, batchRecords));
    }

    protected void processFallback(ComputationContext context) {
        if (policy.isSkipFailure()) {
            batchRecords.forEach(record -> log.error(
                    String.format("Computation %s skips processing of record because of batch failure: %s",
                            metadata.name(), record)));
            checkpointBatch(context);
        } else {
            log.error(String.format("Computation %s aborts after a failure in batch", metadata.name()));
            batchRecords.forEach(record -> log.warn("Record not processed because of the batch failure: " + record));
            context.cancelAskForCheckpoint();
            context.askForTermination();
        }
    }

    /**
     * Called before retrying, can be overridden
     *
     * @param context Computation context that could be used
     * @param failure
     */
    protected void processRetry(ComputationContext context, Throwable failure) {
        log.warn(String.format(
                "Computation: %s fails to process batch of %d records from stream: %s, policy: %s, retrying ...",
                metadata.name(), batchRecords.size(), currentInputStream, policy), failure);
    }

    protected void checkpointBatch(ComputationContext context) {
        context.askForCheckpoint();
        batchRecords.clear();
        newBatch = true;
    }

    protected void processFailure(ComputationContext context, Throwable failure) {
        log.error(String.format(
                "Computation: %s fails to process batch of %d records from stream: %s, after applying retries: %s",
                metadata.name(), batchRecords.size(), currentInputStream, policy), failure);
        batchFailure(context, currentInputStream, batchRecords);
    }

}
