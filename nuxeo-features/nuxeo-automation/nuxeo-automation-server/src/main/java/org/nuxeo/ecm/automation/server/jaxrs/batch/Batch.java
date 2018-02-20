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
 *     Gabriel Barata <gbarata@nuxeo.com>
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;

/**
 * Batch Object to encapsulate all data related to a batch, especially the temporary files used for Blobs.
 * <p>
 * Since 7.4 a batch is backed by the {@link TransientStore}.
 *
 * @since 5.4.2
 */
public class Batch {

    protected static final Log log = LogFactory.getLog(Batch.class);

    public static final String CHUNKED_PARAM_NAME = "chunked";

    protected String key;

    protected Map<String, Serializable> fileEntries;

    public Batch(String key) {
        this(key, new HashMap<>());
    }

    public Batch(String key, Map<String, Serializable> fileEntries) {
        this.key = key;
        this.fileEntries = fileEntries;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns the uploaded blobs in the order the user chose to upload them.
     */
    public List<Blob> getBlobs() {
        List<Blob> blobs = new ArrayList<Blob>();
        List<String> sortedFileIndexes = getOrderedFileIndexes();
        log.debug(String.format("Retrieving blobs for batch %s: %s", key, sortedFileIndexes));
        for (String index : sortedFileIndexes) {
            Blob blob = retrieveBlob(index);
            if (blob != null) {
                blobs.add(blob);
            }
        }
        return blobs;
    }

    public Blob getBlob(String index) {
        log.debug(String.format("Retrieving blob %s for batch %s", index, key));
        return retrieveBlob(index);
    }

    protected List<String> getOrderedFileIndexes() {
        List<String> sortedFileIndexes = new ArrayList<String>(fileEntries.keySet());
        Collections.sort(sortedFileIndexes, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
            }
        });
        return sortedFileIndexes;
    }

    protected Blob retrieveBlob(String index) {
        Blob blob = null;
        BatchFileEntry fileEntry = getFileEntry(index);
        if (fileEntry != null) {
            blob = fileEntry.getBlob();
        }
        return blob;
    }

    public List<BatchFileEntry> getFileEntries() {
        List<BatchFileEntry> batchFileEntries = new ArrayList<BatchFileEntry>();
        List<String> sortedFileIndexes = getOrderedFileIndexes();
        for (String index : sortedFileIndexes) {
            BatchFileEntry fileEntry = getFileEntry(index);
            if (fileEntry != null) {
                batchFileEntries.add(fileEntry);
            }
        }
        return batchFileEntries;
    }

    public BatchFileEntry getFileEntry(String index) {
        return getFileEntry(index, true);
    }

    public BatchFileEntry getFileEntry(String index, boolean fetchBlobs) {
        BatchManager bm = Framework.getService(BatchManager.class);
        String fileEntryKey = (String) fileEntries.get(index);
        if (fileEntryKey == null) {
            return null;
        }
        TransientStore ts = bm.getTransientStore();
        Map<String, Serializable> fileEntryParams = ts.getParameters(fileEntryKey);
        if (fileEntryParams == null) {
            return null;
        }
        boolean chunked = Boolean.parseBoolean((String) fileEntryParams.get(CHUNKED_PARAM_NAME));
        if (chunked) {
            return new BatchFileEntry(fileEntryKey, fileEntryParams);
        } else {
            Blob blob = null;
            if (fetchBlobs) {
                List<Blob> fileEntryBlobs = ts.getBlobs(fileEntryKey);
                if (fileEntryBlobs == null) {
                    return null;
                }
                if (!fileEntryBlobs.isEmpty()) {
                    blob = fileEntryBlobs.get(0);
                }
            }
            return new BatchFileEntry(fileEntryKey, blob);
        }
    }

    /**
     * Adds a file with the given {@code index} to the batch.
     *
     * @return The key of the new {@link BatchFileEntry}.
     * @deprecated since 10.1, use the {@link Blob}-based signature instead
     */
    @Deprecated
    public String addFile(String index, InputStream is, String name, String mime) throws IOException {
        Blob blob = Blobs.createBlob(is);
        return addFile(index, blob, name, mime);
    }

    /**
     * Adds a file with the given {@code index} to the batch.
     *
     * @return The key of the new {@link BatchFileEntry}.
     * @since 10.1
     */
    public String addFile(String index, Blob blob, String name, String mime) throws IOException {
        blob.setFilename(name);
        blob.setMimeType(mime);
        String fileEntryKey = key + "_" + index;
        BatchManager bm = Framework.getService(BatchManager.class);
        TransientStore ts = bm.getTransientStore();
        ts.putBlobs(fileEntryKey, Collections.singletonList(blob));
        ts.putParameter(fileEntryKey, CHUNKED_PARAM_NAME, String.valueOf(false));
        ts.putParameter(key, index, fileEntryKey);
        return fileEntryKey;
    }

    /**
     * Adds a chunk with the given {@code chunkIndex} to the batch file with the given {@code index}.
     *
     * @return The key of the {@link BatchFileEntry}.
     * @since 7.4
     * @deprecated since 10.1, use the {@link Blob}-based signature instead
     */
    @Deprecated
    public String addChunk(String index, InputStream is, int chunkCount, int chunkIndex, String fileName,
            String mimeType, long fileSize) throws IOException {
        Blob blob = Blobs.createBlob(is);
        return addChunk(index, blob, chunkCount, chunkIndex, fileName, mimeType, fileSize);
    }

    /**
     * Adds a chunk with the given {@code chunkIndex} to the batch file with the given {@code index}.
     *
     * @return The key of the {@link BatchFileEntry}.
     * @since 10.1
     */
    public String addChunk(String index, Blob blob, int chunkCount, int chunkIndex, String fileName, String mimeType,
            long fileSize) throws IOException {
        String fileEntryKey = key + "_" + index;
        BatchFileEntry fileEntry = getFileEntry(index);
        if (fileEntry == null) {
            fileEntry = new BatchFileEntry(fileEntryKey, chunkCount, fileName, mimeType, fileSize);
            BatchManager bm = Framework.getService(BatchManager.class);
            TransientStore ts = bm.getTransientStore();
            ts.putParameters(fileEntryKey, fileEntry.getParams());
            ts.putParameter(key, index, fileEntryKey);
        }
        fileEntry.addChunk(chunkIndex, blob);
        return fileEntryKey;
    }

    /**
     * @since 7.4
     */
    public void clean() {
        // Remove batch and all related storage entries from transient store, GC will clean up the files
        log.debug(String.format("Cleaning batch %s", key));
        BatchManager bm = Framework.getService(BatchManager.class);
        TransientStore ts = bm.getTransientStore();
        for (String fileIndex : fileEntries.keySet()) {
            removeFileEntry(fileIndex, ts);
        }
        // Remove batch entry
        ts.remove(key);
    }

    /**
     * @since 8.4
     */
    public boolean removeFileEntry(String index, TransientStore ts) {
        // Check for chunk entries to remove
        BatchFileEntry fileEntry = getFileEntry(index, false);
        if (fileEntry == null) {
            return false;
        }
        if (fileEntry.isChunked()) {
            for (String chunkEntryKey : fileEntry.getChunkEntryKeys()) {
                ts.remove(chunkEntryKey);
            }
            fileEntry.beforeRemove();
        }
        String fileEntryKey = fileEntry.getKey();
        ts.remove(fileEntryKey);
        return true;
    }

    /**
     * @since 8.4
     */
    public boolean removeFileEntry(String index) {
        BatchManager bm = Framework.getService(BatchManager.class);
        TransientStore ts = bm.getTransientStore();
        return removeFileEntry(index, ts);
    }
}
