package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Service interface to collect parameters (Blobs) for an operation or operation
 * chain
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public interface BatchManager {

    /**
     * Add an inputStream in a batch Will create a new {@link Batch} if
     * needed
     *
     * Streams are persisted as temporary files
     *
     * @param batchId
     * @param idx
     * @param is
     * @param name
     * @param mime
     * @throws IOException
     */
    public void addStream(String batchId, String idx, InputStream is,
            String name, String mime) throws IOException;

    /**
     * Get Blobs associated to a given batch. Returns null if batch does not
     * exist
     *
     * @param batchId
     * @return
     */
    public List<Blob> getBlobs(String batchId);

    /**
     * Cleanup the temporary storage associated to the batch
     *
     * @param batchId
     */
    public void clean(String batchId);

    /**
     * Initialize a batch with a given batchId and Context Name
     * If batchId is not provided, it will be automatically generated
     *
     * @param batchId
     * @param contextName
     * @return the batchId
     */
    public String initBatch(String batchId,String contextName);

}