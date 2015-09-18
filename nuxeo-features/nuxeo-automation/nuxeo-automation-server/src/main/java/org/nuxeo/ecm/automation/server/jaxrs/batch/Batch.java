/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.transientstore.AbstractStorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;

/**
 * Batch Object to encapsulate all data related to a batch, especially the temporary files used for Blobs.
 * <p>
 * Since 7.4 a batch is backed by the {@link TransientStore}.
 *
 * @since 5.4.2
 */
public class Batch extends AbstractStorageEntry {

    private static final long serialVersionUID = 1L;

    protected final AtomicInteger uploadInProgress = new AtomicInteger(0);

    public Batch(String id) {
        super(id);
    }

    /**
     * Returns the uploaded blobs in the order the user chose to upload them.
     */
    @Override
    public List<Blob> getBlobs() {
        return getBlobs(0);

    }

    /**
     * @since 5.7
     */
    public List<Blob> getBlobs(int timeoutS) {
        List<Blob> blobs = new ArrayList<Blob>();
        Map<String, Serializable> params = getParameters();
        if (params == null) {
            return blobs;
        }

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

        List<String> sortedIdx = new ArrayList<String>(params.keySet());
        Collections.sort(sortedIdx);
        for (String idx : sortedIdx) {
            Blob blob = retrieveBlob(idx);
            if (blob != null) {
                blobs.add(blob);
            }
        }
        return blobs;
    }

    public Blob getBlob(String idx) {
        return getBlob(idx, 0);
    }

    /**
     * @since 5.7
     */
    public Blob getBlob(String idx, int timeoutS) {
        String fileIdx = "file_" + idx;
        Blob blob = retrieveBlob(fileIdx);
        if (blob == null && timeoutS > 0 && uploadInProgress.get() > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                blob = retrieveBlob(fileIdx);
                if (blob != null) {
                    break;
                }
            }
        }
        return blob;
    }

    protected Blob retrieveBlob(String idx) {
        Blob blob = null;
        String fileEntryId = (String) get(idx);
        if (fileEntryId != null) {
            BatchManager bm = Framework.getService(BatchManager.class);
            BatchFileEntry fileEntry = (BatchFileEntry) bm.getTransientStore().get(fileEntryId);
            if (fileEntry != null) {
                blob = fileEntry.getBlob();
            }
        }
        return blob;
    }

    public void addStream(String idx, InputStream is, String name, String mime) throws IOException {
        uploadInProgress.incrementAndGet();
        try {
            String mimeType = mime;
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            Blob blob = Blobs.createBlob(is, mime);
            blob.setFilename(name);
            addFile(idx, blob);
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    protected BatchFileEntry addFile(String idx, Blob blob) {
        String fileEntryId = getId() + "_" + idx;
        BatchFileEntry fileEntry = new BatchFileEntry(fileEntryId, blob);

        BatchManager bm = Framework.getService(BatchManager.class);
        bm.getTransientStore().put(fileEntry);
        put("file_" + idx, fileEntryId);

        return fileEntry;
    }

    /**
     * @since 7.4
     */
    public void addChunkStream(String idx, InputStream is, int chunkCount, int chunkIdx, String name, String mime,
            long fileSize) throws IOException {
        uploadInProgress.incrementAndGet();
        try {
            addChunk(idx, Blobs.createBlob(is), chunkCount, chunkIdx, name, mime, fileSize);
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    protected BatchFileEntry addChunk(String idx, Blob blob, int chunkCount, int chunkIdx, String name, String mime,
            long fileSize) {

        BatchFileEntry fileEntry = null;
        String fileEntryId = (String) get("file_" + idx);
        if (fileEntryId != null) {
            BatchManager bm = Framework.getService(BatchManager.class);
            fileEntry = (BatchFileEntry) bm.getTransientStore().get(fileEntryId);
        }
        if (fileEntry == null) {
            if (fileEntryId == null) {
                fileEntryId = getId() + "_" + idx;
            }
            fileEntry = new BatchFileEntry(fileEntryId, chunkCount, name, mime, fileSize);
            put("file_" + idx, fileEntryId);
        }
        fileEntry.addChunk(chunkIdx, blob);

        BatchManager bm = Framework.getService(BatchManager.class);
        bm.getTransientStore().put(fileEntry);

        return fileEntry;
    }

    /**
     * @since 7.4
     */
    public void clean() {
        // Remove batch and all related storage entries from transient store, GC will clean up the files
        BatchManager bm = Framework.getService(BatchManager.class);
        TransientStore ts = bm.getTransientStore();
        Map<String, Serializable> params = getParameters();
        if (params != null) {
            for (Serializable v : params.values()) {
                String fileEntryId = (String) v;
                // Check for chunk entries to remove
                BatchFileEntry fileEntry = (BatchFileEntry) ts.get(fileEntryId);
                if (fileEntry.isChunked()) {
                    for (String chunkEntryId : fileEntry.getChunkEntryIds()) {
                        // Remove chunk entry
                        ts.remove(chunkEntryId);
                    }
                }
                // Remove file entry
                ts.remove(fileEntryId);
            }
        }
        // Remove batch entry
        ts.remove(getId());
    }

    @Override
    public void beforeRemove() {
        // Nothing to do here
    }

}
