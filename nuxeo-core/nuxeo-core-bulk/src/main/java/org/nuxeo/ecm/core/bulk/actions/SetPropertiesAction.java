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
import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.COUNTER_STREAM_NAME;

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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.bulk.BulkCounter;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.2
 */
// TODO refactor this computation when the batch policy is introduced
public class SetPropertiesAction implements StreamProcessorTopology {

    private static final Log log = LogFactory.getLog(SetPropertiesAction.class);

    public static final String COMPUTATION_NAME = "SetProperties";

    public static final String STREAM_NAME = "setProperties";

    public static final String BATCH_SIZE_OPT = "batchSize";

    public static final String BATCH_THRESHOLD_MS_OPT = "batchThresholdMs";

    public static final int DEFAULT_BATCH_SIZE = 10;

    public static final int DEFAULT_BATCH_THRESHOLD_MS = 200;

    @Override
    public Topology getTopology(Map<String, String> options) {
        int batchSize = getOptionAsInteger(options, BATCH_SIZE_OPT, DEFAULT_BATCH_SIZE);
        int batchThresholdMs = getOptionAsInteger(options, BATCH_THRESHOLD_MS_OPT, DEFAULT_BATCH_THRESHOLD_MS);
        return Topology.builder()
                       .addComputation(() -> new SetPropertyComputation(COMPUTATION_NAME, batchSize, batchThresholdMs),
                               Arrays.asList("i1:" + STREAM_NAME, "o1:" + COUNTER_STREAM_NAME))
                       .build();
    }

    public static class SetPropertyComputation extends AbstractComputation {

        protected final int batchSize;

        protected final int batchThresholdMs;

        protected final List<String> documentIds;

        protected String currentCommandId;

        protected BulkCommand currentCommand;

        public SetPropertyComputation(String name, int batchSize, int batchThresholdMs) {
            super(name, 1, 1);
            this.batchSize = batchSize;
            this.batchThresholdMs = batchThresholdMs;
            documentIds = new ArrayList<>(batchSize);
        }

        @Override
        public void init(ComputationContext context) {
            log.debug(String.format("Starting computation: %s reading on: %s, batch size: %d, threshold: %dms",
                    COMPUTATION_NAME, STREAM_NAME, batchSize, batchThresholdMs));
            context.setTimer("batch", System.currentTimeMillis() + batchThresholdMs);
        }

        @Override
        public void processTimer(ComputationContext context, String key, long timestamp) {
            processBatch(context);
            context.setTimer("batch", System.currentTimeMillis() + batchThresholdMs);
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
            if (documentIds.size() >= batchSize) {
                processBatch(context);
            }
        }

        protected void loadCurrentBulkCommandContext(String commandId) {
            currentCommandId = commandId;
            KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
            currentCommand = BulkCodecs.getBulkCommandCodec().decode(kvStore.get(commandId + COMMAND));
        }

        @Override
        public void destroy() {
            log.debug(String.format("Destroy computation: %s, pending entries: %d", COMPUTATION_NAME,
                    documentIds.size()));
        }

        protected void processBatch(ComputationContext context) {
            if (!documentIds.isEmpty()) {
                TransactionHelper.runInTransaction(() -> {
                    // for setProperties, parameters are properties to set
                    Map<String, String> properties = currentCommand.getParams();
                    LoginContext loginContext;
                    try {
                        loginContext = Framework.loginAsUser(currentCommand.getUsername());

                        try (CloseableCoreSession session = CoreInstance.openCoreSession(
                                currentCommand.getRepository())) {
                            for (String docId : documentIds) {
                                DocumentModel doc = session.getDocument(new IdRef(docId));
                                properties.forEach(doc::setPropertyValue);
                                session.saveDocument(doc);
                            }
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
    }

    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }
}
