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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.AbstractStorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;

/**
 * Represents a batch file backed by the {@link TransientStore}.
 * <p>
 * The file can be chunked or not. If it is chunked it references its chunks as {@link BatchChunkEntry} objects.
 *
 * @since 7.4
 * @see Batch
 */
public class BatchFileEntry extends AbstractStorageEntry {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(BatchFileEntry.class);

    protected Blob chunkedBlob;

    // Temporary file made from concatenated chunks
    protected File tmpChunkedFile;

    /**
     * Returns a file entry that holds the given blob, not chunked.
     */
    public BatchFileEntry(String id, Blob blob) {
        super(id);
        put("chunked", false);
        setBlobs(Collections.singletonList(blob));
    }

    /**
     * Returns a file entry that references the file chunks.
     *
     * @see BatchChunkEntry
     */
    public BatchFileEntry(String id, int chunkCount, String fileName, String mime, long fileSize) {
        super(id);
        put("chunked", true);
        put("chunkCount", chunkCount);
        put("chunks", new HashMap<Integer, String>());
        put("fileName", fileName);
        put("mimeType", mime);
        put("fileSize", fileSize);
    }

    public boolean isChunked() {
        return (Boolean) get("chunked");
    }

    public String getFileName() {
        if (isChunked()) {
            return (String) get("fileName");
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
            return (String) get("mimeType");
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
            return (long) get("fileSize");
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
            throw new NuxeoException(String.format("Cannot get chunk count of file entry %s as it is not chunked",
                    getId()));
        }
        return (int) get("chunkCount");
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, String> getChunks() {
        if (!isChunked()) {
            throw new NuxeoException(String.format("Cannot get chunks of file entry %s as it is not chunked", getId()));
        }
        return (Map<Integer, String>) get("chunks");
    }

    public List<Integer> getOrderedChunkIds() {
        if (!isChunked()) {
            throw new NuxeoException(String.format("Cannot get chunk ids of file entry %s as it is not chunked",
                    getId()));
        }
        List<Integer> sortedChunkIds = new ArrayList<Integer>(getChunks().keySet());
        Collections.sort(sortedChunkIds);
        return sortedChunkIds;
    }

    public Collection<String> getChunkEntryIds() {
        if (!isChunked()) {
            throw new NuxeoException(String.format("Cannot get chunk entry ids of file entry %s as it is not chunked",
                    getId()));
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
            try {
                Map<Integer, String> chunks = getChunks();
                int uploadedChunkCount = chunks.size();
                int chunkCount = getChunkCount();
                if (uploadedChunkCount != chunkCount) {
                    log.warn(String.format(
                            "Cannot get blob for file entry %s as there are only %d uploaded chunks out of %d.",
                            getId(), uploadedChunkCount, chunkCount));
                    return null;
                }
                chunkedBlob = Blobs.createBlobWithExtension(null);
                tmpChunkedFile = chunkedBlob.getFile();
                BatchManager bm = Framework.getService(BatchManager.class);
                // Sort chunk ids and concatenate them to build the entire blob
                List<Integer> sortedChunks = getOrderedChunkIds();
                for (int idx : sortedChunks) {
                    BatchChunkEntry chunkEntry = (BatchChunkEntry) bm.getTransientStore().get(chunks.get(idx));
                    Blob chunkBlob = chunkEntry.getBlob();
                    if (chunkBlob != null) {
                        transferTo(chunkBlob, tmpChunkedFile);
                    }
                }
                chunkedBlob.setMimeType(getMimeType());
                chunkedBlob.setFilename(getFileName());
                return chunkedBlob;
            } catch (IOException ioe) {
                beforeRemove();
                chunkedBlob = null;
                throw new NuxeoException(ioe);
            }
        } else {
            List<Blob> blobs = getBlobs();
            if (CollectionUtils.isEmpty(blobs)) {
                return null;
            }
            return blobs.get(0);
        }
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

    public String addChunk(int idx, Blob blob) {
        if (!isChunked()) {
            throw new NuxeoException("Cannot add a chunk to a non chunked file entry.");
        }
        int chunkCount = getChunkCount();
        if (idx < 0) {
            throw new NuxeoException(String.format("Cannot add chunk with negative index %d.", idx));
        }
        if (idx >= chunkCount) {
            throw new NuxeoException(String.format(
                    "Cannot add chunk with index %d to file entry %s as chunk count is %d.", idx, getId(), chunkCount));
        }
        if (getChunks().containsKey(idx)) {
            throw new NuxeoException(String.format(
                    "Cannot add chunk with index %d to file entry %s as it already exists.", idx, getId()));
        }

        String chunkEntryId = getId() + "_" + idx;
        BatchChunkEntry chunkEntry = new BatchChunkEntry(chunkEntryId, blob);

        BatchManager bm = Framework.getService(BatchManager.class);
        bm.getTransientStore().put(chunkEntry);

        return chunkEntryId;
    }

    @Override
    public List<Blob> getBlobs() {
        if (isChunked()) {
            return Collections.singletonList(getBlob());
        } else {
            return super.getBlobs();
        }
    }

    @Override
    public void beforeRemove() {
        if (tmpChunkedFile != null && tmpChunkedFile.exists()) {
            log.debug(String.format("Deleting temporary chunked file %s", tmpChunkedFile.getAbsolutePath()));
            tmpChunkedFile.delete();
        }
    }
}
