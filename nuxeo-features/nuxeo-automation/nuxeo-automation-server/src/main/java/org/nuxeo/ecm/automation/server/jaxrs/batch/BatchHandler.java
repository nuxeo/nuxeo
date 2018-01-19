package org.nuxeo.ecm.automation.server.jaxrs.batch;

import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;

import java.util.Map;

public interface BatchHandler {
    String getName();
    void setName(String newName);
    Batch newBatch();
    Batch newBatch(String providedId);
    Batch getBatch(String batchId);
    void init(Map<String, String> configProperties);
    boolean completeUpload(String batchInfo, String fileIndex, BatchFileInfo fileInfo);
}
