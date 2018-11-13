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

/**
 * An abstract {@link Computation} that processes records by batch.
 * <p>
 * The batch capacity and threshold are defined in the computation policy.
 *
 * @since 10.3
 */
public abstract class AbstractBatchComputation extends AbstractComputation {

    private static final Log log = LogFactory.getLog(AbstractBatchComputation.class);

    public static final String TIMER_BATCH = "batch";

    protected List<Record> batchRecords;

    protected String currentInputStream;

    protected boolean newBatch = true;

    protected long thresholdMillis;

    /**
     * Constructor
     *
     * @param name the name of the computation
     * @param nbInputStreams the number of input streams
     * @param nbOutputStreams the number of output streams
     */
    public AbstractBatchComputation(String name, int nbInputStreams, int nbOutputStreams) {
        super(name, nbInputStreams, nbOutputStreams);
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
    protected abstract void batchProcess(ComputationContext context, String inputStreamName, List<Record> records);

    /**
     * Called when the retry policy has failed.
     */
    public abstract void batchFailure(ComputationContext context, String inputStreamName, List<Record> records);

    @Override
    public void init(ComputationContext context) {
        thresholdMillis = context.getPolicy().getBatchThreshold().toMillis();
        context.setTimer(TIMER_BATCH, System.currentTimeMillis() + thresholdMillis);
        batchRecords = new ArrayList<>(context.getPolicy().batchCapacity);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        if (!TIMER_BATCH.equals(key)) {
            return;
        }
        if (!batchRecords.isEmpty()) {
            batchProcess(context);
        }
        context.setTimer(TIMER_BATCH, System.currentTimeMillis() + thresholdMillis);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        if (!inputStreamName.equals(currentInputStream) && !batchRecords.isEmpty()) {
            batchProcess(context);
        }
        if (newBatch) {
            currentInputStream = inputStreamName;
            newBatch = false;
        }
        // on retry we don't want to add multiple time the same record
        if (!batchRecords.contains(record)) {
            batchRecords.add(record);
        }
        if (batchRecords.size() >= context.getPolicy().getBatchCapacity()) {
            batchProcess(context);
        }
    }

    private void batchProcess(ComputationContext context) {
        batchProcess(context, currentInputStream, batchRecords);
        checkpointBatch(context);
    }

    protected void checkpointBatch(ComputationContext context) {
        context.askForCheckpoint();
        batchRecords.clear();
        newBatch = true;
    }

    @Override
    public void processRetry(ComputationContext context, Throwable failure) {
        log.warn(String.format("Computation: %s fails to process batch of %d records, last record: %s, retrying ...",
                metadata.name(), batchRecords.size(), context.getLastOffset()), failure);
    }

    @Override
    public void processFailure(ComputationContext context, Throwable failure) {
        log.error(String.format(
                "Computation: %s fails to process batch of %d records after retries, last record: %s, policy: %s",
                metadata.name(), batchRecords.size(), context.getLastOffset(), context.getPolicy()), failure);
        batchFailure(context, currentInputStream, batchRecords);
        batchRecords.clear();
        newBatch = true;
    }

}
