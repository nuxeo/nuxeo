/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Service interface to collect inputs (Blobs) for an operation or operation
 * chain
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author Antoine Taillefer
 * @since 5.4.2
 */
public interface BatchManager {

    /**
     * Add an inputStream in a batch Will create a new {@link Batch} if needed
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
    void addStream(String batchId, String idx, InputStream is, String name,
            String mime) throws IOException;

    /**
     * Returns true if there is a batch for the given {@code batchId}, false
     * otherwise.
     *
     * @since 5.7.2
     */
    boolean hasBatch(String batchId);

    /**
     * Get Blobs associated to a given batch. Returns null if batch does not
     * exist
     *
     * @param batchId
     * @return
     */
    List<Blob> getBlobs(String batchId);

    /**
     * Get Blobs associated to a given batch. Returns null if batch does not
     * exist
     *
     * Wait for upload in progress if needed
     *
     * @since 5.7
     *
     * @param batchId
     * @return
     */
    List<Blob> getBlobs(String batchId, int timeoutS);

    Blob getBlob(String batchId, String fileId);

    Blob getBlob(String batchId, String fileId, int timeoutS);

    /**
     * Cleanup the temporary storage associated to the batch
     *
     * @param batchId
     */
    void clean(String batchId);

    /**
     * Initialize a batch with a given batchId and Context Name If batchId is
     * not provided, it will be automatically generated
     *
     * @param batchId
     * @param contextName
     * @return the batchId
     */
    String initBatch(String batchId, String contextName);

    /**
     * Executes the chain or operation on the {@code Blobs} from the given
     * {@code batchId}.
     * <p>
     * This method does not clean the temporary storage associated to the
     * {@code batchId}.
     *
     * @since 5.7
     */
    Object execute(String batchId, String chainOrOperationId,
            CoreSession session, Map<String, Object> contextParams,
            Map<String, Object> operationParams) throws ClientException;

    /**
     * Executes the chain or operation on the {@code Blob} from the given
     * {@code batchId} and {@code fileIdx}.
     * <p>
     * This method does not clean the temporary storage associated to the
     * {@code batchId}.
     *
     * @since 5.7.2
     */
    Object execute(String batchId, String fileIdx, String chainOrOperationId,
            CoreSession session, Map<String, Object> contextParams,
            Map<String, Object> operationParams) throws ClientException;

    /**
     * Executes the chain or operation on the {@code Blobs} from the given
     * {@code batchId}.
     * <p>
     * This method clean the temporary storage associated to the {@code batchId}
     * after the execution.
     *
     * @since 5.7
     */
    Object executeAndClean(String batchId, String chainOrOperationId,
            CoreSession session, Map<String, Object> contextParams,
            Map<String, Object> operationParams) throws ClientException;

}
