package org.nuxeo.ecm.automation.server.jaxrs.batch.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import java.util.Map;
import java.util.UUID;

import static org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerComponent.CLIENT_BATCH_ID_FLAG;

public abstract class AbstractBatchHandler implements BatchHandler {
    private static final Log log = LogFactory.getLog(AbstractBatchHandler.class);

    private String name;

    protected AbstractBatchHandler(String name) {
        this();
        this.name = name;
    }

    protected AbstractBatchHandler() {

    }

    @Override public String getName() {
        return name;
    }

    @Override public void setName(String name) {
        this.name = name;
    }

    @Override public abstract Batch newBatch();

    @Override public abstract Batch getBatch(String batchId);

    @Override public abstract Batch newBatch(String providedId);

    @Override public void init(Map<String, String> configProperties) {

    }

    protected Batch initBatch() {
        return initBatch(null);
    }

    protected Batch initBatch(String batchId) {
        TransientStore transientStore = getTransientStore();
        if (StringUtils.isEmpty(batchId)) {
            batchId = generateBatchId();
        } else if (!Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(CLIENT_BATCH_ID_FLAG)) {
            throw new NuxeoException(String.format(
                    "Cannot initialize upload batch with a given id since configuration property %s is not set to true",
                    CLIENT_BATCH_ID_FLAG));
        }

        // That's the way of storing an empty entry
        log.debug("Initializing batch with id: " + batchId);
        transientStore.setCompleted(batchId, false);
        transientStore.putParameter(batchId, "provider", getName());
        return new Batch(batchId);

    }

    protected String generateBatchId() {
        return "batchId-" + UUID.randomUUID().toString();
    }

    protected abstract TransientStore getTransientStore();
}
