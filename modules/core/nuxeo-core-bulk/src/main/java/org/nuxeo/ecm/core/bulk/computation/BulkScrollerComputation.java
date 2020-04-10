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
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

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

    protected final int scrollBatchSize;

    protected final int scrollKeepAliveSeconds;

    protected final List<String> documentIds;

    protected final boolean produceImmediate;

    protected int scrollSize;

    protected int bucketSize;

    protected String actionStream;

    /**
     * @param name the computation name
     * @param nbOutputStreams the number of registered bulk action streams
     * @param scrollBatchSize the batch size to scroll
     * @param scrollKeepAliveSeconds the scroll lifetime
     * @param produceImmediate whether or not the record should be produced immedialitely while scrolling
     */
    public BulkScrollerComputation(String name, int nbOutputStreams, int scrollBatchSize, int scrollKeepAliveSeconds,
            boolean produceImmediate) {
        super(name, 1, nbOutputStreams);
        this.scrollBatchSize = scrollBatchSize;
        this.scrollKeepAliveSeconds = scrollKeepAliveSeconds;
        this.produceImmediate = produceImmediate;
        documentIds = new ArrayList<>(scrollBatchSize);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        TransactionHelper.runInTransaction(() -> processRecord(context, record));
    }

    protected void processRecord(ComputationContext context, Record record) {
        BulkCommand command = null;
        String commandId = null;
        try {
            command = BulkCodecs.getCommandCodec().decode(record.getData());
            commandId = command.getId();
            getCommandConfiguration(command);
            updateStatusAsScrolling(context, commandId);

            long documentCount = 0;
            long bucketNumber = 1;
            try (Scroll scroll = buildScroll(command)) {
                while (scroll.hasNext()) {
                    if (isAbortedCommand(commandId)) {
                        log.debug("Skipping aborted command: {}", commandId);
                        context.askForCheckpoint();
                        return;
                    }
                    List<String> docIds = scroll.next();
                    documentIds.addAll(docIds);
                    while (documentIds.size() >= bucketSize) {
                        produceBucket(context, commandId, bucketSize, bucketNumber++);
                    }
                    documentCount += docIds.size();
                }
            }
            // send remaining document ids
            // there's at most one record because we loop while scrolling
            if (!documentIds.isEmpty()) {
                produceBucket(context, commandId, bucketSize, bucketNumber++);
            }
            updateStatusAfterScroll(context, commandId, documentCount);
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

    protected Scroll buildScroll(BulkCommand command) {
        ScrollRequest request = DocumentScrollRequest.builder(command.getQuery())
                                                     .username(command.getUsername())
                                                     .repository(command.getRepository())
                                                     .size(scrollSize)
                                                     .timeout(Duration.ofSeconds(scrollKeepAliveSeconds))
                                                     .name(command.getScroller())
                                                     .build();
        ScrollService service = Framework.getService(ScrollService.class);
        return service.scroll(request);
    }

    protected void getCommandConfiguration(BulkCommand command) {
        BulkAdminService actionService = Framework.getService(BulkAdminService.class);
        bucketSize = command.getBucketSize() > 0 ? command.getBucketSize()
                : actionService.getBucketSize(command.getAction());
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
        actionStream = actionService.getInputStream(command.getAction());
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
        updateStatusAfterScroll(context, commandId, 0, errorMessage);
    }

    protected void updateStatusAfterScroll(ComputationContext context, String commandId, long documentCount) {
        updateStatusAfterScroll(context, commandId, documentCount, null);
    }

    protected void updateStatusAfterScroll(ComputationContext context, String commandId, long documentCount,
            String errorMessage) {
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
        ((ComputationContextImpl) context).produceRecordImmediate(STATUS_STREAM, commandId,
                BulkCodecs.getStatusCodec().encode(delta));
    }

    /**
     * Produces a bucket as a record to appropriate bulk action stream.
     */
    protected void produceBucket(ComputationContext context, String commandId, int bucketSize, long bucketNumber) {
        List<String> ids = documentIds.subList(0, min(bucketSize, documentIds.size()));
        BulkBucket bucket = new BulkBucket(commandId, ids);
        String key = commandId + ":" + Long.toString(bucketNumber);
        Record record = Record.of(key, BulkCodecs.getBucketCodec().encode(bucket));
        if (produceImmediate) {
            ((ComputationContextImpl) context).produceRecordImmediate(actionStream, record);
        } else {
            context.produceRecord(actionStream, record);
        }
        ids.clear(); // this clear the documentIds part that has been sent
    }

}
