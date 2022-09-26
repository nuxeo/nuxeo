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
package org.nuxeo.ecm.core.bulk.action.computation;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.ABORTED;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.common.collect.Lists;

/**
 * Base class for bulk action computation.
 * <p>
 * Inputs:
 * <ul>
 * <li>i1: Reads {@link BulkBucket}</li>
 * </ul>
 * Outputs for the last computation of the processor
 * <ul>
 * <li>o1: Writes {@link BulkStatus} delta</li>
 * </ul>
 *
 * @since 10.2
 */
public abstract class AbstractBulkComputation extends AbstractComputation {

    private static final Logger log = LogManager.getLogger(AbstractBulkComputation.class);

    protected static final String SELECT_DOCUMENTS_IN = "SELECT * FROM Document, Relation WHERE ecm:uuid IN ('%s')";

    protected Map<String, BulkCommand> commands = new PassiveExpiringMap<>(60, TimeUnit.SECONDS);

    protected BulkCommand command;

    protected BulkStatus delta;

    public AbstractBulkComputation(String name) {
        this(name, 1);
    }

    public AbstractBulkComputation(String name, int nbOutputStreams) {
        super(name, 1, nbOutputStreams);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        BulkBucket bucket = BulkCodecs.getBucketCodec().decode(record.getData());
        command = getCommand(bucket.getCommandId());
        if (command != null) {
            delta = BulkStatus.deltaOf(command.getId());
            delta.setProcessingStartTime(Instant.now());
            delta.setProcessed(bucket.getIds().size());
            startBucket(record.getKey());
            try {
                for (List<String> batch : Lists.partition(bucket.getIds(), command.getBatchSize())) {
                    processBatchOfDocuments(batch);
                }
            } finally {
                delta.setProcessingEndTime(Instant.now());
            }
            endBucket(context, delta);
        } else {
            if (isAbortedCommand(bucket.getCommandId())) {
                log.debug("Skipping aborted command: {}", bucket.getCommandId());
            } else {
                log.warn("Skipping unknown command: {}, offset: {}.",bucket.getCommandId(), context.getLastOffset());
            }
        }
        context.askForCheckpoint();
    }

    protected boolean isAbortedCommand(String commandId) {
        BulkService bulkService = Framework.getService(BulkService.class);
        BulkStatus status = bulkService.getStatus(commandId);
        return ABORTED.equals(status.getState());
    }

    protected BulkCommand getCommand(String commandId) {
        // This is to remove expired/completed commands from the cache map
        commands.size();
        return commands.computeIfAbsent(commandId, id -> Framework.getService(BulkService.class).getCommand(id));
    }

    public BulkCommand getCurrentCommand() {
        return command;
    }

    protected void processBatchOfDocuments(List<String> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        TransactionHelper.runInTransaction(() -> {
            try {
                String username = command.getUsername();
                LoginContext loginContext = SYSTEM_USERNAME.equals(username) ? Framework.login()
                        : Framework.loginAsUser(username);
                String repository = command.getRepository();
                try (CloseableCoreSession session = repository == null ? null : CoreInstance.openCoreSession(repository)) {
                    compute(session, batch, command.getParams());
                } finally {
                    if (loginContext != null) {
                        loginContext.logout();
                    }
                }
            } catch (LoginException e) {
                throw new NuxeoException(e);
            }
        });
    }

    /**
     * Can be overridden to init stuff before processing the bucket
     */
    public void startBucket(String bucketKey) {
        // nothing to do
    }

    /**
     * Can be overridden to write to downstream computation or add results to status
     */
    public void endBucket(ComputationContext context, BulkStatus delta) {
        updateStatus(context, delta);
    }

    @Override
    public void processFailure(ComputationContext context, Throwable failure) {
        log.error(String.format("Action: %s fails on record: %s after retries.", metadata.name(),
                context.getLastOffset()), failure);
        // The delta will be send only if the policy is set with continueOnFailure = true
        delta.inError(metadata.name() + " fails on " + context.getLastOffset() + ": " + failure.getMessage());
        endBucket(context, delta);
    }

    public static void updateStatus(ComputationContext context, BulkStatus delta) {
        context.produceRecord(OUTPUT_1, delta.getId(), BulkCodecs.getStatusCodec().encode(delta));
    }

    protected abstract void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties);

    /**
     * Helper to load a list of documents. Documents without read access or that does not exists are not returned.
     */
    public DocumentModelList loadDocuments(CoreSession session, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return new DocumentModelListImpl(0);
        }
        try {
            DocumentModelList ret = session.query(String.format(SELECT_DOCUMENTS_IN, String.join("', '", documentIds)));
            if (log.isDebugEnabled() && ret.size() < documentIds.size()) {
                // some documents might have been deleted since scroller projection
                List<String> notFound = new ArrayList<>(documentIds);
                ret.forEach(doc -> notFound.remove(doc.getId()));
                log.debug("Some documents are not accessible: " + notFound);
            }
            return ret;
        } catch (DocumentNotFoundException | PropertyConversionException e) {
            // A corrupted document prevents to load the batch of docs
            log.warn("Fail to loadDocuments on bulk command: {}, because of: {}, retrying without batching",
                    command.getId(), e.getMessage());
            return loadDocumentsOneByOne(session, documentIds);
        }
    }

    public DocumentModelList loadDocumentsOneByOne(CoreSession session, List<String> documentIds) {
        DocumentModelList ret = new DocumentModelListImpl(documentIds.size());
        for (String documentId : documentIds) {
            try {
                ret.add(session.getDocument(new IdRef(documentId)));
            } catch (DocumentNotFoundException | PropertyConversionException e) {
                String message = "Skipping corrupted doc: " + documentId + ", because of: " + e.getMessage();
                log.error(message);
                log.debug(message, e);
            }
        }
        return ret;
    }
}
