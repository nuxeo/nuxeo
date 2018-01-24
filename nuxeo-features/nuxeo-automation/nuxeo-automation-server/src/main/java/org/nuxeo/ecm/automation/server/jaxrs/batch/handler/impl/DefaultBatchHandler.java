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
package org.nuxeo.ecm.automation.server.jaxrs.batch.handler.impl;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.AbstractBatchHandler;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;

/**
 * Default batch handler,
 *
 * @since 10.1
 */
public class DefaultBatchHandler extends AbstractBatchHandler {

    @Override
    public Batch getBatch(String batchId) {
        Map<String, Serializable> parameters = getBatchParameters(batchId);
        if (parameters == null) {
            return null;
        }

        // create the batch
        return new Batch(batchId, parameters, getName());
    }

    @Override
    public boolean completeUpload(String batchId, String fileIndex, BatchFileInfo fileInfo) {
        Batch batch = getBatch(batchId);
        BatchFileEntry fileEntry = batch.getFileEntry(fileIndex, true);
        return fileEntry.getFileSize() == fileInfo.getLength()
                && Objects.equals(fileEntry.getMimeType(), fileInfo.getMimeType())
                && Objects.equals(fileEntry.getBlob().getDigest(), fileInfo.getMd5());
    }

}
