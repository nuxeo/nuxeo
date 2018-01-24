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
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.util.Map;

import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;

/**
 * Batch Handler encapsulates functionality to handle Batch Upload behaviours.
 *
 * @since 10.1
 */
public interface BatchHandler {

    /**
     * Initializes this batch handler with the given name and configuration properties.
     *
     * @param name the batch handler's name
     * @param properties the configuration properties
     */
    void initialize(String name, Map<String, String> properties);

    /**
     * Gets the batch handler's name.
     *
     * @return the batch handler's name
     */
    String getName();

    /**
     * Initiates a new batch with an optional externally provided id.
     *
     * @param batchId the id to use for the batch, or {@code null} to generate it
     * @return a newly created batch
     */
    Batch newBatch(String batchId);

    /**
     * Attempts to fetch a batch with the given id.
     *
     * @param batchId the batch id to fetch
     * @return the batch with the given id, or {@code null} if not found
     */
    Batch getBatch(String batchId);

    /**
     * Callback for the batch handler to execute post-upload actions. This is only typically used in third-party batch
     * handlers.
     *
     * @param batchId the batch id
     * @param fileIdx the file index within the batch
     * @param fileInfo file information regarting the uploaded file
     * @return {@code true} if the action was success
     */
    default boolean completeUpload(String batchId, String fileIdx, BatchFileInfo fileInfo) {
        return true;
    }

}
