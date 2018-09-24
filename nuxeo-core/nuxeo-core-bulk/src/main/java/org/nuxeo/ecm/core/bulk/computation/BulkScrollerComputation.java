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

import static java.lang.Integer.max;
import static java.lang.Math.min;
import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.RUNNING;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.SCHEDULED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.SCROLLING_RUNNING;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkProcessor;
import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Reads command and execute NXQL to materializes a document set for a command. It outputs the bucket of document ids in
 * the action input streams.
 *
 * @since 10.2
 */
public class BulkScrollerComputation extends AbstractComputation {

    private static final Log log = LogFactory.getLog(BulkScrollerComputation.class);

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
    public BulkScrollerComputation(String name, int nbOutputStreams, int scrollBatchSize,
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
            BulkCommand command = BulkCodecs.getCommandCodec().decode(record.getData());
            BulkStatus currentStatus = BulkCodecs.getStatusCodec().decode(kvStore.get(commandId + STATUS));
            if (!SCHEDULED.equals(currentStatus.getState())) {
                log.error("Discard record: " + record + " because it's already building");
                context.askForCheckpoint();
                return;
            }
            currentStatus.setState(SCROLLING_RUNNING);
            context.produceRecord(BulkProcessor.KVWRITER_ACTION_NAME, commandId,
                    BulkCodecs.getStatusCodec().encode(currentStatus));

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
                            // key are prefixed with commandId:, suffix are:
                            // bucketSize / 2 * bucketSize / ... / total document count
                            bucketNumber++;
                            produceBucket(context, command.getAction(), commandId, bucketNumber);
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
                    currentStatus.setState(RUNNING);
                    currentStatus.setCount(documentCount);
                    context.produceRecord(BulkProcessor.KVWRITER_ACTION_NAME, commandId,
                            BulkCodecs.getStatusCodec().encode(currentStatus));

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
    protected void produceBucket(ComputationContext context, String action, String commandId, long bucketNumber) {
        List<String> ids = documentIds.subList(0, min(bucketSize, documentIds.size()));
        BulkBucket bucket = new BulkBucket(commandId, ids);
        context.produceRecord(action, commandId + ":" + Long.toString(bucketNumber),
                BulkCodecs.getBucketCodec().encode(bucket));
        context.askForCheckpoint();
        ids.clear(); // this clear the documentIds part that has been sent
    }

}
