package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class BatchManagerComponent extends DefaultComponent implements BatchManager {

    protected Map<String, Batch> batches = new ConcurrentHashMap<String, Batch>();

    protected final static String DEFAULT_CONTEXT = "None" ;

    public String initBatch(String batchId,String contextName) {

        if (batchId==null || batchId.isEmpty()) {
            batchId = "batchId-" + UUID.randomUUID().toString();
        }
        if (contextName==null || contextName.isEmpty()) {
            contextName=DEFAULT_CONTEXT;
        }

        if (batches.keySet().contains(batchId)) {
            throw new UnsupportedOperationException("Batch Id " + batchId + "already exists");
        }
        batches.put(batchId, new Batch(batchId));

        return batchId;
    }

    public void addStream(String batchId, String idx, InputStream is, String name, String mime ) throws IOException {
        if (!batches.keySet().contains(batchId)) {
            batchId = initBatch(batchId, null);
        }
        Batch batch = batches.get(batchId);
        batch.addStream(idx, is,name,mime);
    }

    public List<Blob> getBlobs(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch==null) {
            return null;
        }
        return batch.getBlobs();
    }

    public void clean(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch!=null) {
            batch.clear();
            batches.remove(batchId);
        }
    }

}
