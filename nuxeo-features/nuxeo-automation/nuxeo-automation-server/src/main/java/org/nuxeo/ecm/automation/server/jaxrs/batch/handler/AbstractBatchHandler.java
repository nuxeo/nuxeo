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
 *     Luís Duarte
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch.handler;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerComponent.CLIENT_BATCH_ID_FLAG;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Abstract batch handler: common code.
 *
 * @since 10.1
 */
public abstract class AbstractBatchHandler implements BatchHandler {

    private static final Log log = LogFactory.getLog(AbstractBatchHandler.class);

    // property passed at initialization time
    public static final String PROP_TRANSIENT_STORE_NAME = "transientStore";

    /** Transient store key for the batch handler name. */
    public static final String BATCH_HANDLER_NAME = "handler";

    protected String name;

    protected String transientStoreName;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void initialize(String name, Map<String, String> properties) {
        this.name = name;
        initialize(properties);
    }

    protected void initialize(Map<String, String> properties) {
        if (isEmpty(properties.get(PROP_TRANSIENT_STORE_NAME))) {
            throw new NuxeoException("Missing configuration property: " + PROP_TRANSIENT_STORE_NAME);
        }
        transientStoreName = properties.get(PROP_TRANSIENT_STORE_NAME);
    }

    protected TransientStore getTransientStore() {
        return Framework.getService(TransientStoreService.class).getStore(transientStoreName);
    }

    /** Gets the batch parameters, or {@code null} if the batch is not found. */
    protected Map<String, Serializable> getBatchParameters(String batchId) {
        TransientStore transientStore = getTransientStore();
        Map<String, Serializable> parameters = transientStore.getParameters(batchId);
        if (parameters == null) {
            if (isEmpty(batchId) || !transientStore.exists(batchId)) {
                return null;
            }
            parameters = new HashMap<>();
        }
        // check that this batch is for this handler
        String handlerName = (String) parameters.remove(BATCH_HANDLER_NAME);
        if (handlerName != null && !handlerName.equals(getName())) {
            return null;
        }
        return parameters;
    }

    @Override
    public Batch newBatch(String batchId) {
        TransientStore transientStore = getTransientStore();
        if (isEmpty(batchId)) {
            batchId = generateBatchId();
        } else if (!Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(CLIENT_BATCH_ID_FLAG)) {
            throw new NuxeoException(String.format(
                    "Cannot initialize upload batch with a given id since configuration property %s is not set to true",
                    CLIENT_BATCH_ID_FLAG));
        }

        // That's the way of storing an empty entry
        log.debug("Initializing batch with id: " + batchId);
        transientStore.setCompleted(batchId, false);
        transientStore.putParameter(batchId, BATCH_HANDLER_NAME, getName());
        return new Batch(batchId);
    }

    protected String generateBatchId() {
        return "batchId-" + UUID.randomUUID();
    }

}
