package org.nuxeo.ecm.automation.server.jaxrs.batch.handler.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.AbstractBatchHandler;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultBatchHandlerImpl extends AbstractBatchHandler {

    protected static final Log log = LogFactory.getLog(DefaultBatchHandlerImpl.class);

    protected static final String BATCH_HANDLER_NAME = "default";

    private static class Config {
        public static final String TRANSIENT_STORE_NAME = "transientStore";
    }

    private TransientStore transientStore;
    private TransientStoreService transientStoreService;

    private String transientStoreName;

    public DefaultBatchHandlerImpl() {
        super(BATCH_HANDLER_NAME);
    }

    @Override public Batch newBatch() {
        return initBatch();
    }

    protected Batch initBatch() {
        return initBatch(null);
    }

    @Override public Batch newBatch(String providedId) {
        return initBatch(providedId);
    }

    @Override public Batch getBatch(String batchId) {
        TransientStore transientStore = getTransientStore();
        Map<String, Serializable> batchEntryParams = transientStore.getParameters(batchId);

        if (batchEntryParams == null) {
            if (!hasBatch(batchId)) {
                return null;
            }
            batchEntryParams = new HashMap<>();
        }

        String batchProvider = batchEntryParams.getOrDefault("provider", getName()).toString();
        batchEntryParams.remove("provider");

        if (getName().equalsIgnoreCase(batchProvider)) {
            return new Batch(getName(), batchId, batchEntryParams, this);
        }

        return null;
    }

    private boolean hasBatch(String batchId) {
        return !StringUtils.isEmpty(batchId) && transientStore.exists(batchId);
    }

    @Override public void init(Map<String, String> configProperties) {
        if (!containsRequired(configProperties)) {
            throw new NuxeoException();
        }

        transientStoreName = configProperties.get(Config.TRANSIENT_STORE_NAME);

        super.init(configProperties);
    }

    @Override
    public boolean completeUpload(String batchId, String fileIndex, BatchFileInfo fileInfo) {
        Batch batch = getBatch(batchId);
        BatchFileEntry fileEntry = batch.getFileEntry(fileIndex, true);
        return fileEntry.getFileSize() == fileInfo.getFileSize() &&
                Objects.equals(fileEntry.getMimeType(), fileInfo.getMimeType()) &&
                Objects.equals(fileEntry.getBlob().getDigest(), fileInfo.getMd5())
                ;
    }

    private boolean containsRequired(Map<String, String> configProperties) {
        if (!configProperties.containsKey(Config.TRANSIENT_STORE_NAME)) {
            return false;
        }

        return true;
    }

    protected TransientStore getTransientStore() {
        if (transientStoreService == null) {
            transientStoreService = Framework.getService(TransientStoreService.class);
            transientStore = transientStoreService.getStore(transientStoreName);
        }

        return transientStore;
    }


}
