/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.RestBinding;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchHandlerDescriptor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime Component implementing the {@link BatchManager} service with the {@link TransientStore}.
 *
 * @since 5.4.2
 */
public class BatchManagerComponent extends DefaultComponent implements BatchManager {

    protected static final String TRANSIENT_STORE_NAME = "BatchManagerCache";

    public static final String CLIENT_BATCH_ID_FLAG = "allowClientGeneratedBatchId";

    /**
     * The default batch handler name.
     *
     * @since 10.1
     */
    public static final String DEFAULT_BATCH_HANDLER = "default";

    /** @since 10.1 */
    public static final String XP_BATCH_HANDLER = "handlers";

    protected Map<String, BatchHandler> handlers = new HashMap<>();

    protected final AtomicInteger uploadInProgress = new AtomicInteger(0);

    static {
        ComplexTypeJSONDecoder.registerBlobDecoder(new JSONBatchBlobDecoder());
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<BatchHandlerDescriptor> descriptors = getDescriptors(XP_BATCH_HANDLER);
        descriptors.forEach(d -> {
            try {
                BatchHandler handler = d.klass.newInstance();
                handler.initialize(d.name, d.properties);
                handlers.put(d.name, handler);
            } catch (ReflectiveOperationException e) {
                getLog().error(e,e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        handlers.clear();
    }

    @Override
    public TransientStore getTransientStore() {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        return tss.getStore(TRANSIENT_STORE_NAME);
    }

    @Override
    public Set<String> getSupportedHandlers() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    @Override
    public BatchHandler getHandler(String handlerName) {
        return handlers.get(handlerName);
    }

    @Override
    public String initBatch() {
        Batch batch = initBatchInternal(null);
        return batch.getKey();
    }

    @Override
    @Deprecated
    public String initBatch(String batchId, String contextName) {
        Batch batch = initBatchInternal(batchId);
        return batch.getKey();
    }

    protected Batch initBatchInternal(String batchId) {
        BatchHandler batchHandler = handlers.get(DEFAULT_BATCH_HANDLER);
        return batchHandler.newBatch(batchId);
    }

    @Override
    public Batch initBatch(String handlerName) {
        if (isEmpty(handlerName)) {
            handlerName = DEFAULT_BATCH_HANDLER;
        }
        BatchHandler batchHandler = handlers.get(handlerName);
        if (batchHandler == null) {
            throw new IllegalArgumentException("Batch handler does not exist: " + handlerName);
        }
        return batchHandler.newBatch(null);
    }

    @Override
    public Batch getBatch(String batchId) {
        return handlers.values()
                       .stream()
                       .map(batchHandler -> batchHandler.getBatch(batchId))
                       .filter(Objects::nonNull)
                       .findFirst()
                       .orElse(null);
    }

    @Override
    public void addStream(String batchId, String index, InputStream is, String name, String mime) throws IOException {
        Blob blob = Blobs.createBlob(is);
        addBlob(batchId, index, blob, name, mime);
    }

    @Override
    public void addBlob(String batchId, String index, Blob blob, String name, String mime) throws IOException {
        uploadInProgress.incrementAndGet();
        try {
            Batch batch = getBatch(batchId);
            if (batch == null) {
                batch = initBatchInternal(batchId);
            }
            batch.addFile(index, blob, name, mime);
            getLog().debug(String.format("Added file %s [%s] to batch %s", index, name, batch.getKey()));
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    @Override
    public void addStream(String batchId, String index, InputStream is, int chunkCount, int chunkIndex, String name,
            String mime, long fileSize) throws IOException {
        Blob blob = Blobs.createBlob(is);
        addBlob(batchId, index, blob, chunkCount, chunkIndex, name, mime, fileSize);
    }

    @Override
    public void addBlob(String batchId, String index, Blob blob, int chunkCount, int chunkIndex, String name,
            String mime, long fileSize) throws IOException {
        uploadInProgress.incrementAndGet();
        try {
            Batch batch = getBatch(batchId);
            if (batch == null) {
                batch = initBatchInternal(batchId);
            }
            batch.addChunk(index, blob, chunkCount, chunkIndex, name, mime, fileSize);
            getLog().debug(String.format("Added chunk %s to file %s [%s] in batch %s", chunkIndex, index, name,
                    batch.getKey()));
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    @Override
    public boolean hasBatch(String batchId) {
        return handlers.values().stream().anyMatch(batchHandler -> batchHandler.getBatch(batchId) != null);
    }

    @Override
    public List<Blob> getBlobs(String batchId) {
        return getBlobs(batchId, 0);
    }

    @Override
    public List<Blob> getBlobs(String batchId, int timeoutS) {
        if (uploadInProgress.get() > 0 && timeoutS > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (uploadInProgress.get() == 0) {
                    break;
                }
            }
        }
        Batch batch = getBatch(batchId);
        if (batch == null) {
            getLog().error("Unable to find batch with id " + batchId);
            return Collections.emptyList();
        }
        return batch.getBlobs();
    }

    @Override
    public Blob getBlob(String batchId, String fileIndex) {
        return getBlob(batchId, fileIndex, 0);
    }

    @Override
    public Blob getBlob(String batchId, String fileIndex, int timeoutS) {
        Blob blob = getBatchBlob(batchId, fileIndex);
        if (blob == null && timeoutS > 0 && uploadInProgress.get() > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                blob = getBatchBlob(batchId, fileIndex);
                if (blob != null) {
                    break;
                }
            }
        }
        if (!hasBatch(batchId)) {
            getLog().error("Unable to find batch with id " + batchId);
            return null;
        }
        return blob;
    }

    protected Blob getBatchBlob(String batchId, String fileIndex) {
        Blob blob = null;
        Batch batch = getBatch(batchId);
        if (batch != null) {
            blob = batch.getBlob(fileIndex);
        }
        return blob;
    }

    @Override
    public List<BatchFileEntry> getFileEntries(String batchId) {
        Batch batch = getBatch(batchId);
        if (batch == null) {
            return null;
        }
        return batch.getFileEntries();
    }

    @Override
    public BatchFileEntry getFileEntry(String batchId, String fileIndex) {
        Batch batch = getBatch(batchId);
        if (batch == null) {
            return null;
        }
        return batch.getFileEntry(fileIndex);
    }

    @Override
    public void clean(String batchId) {
        Batch batch = getBatch(batchId);
        if (batch != null) {
            batch.clean();
        }
    }

    @Override
    public Object execute(String batchId, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        List<Blob> blobs = getBlobs(batchId, getUploadWaitTimeout());
        if (blobs == null) {
            String message = String.format("Unable to find batch associated with id '%s'", batchId);
            getLog().error(message);
            throw new NuxeoException(message);
        }
        return execute(new BlobList(blobs), chainOrOperationId, session, contextParams, operationParams);
    }

    @Override
    public Object execute(String batchId, String fileIndex, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        Blob blob = getBlob(batchId, fileIndex, getUploadWaitTimeout());
        if (blob == null) {
            String message = String.format(
                    "Unable to find batch associated with id '%s' or file associated with index '%s'", batchId,
                    fileIndex);
            getLog().error(message);
            throw new NuxeoException(message);
        }
        return execute(blob, chainOrOperationId, session, contextParams, operationParams);
    }

    protected Object execute(Object blobInput, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        if (contextParams == null) {
            contextParams = new HashMap<>();
        }
        if (operationParams == null) {
            operationParams = new HashMap<>();
        }

        try (OperationContext ctx = new OperationContext(session)) {

            AutomationServer server = Framework.getService(AutomationServer.class);
            RestBinding binding = server.getOperationBinding(chainOrOperationId);

            if (binding != null && binding.isAdministrator) {
                Principal principal = ctx.getPrincipal();
                if (!(principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAdministrator())) {
                    String message = "Not allowed. You must be administrator to use this operation";
                    getLog().error(message);
                    throw new WebSecurityException(message);
                }
            }

            ctx.setInput(blobInput);
            ctx.putAll(contextParams);

            AutomationService as = Framework.getService(AutomationService.class);
            // Drag and Drop action category is accessible from the chain sub context as chain parameters
            return as.run(ctx, chainOrOperationId, operationParams);
        } catch (OperationException e) {
            getLog().error("Error while executing automation batch ", e);
            throw new NuxeoException(e);
        }
    }

    protected int getUploadWaitTimeout() {
        String t = Framework.getProperty("org.nuxeo.batch.upload.wait.timeout", "5");
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            getLog().error("Wrong number format for upload wait timeout property", e);
            return 5;
        }
    }

    @Override
    public Object executeAndClean(String batchId, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        try {
            return execute(batchId, chainOrOperationId, session, contextParams, operationParams);
        } finally {
            clean(batchId);
        }
    }

    @Override
    public boolean removeFileEntry(String batchId, String filedIdx) {
        Batch batch = getBatch(batchId);
        return batch != null && batch.removeFileEntry(filedIdx);
    }
}
