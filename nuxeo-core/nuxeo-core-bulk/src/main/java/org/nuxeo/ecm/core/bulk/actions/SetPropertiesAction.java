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

import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.COUNTER_ACTION_NAME;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkCounter;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.2
 */
public class SetPropertiesAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "setProperties";

    public static final String BATCH_SIZE_OPT = "batchSize";

    public static final String BATCH_THRESHOLD_MS_OPT = "batchThresholdMs";

    public static final int DEFAULT_BATCH_SIZE = 10;

    public static final int DEFAULT_BATCH_THRESHOLD_MS = 200;

    @Override
    public Topology getTopology(Map<String, String> options) {
        int batchSize = getOptionAsInteger(options, BATCH_SIZE_OPT, DEFAULT_BATCH_SIZE);
        int batchThresholdMs = getOptionAsInteger(options, BATCH_THRESHOLD_MS_OPT, DEFAULT_BATCH_THRESHOLD_MS);
        Computation computation = createComputation(batchSize, batchThresholdMs);
        List<String> ios = Arrays.asList("i1:" + getActionName(), "o1:" + COUNTER_ACTION_NAME);
        return Topology.builder().addComputation(() -> computation, ios).build();
    }

    protected String getActionName() {
        return ACTION_NAME;
    }

    protected Computation createComputation(int batchSize, int batchThresholdMs) {
        return new SetPropertyComputation(getActionName(), batchSize, batchThresholdMs);
    }

    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static class SetPropertyComputation extends AbstractBulkComputation {

        public SetPropertyComputation(String name, int batchSize, int batchThresholdMs) {
            super(name, 1, 1, batchSize, batchThresholdMs);
        }

        @Override
        protected void processBatch(ComputationContext context) {
            if (!documentIds.isEmpty()) {
                TransactionHelper.runInTransaction(() -> {
                    try {
                        LoginContext loginContext = Framework.loginAsUser(currentCommand.getUsername());
                        String repository = currentCommand.getRepository();
                        // for setProperties, parameters are properties to set
                        Map<String, Serializable> properties = currentCommand.getParams();
                        try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
                            doProcessBatch(session, documentIds, properties);
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

        public void doProcessBatch(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            ids.forEach(id -> {
                DocumentModel doc = session.getDocument(new IdRef(id));
                properties.forEach(doc::setPropertyValue);
                session.saveDocument(doc);
            });
        }
    }

}
