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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected static final Log log = LogFactory.getLog(Batch.class);

    public Batch(String id) {
        super(id);
    }

    /**
     * Returns the uploaded blobs in the order the user chose to upload them.
     */
    @Override
    public List<Blob> getBlobs() {
        List<Blob> blobs = new ArrayList<Blob>();
        if (getParameters() == null) {
            return blobs;
        }
        List<String> sortedIdx = new ArrayList<String>(getParameters().keySet());
        Collections.sort(sortedIdx, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
            }
        });
        log.debug(String.format("Retrieving blobs for batch %s: %s", getId(), sortedIdx));
        for (String idx : sortedIdx) {
            Blob blob = retrieveBlob(idx);
            if (blob != null) {
                blobs.add(blob);
            }
        }
        return blobs;
    }

    public Blob getBlob(String idx) {
        log.debug(String.format("Retrieving blob %s for batch %s", idx, getId()));
        return retrieveBlob(idx);
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

    /**
     * Adds a file with the given {@code idx} to the batch.
     *
     * @return The id of the new {@link BatchFileEntry}.
     */
    public String addFile(String idx, InputStream is, String name, String mime) throws IOException {
        String mimeType = mime;
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        Blob blob = Blobs.createBlob(is, mime);
        blob.setFilename(name);

        String fileEntryId = getId() + "_" + idx;
        BatchFileEntry fileEntry = new BatchFileEntry(fileEntryId, blob);

        BatchManager bm = Framework.getService(BatchManager.class);
        bm.getTransientStore().put(fileEntry);

        return fileEntryId;
    }

    /**
     * Adds a chunk with the given {@code chunkIdx} to the batch file with the given {@code idx}.
     *
     * @return The id of the {@link BatchFileEntry}.
     * @since 7.4
     */
    public String addChunk(String idx, InputStream is, int chunkCount, int chunkIdx, String name, String mime,
            long fileSize) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        Blob blob = Blobs.createBlob(is);

        BatchFileEntry fileEntry = null;
        String fileEntryId = (String) get(idx);
        if (fileEntryId != null) {
            fileEntry = (BatchFileEntry) bm.getTransientStore().get(fileEntryId);
        }
        if (fileEntry == null) {
            if (fileEntryId == null) {
                fileEntryId = getId() + "_" + idx;
            }
            fileEntry = new BatchFileEntry(fileEntryId, chunkCount, name, mime, fileSize);
            bm.getTransientStore().put(fileEntry);
        }
        String chunkEntryId = fileEntry.addChunk(chunkIdx, blob);

        // Need to synchronize manipulation of the file TransientStore entry params
        synchronized (this) {
            fileEntry = (BatchFileEntry) bm.getTransientStore().get(fileEntryId);
            fileEntry.getChunks().put(chunkIdx, chunkEntryId);
            put(idx, fileEntryId);
            bm.getTransientStore().put(fileEntry);
        }
        return fileEntryId;
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
