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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime Component implementing the {@link BatchManager} service
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class BatchManagerComponent extends DefaultComponent implements
        BatchManager {

    protected ConcurrentHashMap<String, Batch> batches = new ConcurrentHashMap<String, Batch>();

    protected static final String DEFAULT_CONTEXT = "None";

    static {
        ComplexTypeJSONDecoder.registerBlobDecoder(new JSONBatchBlobDecoder());
    }

    public String initBatch(String batchId, String contextName) {
        Batch batch = initBatchInternal(batchId, contextName);
        return batch.id;
    }

    protected Batch initBatchInternal(String batchId, String contextName) {
        if (batchId == null || batchId.isEmpty()) {
            batchId = "batchId-" + UUID.randomUUID().toString();
        }
        if (contextName == null || contextName.isEmpty()) {
            contextName = DEFAULT_CONTEXT;
        }

        Batch newBatch = new Batch(batchId);
        Batch existingBatch = batches.putIfAbsent(batchId, newBatch);

        if (existingBatch != null) {
            return existingBatch;
        } else {
            return newBatch;
        }
    }

    public void addStream(String batchId, String idx, InputStream is,
            String name, String mime) throws IOException {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            batch = initBatchInternal(batchId, null);
        }
        batch.addStream(idx, is, name, mime);
    }

    public List<Blob> getBlobs(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            return null;
        }
        return batch.getBlobs();
    }

    public Blob getBlob(String batchId, String fileId) {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            return null;
        }
        return batch.getBlob(fileId);
    }

    public void clean(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch != null) {
            batch.clear();
            batches.remove(batchId);
        }
    }
}
