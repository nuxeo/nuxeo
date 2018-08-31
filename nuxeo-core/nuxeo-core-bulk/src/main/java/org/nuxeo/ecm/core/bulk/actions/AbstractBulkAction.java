/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.bulk.actions;

import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkRecords.commandIdFrom;
import static org.nuxeo.ecm.core.bulk.BulkRecords.docIdsFrom;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.COMMAND;
import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.COUNTER_ACTION_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.bulk.BulkCounter;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.3
 */
public abstract class AbstractBulkAction implements StreamProcessorTopology {

    public static final String BATCH_SIZE_OPT = "batchSize";

    public static final String BATCH_THRESHOLD_MS_OPT = "batchThresholdMs";

    public static final int DEFAULT_BATCH_SIZE = 10;

    public static final int DEFAULT_BATCH_THRESHOLD_MS = 200;

    protected final String name;

    protected AbstractBulkAction(String name) {
        this.name = name;
    }

    @Override
    public Topology getTopology(Map<String, String> options) {
        int size = getOptionAsInteger(options, BATCH_SIZE_OPT, DEFAULT_BATCH_SIZE);
        int threshold = getOptionAsInteger(options, BATCH_THRESHOLD_MS_OPT, DEFAULT_BATCH_THRESHOLD_MS);
        return Topology.builder().addComputation(() -> createComputation(size, threshold), getStreamMapping()).build();
    }

    protected List<String> getStreamMapping() {
        return Arrays.asList("i1:" + getActionName(), "o1:" + COUNTER_ACTION_NAME);
    }

    protected String getActionName() {
        return name;
    }

    protected abstract Computation createComputation(int size, int timer);

    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    protected abstract static class AbstractBulkComputation extends AbstractComputation {

        protected final List<String> documentIds;

        protected final int size;

        protected final int timer;

        protected String currentCommandId;

        protected BulkCommand currentCommand;

        public AbstractBulkComputation(String name, int nbInputStreams, int nbOutputStreams, int size, int timer) {
            super(name, nbInputStreams, nbOutputStreams);
            this.documentIds = new ArrayList<>(size);
            this.timer = timer;
            this.size = size;
        }

        @Override
        public void init(ComputationContext context) {
            getLog().debug(
                    String.format("Starting computation: %s, size: %d, timer: %dms", metadata.name(), size, timer));
            context.setTimer("timer", System.currentTimeMillis() + timer);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            String commandId = commandIdFrom(record);
            if (currentCommandId == null) {
                // first time we need to process something
                loadCurrentBulkCommandContext(commandId);
            } else if (!currentCommandId.equals(commandId)) {
                // new bulk id computation - send remaining elements
                processBatch(context);
                documentIds.clear();
                loadCurrentBulkCommandContext(commandId);
            }
            // process record
            documentIds.addAll(docIdsFrom(record));
            if (documentIds.size() >= size) {
                processBatch(context);
            }
        }

        @Override
        public void processTimer(ComputationContext context, String key, long timestamp) {
            processBatch(context);
            context.setTimer("timer", System.currentTimeMillis() + timer);
        }

        @Override
        public void destroy() {
            getLog().debug(
                    String.format("Destroy computation: %s, pending entries: %d", metadata.name(), documentIds.size()));
        }

        protected Log getLog() {
            return LogFactory.getLog(getClass());
        }

        protected void loadCurrentBulkCommandContext(String commandId) {
            currentCommandId = commandId;
            KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
            currentCommand = BulkCodecs.getBulkCommandCodec().decode(kvStore.get(commandId + COMMAND));
        }

        protected void processBatch(ComputationContext context) {
            if (!documentIds.isEmpty()) {
                TransactionHelper.runInTransaction(() -> {
                    try {
                        LoginContext loginContext = Framework.loginAsUser(currentCommand.getUsername());
                        String repository = currentCommand.getRepository();
                        try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
                            compute(session, documentIds, currentCommand.getParams());
                        } finally {
                            loginContext.logout();
                        }
                    } catch (LoginException e) {
                        throw new NuxeoException(e);
                    }
                });
                BulkCounter counter = new BulkCounter(currentCommandId, documentIds.size());
                context.produceRecord("o1", currentCommandId, BulkCodecs.getBulkCounterCodec().encode(counter));
                documentIds.clear();
                context.askForCheckpoint();
            }
        }

        protected abstract void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties);
    }

}
