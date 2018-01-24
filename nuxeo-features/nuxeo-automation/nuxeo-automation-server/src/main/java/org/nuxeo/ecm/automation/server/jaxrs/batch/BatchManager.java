/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

/**
 * Service interface to collect inputs (Blobs) for an operation or operation chain.
 *
 * @since 5.4.2
 */
public interface BatchManager {

    /**
     * Returns the {@link TransientStore} backing the batches.
     *
     * @since 7.4
     */
    TransientStore getTransientStore();

    /**
     * Adds an inputStream as a blob to a batch. Will create a new {@link Batch} if needed.
     * <p>
     * Streams are persisted as temporary files.
     *
     * @deprecated since 10.1, use {@link #addBlob(String, String, Blob, String, String)} instead
     */
    @Deprecated
    void addStream(String batchId, String index, InputStream is, String name, String mime) throws IOException;

    /**
     * Adds a blob to a batch. Will create a new {@link Batch} if needed.
     *
     * @since 10.1
     */
    void addBlob(String batchId, String index, Blob blob, String name, String mime) throws IOException;

    /**
     * Adds an inputStream as a chunk to a batch. Will create a new {@link Batch} if needed.
     * <p>
     * Streams are persisted as temporary files.
     *
     * @since 7.4
     * @deprecated since 10.1, use {@link #addBlob(String, String, Blob, int, int, String, String, long)} instead
     */
    @Deprecated
    void addStream(String batchId, String index, InputStream is, int chunkCount, int chunkIndex, String name,
            String mime, long fileSize) throws IOException;

    /**
     * Adds a blob as a chunk to a batch. Will create a new {@link Batch} if needed.
     *
     * @since 10.1
     */
    void addBlob(String batchId, String index, Blob blob, int chunkCount, int chunkIndex, String name, String mime,
            long fileSize) throws IOException;

    /**
     * Returns true if there is a batch for the given {@code batchId}, false otherwise.
     *
     * @since 5.7.2
     */
    boolean hasBatch(String batchId);

    /**
     * Gets Blobs associated to a given batch. Returns null if batch does not exist.
     */
    List<Blob> getBlobs(String batchId);

    /**
     * Gets Blobs associated to a given batch. Returns null if batch does not exist. Waits for upload in progress if
     * needed.
     *
     * @since 5.7
     */
    List<Blob> getBlobs(String batchId, int timeoutS);

    Blob getBlob(String batchId, String fileIndex);

    Blob getBlob(String batchId, String fileIndex, int timeoutS);

    /**
     * @since 7.4
     */
    List<BatchFileEntry> getFileEntries(String batchId);

    /**
     * @since 7.4
     */
    BatchFileEntry getFileEntry(String batchId, String fileIndex);

    /**
     * Cleans up the temporary storage associated to the batch.
     */
    void clean(String batchId);

    /**
     * Initializes a batch by with an automatically generated id.
     *
     * @return the batch id
     * @since 7.4
     */
    String initBatch();

    /**
     * Initializes a batch with a given batchId and Context Name. If batchId is not provided, it will be automatically
     * generated.
     *
     * @return the batchId
     * @deprecated since 7.10, use {@link BatchManager#initBatch()} instead.
     */
    @Deprecated
    String initBatch(String batchId, String contextName);

    /**
     * Initiates a new batch with the given handler.
     *
     * @param handlerName the batch handler name
     * @return the newly created batch
     * @throws IllegalArgumentException it the batch handler does not exist
     * @since 10.1
     */
    Batch initBatch(String handlerName);

    /**
     * Executes the chain or operation on the {@code Blobs} from the given {@code batchId}.
     * <p>
     * This method does not clean the temporary storage associated to the {@code batchId}.
     *
     * @since 5.7
     */
    Object execute(String batchId, String chainOrOperationId, CoreSession session, Map<String, Object> contextParams,
            Map<String, Object> operationParams);

    /**
     * Executes the chain or operation on the {@code Blob} from the given {@code batchId} and {@code fileIndex}.
     * <p>
     * This method does not clean the temporary storage associated to the {@code batchId}.
     *
     * @since 5.7.2
     */
    Object execute(String batchId, String fileIndex, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams);

    /**
     * Executes the chain or operation on the {@code Blobs} from the given {@code batchId}.
     * <p>
     * This method cleans the temporary storage associated to the {@code batchId} after the execution.
     *
     * @since 5.7
     */
    Object executeAndClean(String batchId, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams);

    /**
     * Removes a file from a batch.
     *
     * @since 8.4
     */
    boolean removeFileEntry(String batchId, String filedIdx);

    /**
     * Fetches information about a batch.
     *
     * @param batchId the batch id
     * @return the batch, or {@code null} if it doesn't exist
     * @since 10.1
     */
    Batch getBatch(String batchId);

    /**
     * Returns the supported batch handler names.
     *
     * @return the supported batch handler names
     * @since 10.1
     */
    Set<String> getSupportedHandlers();

    /**
     * Gets a batch handler.
     *
     * @param handlerName the batch handler name
     * @return the batch handler, or {@code null} if it doesn't exist
     * @since 10.1
     */
    BatchHandler getHandler(String handlerName);

}
