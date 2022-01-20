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

import static java.lang.Math.min;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.ABORTED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.RUNNING;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.SCROLLING_RUNNING;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.BulkAdminService;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.scroll.DocumentScrollRequest;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Materializes the document set for a command.
 * <p>
 * Inputs:
 * <ul>
 * <li>i1: Reads a stream of {@link BulkCommand} sharded by action</li>
 * </ul>
 * <p>
 * Outputs:
 * <ul>
 * <li>- "actionName": Writes {@link BulkBucket} into the action stream</li>
 * <li>- "status": Writes {@link BulkStatus} into the action stream</li>
 * </ul>
 *
 * @since 10.2
 */
public class BulkScrollerComputation extends AbstractComputation {

    private static final Logger log = LogManager.getLogger(BulkScrollerComputation.class);

    public static final int MAX_SCROLL_SIZE = 4_000;

    // @since 2021.15 threshold to trace Big Bulk Command (BBC)
    public static final long BIG_BULK_COMMAND_THRESHOLD = 50_000;

    protected final int scrollBatchSize;

    protected final int scrollKeepAliveSeconds;

    protected final List<String> documentIds;

    protected final boolean produceImmediate;

    // @since 11.4
    protected final long produceImmediateThreshold;

    protected final int transactionTimeoutSeconds;

    protected int scrollSize;

    protected int bucketSize;

    public static Builder builder(String name, int nbOutputStreams) {
        return new Builder(name, nbOutputStreams);
    }

    protected BulkScrollerComputation(Builder builder) {
        super(builder.name, 1, builder.nbOutputStreams);
        this.scrollBatchSize = builder.scrollBatchSize;
        this.scrollKeepAliveSeconds = builder.scrollKeepAliveSeconds;
        this.produceImmediate = builder.produceImmediate;
        this.produceImmediateThreshold = builder.produceImmediateThreshold;
        this.transactionTimeoutSeconds = Math.toIntExact(builder.transactionTimeout.getSeconds());
        documentIds = new ArrayList<>(scrollBatchSize);
    }

    /**
     * @param name the computation name
     * @param nbOutputStreams the number of registered bulk action streams
     * @param scrollBatchSize the batch size to scroll
     * @param scrollKeepAliveSeconds the scroll lifetime
     * @param produceImmediate whether or not the record should be produced immedialitely while scrolling
     */
    public BulkScrollerComputation(String name, int nbOutputStreams, int scrollBatchSize, int scrollKeepAliveSeconds,
            boolean produceImmediate) {
        this(builder(name, nbOutputStreams).setScrollBatchSize(scrollBatchSize)
                                           .setScrollKeepAliveSeconds(scrollKeepAliveSeconds)
                                           .setProduceImmediate(produceImmediate));
    }

    // @since 11.2
    public BulkScrollerComputation(String name, int nbOutputStreams, int scrollBatchSize, int scrollKeepAliveSeconds,
            Duration transactionTimeout, boolean produceImmediate) {
        this(builder(name, nbOutputStreams).setScrollBatchSize(scrollBatchSize)
                .setScrollKeepAliveSeconds(scrollKeepAliveSeconds)
                .setProduceImmediate(produceImmediate)
                .setTransactionTimeout(transactionTimeout));
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        boolean newTransaction = true;
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            newTransaction = false;
            log.warn("Already inside a transaction, timeout cannot be applied, record: " + record, new Throwable("stack"));
        } else if (!TransactionHelper.startTransaction(transactionTimeoutSeconds)) {
            throw new TransactionRuntimeException("Cannot start transaction");
        }
        try {
            processRecord(context, record);
        } finally {
            if (newTransaction) {
                // Always rollback because we don't write anything
                TransactionHelper.setTransactionRollbackOnly();
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    protected void processRecord(ComputationContext context, Record record) {
        BulkCommand command = null;
        String commandId = null;
        try {
            command = BulkCodecs.getCommandCodec().decode(record.getData());
            commandId = command.getId();
            computeScrollAndBucketSize(command);
            updateStatusAsScrolling(context, commandId);

            long documentCount = 0;
            long bucketNumber = 1;
            final long queryLimit = getQueryLimit(command);
            boolean limitReached = false;
            boolean bigBulkCommand = false;
            scrollLoop:
            try (Scroll scroll = buildScroll(command)) {
                while (scroll.hasNext()) {
                    if (isAbortedCommand(commandId)) {
                        log.debug("Skipping aborted command: {}", commandId);
                        context.askForCheckpoint();
                        return;
                    }
                    List<String> docIds = scroll.next();
                    int scrollCount = docIds.size();
                    if (documentCount + scrollCount < queryLimit) {
                        documentIds.addAll(docIds);
                    } else {
                        scrollCount = Math.toIntExact(queryLimit - documentCount);
                        documentIds.addAll(docIds.subList(0, scrollCount));
                        limitReached = true;
                    }
                    while (documentIds.size() >= bucketSize) {
                        produceBucket(context, command.getAction(), commandId, bucketSize, bucketNumber++, documentCount);
                    }
                    documentCount += scrollCount;
                    if (!bigBulkCommand && documentCount > BIG_BULK_COMMAND_THRESHOLD) {
                        log.warn("BBC: {} (Big Bulk Command) detected, scrolling more than {} items: {}.", commandId,
                                BIG_BULK_COMMAND_THRESHOLD, command);
                        bigBulkCommand = true;
                    }
                    if (limitReached) {
                        log.warn("Scroll limit {} reached for command {}", queryLimit, commandId);
                        break scrollLoop;
                    }
                }
            }

            // send remaining document ids
            // there's at most one record because we loop while scrolling
            if (!documentIds.isEmpty()) {
                produceBucket(context, command.getAction(), commandId, bucketSize, bucketNumber++, documentCount);
            }
            // update status after scroll when we handle the scroller
            updateStatusAfterScroll(context, commandId, documentCount, limitReached);
            if (bigBulkCommand) {
                log.warn("BBC: {} scroll done: {} items.", commandId, documentCount);
            }
        } catch (IllegalArgumentException | QueryParseException | DocumentNotFoundException e) {
            log.error("Invalid query results in an empty document set: {}", command, e);
            updateStatusAfterScroll(context, commandId, "Invalid query");
        } catch (NuxeoException e) {
            if (command != null) {
                log.error("Invalid command produces an empty document set: {}", command, e);
                updateStatusAfterScroll(context, command.getId(), "Invalid command");
            } else {
                log.error("Discard invalid record: {}", record, e);
            }
        }
        context.askForCheckpoint();
    }

    private long getQueryLimit(BulkCommand command) {
        Long limit = command.getQueryLimit();
        if (limit == null || limit <= 0) {
            return Long.MAX_VALUE;
        }
        return limit;
    }

    protected Scroll buildScroll(BulkCommand command) {
        ScrollRequest request;
        if (command.useGenericScroller()) {
            request = GenericScrollRequest.builder(command.getScroller(), command.getQuery())
                                          .options(command.getParams())
                                          .size(scrollSize)
                                          .build();

        } else {
            request = DocumentScrollRequest.builder(command.getQuery())
                                           .username(command.getUsername())
                                           .repository(command.getRepository())
                                           .size(scrollSize)
                                           .timeout(Duration.ofSeconds(scrollKeepAliveSeconds))
                                           .name(command.getScroller())
                                           .build();
        }
        ScrollService service = Framework.getService(ScrollService.class);
        return service.scroll(request);
    }

    protected void computeScrollAndBucketSize(BulkCommand command) {
        bucketSize = command.getBucketSize() > 0 ? command.getBucketSize()
                : Framework.getService(BulkAdminService.class).getBucketSize(command.getAction());
        scrollSize = scrollBatchSize;
        if (bucketSize > scrollSize) {
            if (bucketSize <= MAX_SCROLL_SIZE) {
                scrollSize = bucketSize;
            } else {
                log.warn("Bucket size: {} too big for command: {}, reduce to: {}", bucketSize, command,
                        MAX_SCROLL_SIZE);
                scrollSize = bucketSize = MAX_SCROLL_SIZE;
            }
        }
    }

    protected boolean isAbortedCommand(String commandId) {
        BulkService bulkService = Framework.getService(BulkService.class);
        BulkStatus status = bulkService.getStatus(commandId);
        return ABORTED.equals(status.getState());
    }

    protected void updateStatusAsScrolling(ComputationContext context, String commandId) {
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        delta.setState(SCROLLING_RUNNING);
        delta.setScrollStartTime(Instant.now());
        ((ComputationContextImpl) context).produceRecordImmediate(STATUS_STREAM, commandId,
                BulkCodecs.getStatusCodec().encode(delta));
    }

    protected void updateStatusAfterScroll(ComputationContext context, String commandId, String errorMessage) {
        updateStatusAfterScroll(context, commandId, 0, errorMessage, false);
    }

    protected void updateStatusAfterScroll(ComputationContext context, String commandId, long documentCount,
            boolean limited) {
        updateStatusAfterScroll(context, commandId, documentCount, null, limited);
    }

    protected void updateStatusAfterScroll(ComputationContext context, String commandId, long documentCount,
            String errorMessage, boolean limited) {
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        if (errorMessage != null) {
            delta.inError(errorMessage);
        }
        if (documentCount == 0) {
            delta.setState(COMPLETED);
            delta.setCompletedTime(Instant.now());
        } else {
            delta.setState(RUNNING);
        }
        delta.setScrollEndTime(Instant.now());
        delta.setTotal(documentCount);
        delta.setQueryLimitReached(limited);
        ((ComputationContextImpl) context).produceRecordImmediate(STATUS_STREAM, commandId,
                BulkCodecs.getStatusCodec().encode(delta));
    }

    /**
     * Produces a bucket as a record to appropriate bulk action stream.
     */
    protected void produceBucket(ComputationContext context, String action, String commandId, int bucketSize, long bucketNumber,
            long documentCount) {
        List<String> ids = documentIds.subList(0, min(bucketSize, documentIds.size()));
        BulkBucket bucket = new BulkBucket(commandId, ids);
        String key = commandId + ":" + Long.toString(bucketNumber);
        Record record = Record.of(key, BulkCodecs.getBucketCodec().encode(bucket));
        if (produceImmediate || (produceImmediateThreshold > 0 && documentCount > produceImmediateThreshold)) {
            ComputationContextImpl contextImpl = (ComputationContextImpl) context;
            if (!contextImpl.getRecords(action).isEmpty()) {
                flushRecords(contextImpl, action, commandId);
            }
            contextImpl.produceRecordImmediate(action, record);
        } else {
            context.produceRecord(action, record);
        }
        ids.clear(); // this clear the documentIds part that has been sent
    }

    protected void flushRecords(ComputationContextImpl contextImpl, String action,  String commandId) {
        log.warn("Scroller records threshold reached ({}) for action: {} on command: {}, flushing records downstream",
                produceImmediateThreshold, action, commandId);
        contextImpl.getRecords(action)
                   .forEach(record -> contextImpl.produceRecordImmediate(action, record));
        contextImpl.getRecords(action).clear();
    }

    /**
     * @since 11.4
     */
    public static class Builder {
        protected String name;

        protected int nbOutputStreams;

        protected int scrollBatchSize;

        protected int scrollKeepAliveSeconds;

        protected boolean produceImmediate;

        protected int produceImmediateThreshold;

        protected Duration transactionTimeout;

        protected long queryLimit;

        /**
         * @param name the computation name
         * @param nbOutputStream the number of registered bulk action streams
         */
        public Builder(String name, int nbOutputStream) {
            this.name = name;
            this.nbOutputStreams = nbOutputStream;
        }

        /**
         * @param scrollBatchSize the batch size to scroll
         */
        public Builder setScrollBatchSize(int scrollBatchSize) {
            this.scrollBatchSize = scrollBatchSize;
            return this;
        }

        /**
         * @param scrollKeepAliveSeconds the scroll lifetime between fetch
         */
        public Builder setScrollKeepAliveSeconds(int scrollKeepAliveSeconds) {
            this.scrollKeepAliveSeconds = scrollKeepAliveSeconds;
            return this;
        }

        /**
         * @param produceImmediate whether or not the record should be produced immediately while scrolling
         */
        public Builder setProduceImmediate(boolean produceImmediate) {
            this.produceImmediate = produceImmediate;
            return this;
        }

        /**
         * @param produceImmediateThreshold produce record immediately after the threshold to prevent OOM
         */
        public Builder setProduceImmediateThreshold(int produceImmediateThreshold) {
            this.produceImmediateThreshold = produceImmediateThreshold;
            return this;
        }

        /**
         * @param transactionTimeout set an explicit transaction timeout for the scroll
         */
        public Builder setTransactionTimeout(Duration transactionTimeout) {
            this.transactionTimeout = transactionTimeout;
            return this;
        }

        public BulkScrollerComputation build() {
            return new BulkScrollerComputation(this);
        }
    }
}
