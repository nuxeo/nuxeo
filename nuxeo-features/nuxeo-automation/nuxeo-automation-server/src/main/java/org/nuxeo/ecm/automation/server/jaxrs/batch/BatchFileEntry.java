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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;

/**
 * Represents a batch file backed by the {@link TransientStore}.
 * <p>
 * The file can be chunked or not. If it is chunked it references its chunks as {@link TransientStore} entry keys.
 *
 * @since 7.4
 * @see Batch
 */
public class BatchFileEntry {

    protected static final Log log = LogFactory.getLog(BatchFileEntry.class);

    protected String key;

    protected Map<String, Serializable> params;

    protected Blob blob;

    protected Blob chunkedBlob;

    /**
     * Returns a file entry that holds the given blob, not chunked.
     */
    public BatchFileEntry(String key, Blob blob) {
        this(key, false);
        this.blob = blob;
    }

    /**
     * Returns a file entry that references the file chunks.
     */
    public BatchFileEntry(String key, int chunkCount, String fileName, String mimeType, long fileSize) {
        this(key, true);
        params.put("chunkCount", String.valueOf(chunkCount));
        if (!StringUtils.isEmpty(fileName)) {
            params.put("fileName", fileName);
        }
        if (!StringUtils.isEmpty(mimeType)) {
            params.put("mimeType", mimeType);
        }
        params.put("fileSize", String.valueOf(fileSize));
    }

    /**
     * Returns a file entry that holds the given parameters.
     */
    public BatchFileEntry(String key, Map<String, Serializable> params) {
        this.key = key;
        this.params = params;
    }

    protected BatchFileEntry(String key, boolean chunked) {
        this.key = key;
        params = new HashMap<>();
        params.put(Batch.CHUNKED_PARAM_NAME, String.valueOf(chunked));
    }

    public String getKey() {
        return key;
    }

    public Map<String, Serializable> getParams() {
        return params;
    }

    public boolean isChunked() {
        return Boolean.parseBoolean((String) params.get(Batch.CHUNKED_PARAM_NAME));
    }

    public String getFileName() {
        if (isChunked()) {
            return (String) params.get("fileName");
        } else {
            Blob blob = getBlob();
            if (blob == null) {
                return null;
            } else {
                return blob.getFilename();
            }
        }
    }

    public String getMimeType() {
        if (isChunked()) {
            return (String) params.get("mimeType");
        } else {
            Blob blob = getBlob();
            if (blob == null) {
                return null;
            } else {
                return blob.getMimeType();
            }
        }
    }

    public long getFileSize() {
        if (isChunked()) {
            return Long.parseLong((String) params.get("fileSize"));
        } else {
            Blob blob = getBlob();
            if (blob == null) {
                return -1;
            } else {
                return blob.getLength();
            }
        }
    }

    public int getChunkCount() {
        if (!isChunked()) {
            throw new NuxeoException(
                    String.format("Cannot get chunk count of file entry %s as it is not chunked", key));
        }
        return Integer.parseInt((String) params.get("chunkCount"));
    }

    public Map<Integer, String> getChunks() {
        if (!isChunked()) {
            throw new NuxeoException(String.format("Cannot get chunks of file entry %s as it is not chunked", key));
        }
        Map<Integer, String> chunks = new HashMap<>();
        for (String param : params.keySet()) {
            if (NumberUtils.isDigits(param)) {
                chunks.put(Integer.valueOf(param), (String) params.get(param));
            }
        }
        return chunks;
    }

    public List<Integer> getOrderedChunkIndexes() {
        if (!isChunked()) {
            throw new NuxeoException(
                    String.format("Cannot get chunk indexes of file entry %s as it is not chunked", key));
        }
        List<Integer> sortedChunkIndexes = new ArrayList<>(getChunks().keySet());
        Collections.sort(sortedChunkIndexes);
        return sortedChunkIndexes;
    }

    public Collection<String> getChunkEntryKeys() {
        if (!isChunked()) {
            throw new NuxeoException(
                    String.format("Cannot get chunk entry keys of file entry %s as it is not chunked", key));
        }
        return getChunks().values();
    }

    public boolean isChunksCompleted() {
        return getChunks().size() == getChunkCount();
    }

    public Blob getBlob() {
        if (isChunked()) {
            // First check if blob chunks have already been read and concatenated
            if (chunkedBlob != null) {
                return chunkedBlob;
            }
            File tmpChunkedFile = null;
            try {
                Map<Integer, String> chunks = getChunks();
                int uploadedChunkCount = chunks.size();
                int chunkCount = getChunkCount();
                if (uploadedChunkCount != chunkCount) {
                    log.warn(String.format(
                            "Cannot get blob for file entry %s as there are only %d uploaded chunks out of %d.", key,
                            uploadedChunkCount, chunkCount));
                    return null;
                }
                chunkedBlob = Blobs.createBlobWithExtension(null);
                // Temporary file made from concatenated chunks
                tmpChunkedFile = chunkedBlob.getFile();
                BatchManager bm = Framework.getService(BatchManager.class);
                TransientStore ts = bm.getTransientStore();
                // Sort chunk indexes and concatenate them to build the entire blob
                List<Integer> sortedChunkIndexes = getOrderedChunkIndexes();
                for (int index : sortedChunkIndexes) {
                    Blob chunk = getChunk(ts, chunks.get(index));
                    if (chunk != null) {
                        transferTo(chunk, tmpChunkedFile);
                    }
                }
                // Store tmpChunkedFile as a parameter for later deletion
                ts.putParameter(key, "tmpChunkedFilePath", tmpChunkedFile.getAbsolutePath());
                chunkedBlob.setMimeType(getMimeType());
                chunkedBlob.setFilename(getFileName());
                return chunkedBlob;
            } catch (IOException ioe) {
                if (tmpChunkedFile != null && tmpChunkedFile.exists()) {
                    tmpChunkedFile.delete();
                }
                chunkedBlob = null;
                throw new NuxeoException(ioe);
            }
        } else {
            return blob;
        }
    }

    protected Blob getChunk(TransientStore ts, String key) {
        List<Blob> blobs = ts.getBlobs(key);
        if (CollectionUtils.isEmpty(blobs)) {
            return null;
        }
        return blobs.get(0);
    }

    /**
     * Appends the given blob to the given file.
     */
    protected void transferTo(Blob blob, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file, true)) {
            try (InputStream in = blob.getStream()) {
                IOUtils.copy(in, out);
            }
        }
    }

    public String addChunk(int index, Blob blob) {
        if (!isChunked()) {
            throw new NuxeoException("Cannot add a chunk to a non chunked file entry.");
        }
        int chunkCount = getChunkCount();
        if (index < 0) {
            throw new NuxeoException(String.format("Cannot add chunk with negative index %d.", index));
        }
        if (index >= chunkCount) {
            throw new NuxeoException(String.format(
                    "Cannot add chunk with index %d to file entry %s as chunk count is %d.", index, key, chunkCount));
        }
        if (getChunks().containsKey(index)) {
            throw new NuxeoException(
                    String.format("Cannot add chunk with index %d to file entry %s as it already exists.", index, key));
        }

        String chunkEntryKey = key + "_" + index;
        BatchManager bm = Framework.getService(BatchManager.class);
        TransientStore ts = bm.getTransientStore();
        ts.putBlobs(chunkEntryKey, Collections.singletonList(blob));
        ts.putParameter(key, String.valueOf(index), chunkEntryKey);

        return chunkEntryKey;
    }

    public void beforeRemove() {
        BatchManager bm = Framework.getService(BatchManager.class);
        String tmpChunkedFilePath = (String) bm.getTransientStore().getParameter(key, "tmpChunkedFilePath");
        if (tmpChunkedFilePath != null) {
            File tmpChunkedFile = new File(tmpChunkedFilePath);
            if (tmpChunkedFile.exists()) {
                log.debug(String.format("Deleting temporary chunked file %s", tmpChunkedFilePath));
                tmpChunkedFile.delete();
            }
        }
    }
}
