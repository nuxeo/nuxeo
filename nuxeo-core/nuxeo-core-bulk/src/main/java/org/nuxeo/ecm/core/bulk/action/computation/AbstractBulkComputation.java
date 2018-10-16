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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
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

    protected BulkCommand command;

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
        if (command == null) {
            // this requires a manual intervention, the kv store might have been lost
            getLog().error(String.format("Stopping processing, unknown command: %s, offset: %s, record: %s.",
                    bucket.getCommandId(), context.getLastOffset(), record));
            context.askForTermination();
            return;
        }
        for (List<String> batch : Lists.partition(bucket.getIds(), command.getBatchSize())) {
            processBatchOfDocuments(batch);
        }
        endBucket(context, bucket.getIds().size());
        context.askForCheckpoint();
    }

    protected BulkCommand getCommand(String commandId) {
        if (command == null || !command.getId().equals(commandId)) {
            return Framework.getService(BulkService.class).getCommand(commandId);
        }
        return command;
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
                LoginContext loginContext = Framework.loginAsUser(command.getUsername());
                String repository = command.getRepository();
                try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
                    compute(session, batch, command.getParams());
                } finally {
                    loginContext.logout();
                }
            } catch (LoginException e) {
                throw new NuxeoException(e);
            }
        });
    }

    /**
     * Can be overridden to write to downstream computation.
     */
    public void endBucket(ComputationContext context, int bucketSize) {
        updateStatusProcessed(context, command.getId(), bucketSize);
    }

    /**
     * This should be called by the last computation of the action's topology to inform on the progress of the command.
     */
    public static void updateStatusProcessed(ComputationContext context, String commandId, long processed) {
        updateStatusProcessed(context, commandId, processed, null);
    }

    /**
     * This should be called by the last computation of the action's topology to inform on the progress of the command.
     */
    public static void updateStatusProcessed(ComputationContext context, String commandId, long processed,
            Map<String, Serializable> result) {
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        delta.setProcessed(processed);
        if (result != null) {
            delta.setResult(result);
        }
        context.produceRecord(OUTPUT_1, commandId, BulkCodecs.getStatusCodec().encode(delta));
    }

    protected Log getLog() {
        return LogFactory.getLog(getClass());
    }

    protected abstract void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties);
}
