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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
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

    protected boolean isChunked() {
        return (Boolean) get("chunked");
    }

    /**
     * Returns a file entry that holds the given blob, not chunked.
     */
    public BatchFileEntry(String id, Blob blob) {
        super(id);
        put("chunked", false);
        addBlob(blob);
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

    @SuppressWarnings("unchecked")
    public Blob getBlob() {
        if (isChunked()) {
            try {
                // TODO https://jira.nuxeo.com/browse/NXP-17885
                // Find a way to delete tmp file once used
                Blob blob = Blobs.createBlobWithExtension(null);
                BatchManager bm = Framework.getService(BatchManager.class);
                Map<Integer, String> chunks = (Map<Integer, String>) get("chunks");
                int uploadedChunkCount = chunks.keySet().size();
                int chunkCount = (int) get("chunkCount");
                if (uploadedChunkCount != chunkCount) {
                    log.warn(String.format(
                            "Cannot get blob for file entry %s as there are only %d uploaded chunks out of %d.",
                            getId(), uploadedChunkCount, chunkCount));
                    return null;
                }
                // Sort chunks and concatenate them to build the entire blob
                List<Integer> sortedChunks = new ArrayList<Integer>(chunks.keySet());
                Collections.sort(sortedChunks);
                for (int idx : sortedChunks) {
                    BatchChunkEntry chunkEntry = (BatchChunkEntry) bm.getTransientStore().get(chunks.get(idx));
                    chunkEntry.getBlob().transferTo(blob.getFile());
                }
                blob.setMimeType((String) get("mimeType"));
                blob.setFilename((String) get("fileName"));
                return blob;
            } catch (IOException ioe) {
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

    @SuppressWarnings("unchecked")
    public BatchChunkEntry addChunk(int idx, Blob blob) {
        if (!isChunked()) {
            throw new NuxeoException("Cannot add a chunk to a non chunked file entry.");
        }
        int chunkCount = (Integer) get("chunkCount");
        if (idx < 0) {
            throw new NuxeoException(String.format("Cannot add chunk with negative index %d.", idx));
        }
        if (idx >= chunkCount) {
            throw new NuxeoException(String.format(
                    "Cannot add chunk with index %d to file entry %s as chunk count is %d.", idx, getId(), chunkCount));
        }
        Map<Integer, String> chunks = (Map<Integer, String>) get("chunks");
        if (chunks.containsKey(idx)) {
            throw new NuxeoException(String.format(
                    "Cannot add chunk with index %d to file entry %s as it already exists.", idx, getId()));
        }

        String chunkEntryId = getId() + "_" + idx;
        BatchChunkEntry chunkEntry = new BatchChunkEntry(chunkEntryId, blob);

        BatchManager bm = Framework.getService(BatchManager.class);
        bm.getTransientStore().put(chunkEntry);
        chunks.put(idx, chunkEntryId);

        return chunkEntry;
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getChunkEntryIds() {
        if (!isChunked()) {
            throw new NuxeoException(String.format("Cannot get chunk entry ids of file entry %s as it is not chunked",
                    getId()));
        }
        return ((Map<Integer, String>) get("chunks")).values();
    }

    @Override
    public void beforeRemove() {
        // Nothing to do here
    }
}
