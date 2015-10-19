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
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerComponent;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.AbstractTransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.core.cache" })
public class BatchManagerTest {

    @Test
    public void testServiceRegistred() {
        BatchManager bm = Framework.getService(BatchManager.class);
        assertNotNull(bm);
    }

    @Test
    public void testTransientStoreRegistered() {
        BatchManager bm = Framework.getService(BatchManager.class);
        assertNotNull(bm.getTransientStore());
    }

    @Test
    public void testBatchInit() {
        // Init with no batch id
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();
        assertNotNull(batchId);
        assertTrue(bm.hasBatch(batchId));
        Batch batch = ((BatchManagerComponent) bm).getBatch(batchId);
        assertNotNull(batch);
        assertEquals(batchId, batch.getKey());

        // Init with a batch id
        batchId = bm.initBatch("testBatchId", null);
        assertEquals("testBatchId", batchId);
        assertTrue(bm.hasBatch("testBatchId"));
        batch = ((BatchManagerComponent) bm).getBatch("testBatchId");
        assertNotNull(batch);
        assertEquals("testBatchId", batch.getKey());

    }

    @Test
    public void testAddFileStream() throws IOException {
        // Add 2 file streams
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();
        InputStream is = new ByteArrayInputStream("Contenu accentué".getBytes("UTF-8"));
        bm.addStream(batchId, "0", is, "Mon doc 1.txt", "text/plain");
        is = new ByteArrayInputStream("Autre contenu accentué".getBytes("UTF-8"));
        bm.addStream(batchId, "1", is, "Mon doc 2.txt", "text/plain");

        // Check batch blobs
        Blob blob1 = bm.getBlob(batchId, "0");
        assertEquals("Mon doc 1.txt", blob1.getFilename());
        assertEquals("text/plain", blob1.getMimeType());
        assertEquals("Contenu accentué", blob1.getString());
        Blob blob2 = bm.getBlob(batchId, "1");
        assertEquals("Mon doc 2.txt", blob2.getFilename());
        assertEquals("text/plain", blob2.getMimeType());
        assertEquals("Autre contenu accentué", blob2.getString());
        List<Blob> blobs = bm.getBlobs(batchId);
        assertEquals(2, blobs.size());
        assertEquals(blob1, blobs.get(0));
        assertEquals(blob2, blobs.get(1));

        // Check transient store
        // Batch entry
        Batch batch = ((BatchManagerComponent) bm).getBatch(batchId);
        assertNotNull(batch);
        assertEquals(batchId, batch.getKey());
        assertEquals(blob1, batch.getBlob("0"));
        assertEquals(blob2, batch.getBlob("1"));
        assertTrue(ListUtils.isEqualList(blobs, batch.getBlobs()));

        // Batch file entries
        List<BatchFileEntry> batchFileEntries = batch.getFileEntries();
        assertEquals(2, batchFileEntries.size());

        BatchFileEntry fileEntry1 = batchFileEntries.get(0);
        assertEquals(batchId + "_0", fileEntry1.getKey());
        assertFalse(fileEntry1.isChunked());
        assertEquals("Mon doc 1.txt", fileEntry1.getFileName());
        assertEquals("text/plain", fileEntry1.getMimeType());
        assertEquals(17, fileEntry1.getFileSize());
        assertEquals(blob1, fileEntry1.getBlob());

        BatchFileEntry fileEntry2 = batchFileEntries.get(1);
        assertEquals(batchId + "_1", fileEntry2.getKey());
        assertFalse(fileEntry2.isChunked());
        assertEquals("Mon doc 2.txt", fileEntry2.getFileName());
        assertEquals("text/plain", fileEntry2.getMimeType());
        assertEquals(23, fileEntry2.getFileSize());
        assertEquals(blob2, fileEntry2.getBlob());
    }

    @Test
    public void testAddChunkStream() throws IOException {
        // Add 3 chunk streams in disorder
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();

        String fileContent = "Contenu accentué composé de 3 chunks";
        String chunk1 = "Contenu accentu";
        String chunk2 = "é composé de ";
        String chunk3 = "3 chunks";
        long fileSize = fileContent.getBytes().length;
        bm.addStream(batchId, "0", new ByteArrayInputStream(chunk1.getBytes("UTF-8")), 3, 0, "Mon doc.txt",
                "text/plain", fileSize);
        bm.addStream(batchId, "0", new ByteArrayInputStream(chunk3.getBytes("UTF-8")), 3, 2, "Mon doc.txt",
                "text/plain", fileSize);
        bm.addStream(batchId, "0", new ByteArrayInputStream(chunk2.getBytes("UTF-8")), 3, 1, "Mon doc.txt",
                "text/plain", fileSize);

        // Check batch blobs
        Blob blob = bm.getBlob(batchId, "0");
        bm.getBlob(batchId, "0");
        assertEquals("Mon doc.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(fileContent, blob.getString());

        // Check transient store

        // Batch entry
        Batch batch = ((BatchManagerComponent) bm).getBatch(batchId);
        assertNotNull(batch);
        assertEquals(batchId, batch.getKey());
        assertEquals(blob, batch.getBlob("0"));

        // Batch file entries
        List<BatchFileEntry> batchFileEntries = batch.getFileEntries();
        assertEquals(1, batchFileEntries.size());
        BatchFileEntry fileEntry = batchFileEntries.get(0);
        assertEquals(batchId + "_0", fileEntry.getKey());
        assertTrue(fileEntry.isChunked());
        assertEquals("Mon doc.txt", fileEntry.getFileName());
        assertEquals("text/plain", fileEntry.getMimeType());
        assertEquals(fileSize, fileEntry.getFileSize());
        assertEquals(3, fileEntry.getChunkCount());
        assertEquals(Arrays.asList(0, 1, 2), fileEntry.getOrderedChunkIndexes());
        assertEquals(blob, fileEntry.getBlob());

        // Batch chunk entries
        Collection<String> chunkEntryKeys = fileEntry.getChunkEntryKeys();
        assertEquals(3, chunkEntryKeys.size());

        String chunkEntryKey1 = batchId + "_0_0";
        assertTrue(chunkEntryKeys.contains(chunkEntryKey1));
        List<Blob> chunkEntryBlobs = bm.getTransientStore().getBlobs(chunkEntryKey1);
        assertEquals(1, chunkEntryBlobs.size());
        Blob blob1 = chunkEntryBlobs.get(0);
        assertEquals(chunk1, blob1.getString());
        assertEquals(15, blob1.getLength());

        String chunkEntryKey2 = batchId + "_0_1";
        assertTrue(chunkEntryKeys.contains(chunkEntryKey2));
        chunkEntryBlobs = bm.getTransientStore().getBlobs(chunkEntryKey2);
        assertEquals(1, chunkEntryBlobs.size());
        Blob blob2 = chunkEntryBlobs.get(0);
        assertEquals(chunk2, blob2.getString());
        assertEquals(15, blob2.getLength());

        String chunkEntryKey3 = batchId + "_0_2";
        assertTrue(chunkEntryKeys.contains(chunkEntryKey3));
        chunkEntryBlobs = bm.getTransientStore().getBlobs(chunkEntryKey3);
        assertEquals(1, chunkEntryBlobs.size());
        Blob blob3 = chunkEntryBlobs.get(0);
        assertEquals(chunk3, blob3.getString());
        assertEquals(8, blob3.getLength());

        // Clean batch
        bm.clean(batchId);
    }

    @Test
    public void testBatchCleanup() throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);

        String batchId = bm.initBatch(null, null);
        assertNotNull(batchId);

        // Add non chunked files
        for (int i = 0; i < 10; i++) {
            bm.addStream(batchId, "" + i, new ByteArrayInputStream(("SomeContent" + i).getBytes()), i + ".txt",
                    "text/plain");
        }
        // Add chunked file
        bm.addStream(batchId, "10", new ByteArrayInputStream(("Chunk 1 ").getBytes()), 2, 0, "chunkedFile.txt",
                "text/plain", 16);
        bm.addStream(batchId, "10", new ByteArrayInputStream(("Chunk 2 ").getBytes()), 2, 1, "chunkedFile.txt",
                "text/plain", 16);

        List<Blob> blobs = bm.getBlobs(batchId);
        assertNotNull(blobs);
        Assert.assertEquals(11, blobs.size());

        Assert.assertEquals("4.txt", blobs.get(4).getFilename());
        Assert.assertEquals("SomeContent7", blobs.get(7).getString());
        Assert.assertEquals("Chunk 1 Chunk 2 ", blobs.get(10).getString());

        // Batch data
        assertTrue(bm.getTransientStore().exists(batchId));
        assertTrue(bm.getTransientStore().exists(batchId + "_5"));
        assertTrue(bm.getTransientStore().exists(batchId + "_10"));
        assertTrue(bm.getTransientStore().exists(batchId + "_10_0"));
        assertTrue(bm.getTransientStore().exists(batchId + "_10_1"));

        // Batch non chunked file
        FileBlob fileBlob = (FileBlob) blobs.get(9);
        File tmpFile = fileBlob.getFile();
        assertNotNull(tmpFile);
        assertTrue(tmpFile.exists());

        // Batch chunked file
        FileBlob chunkedFileBlob = (FileBlob) blobs.get(10);
        File tmpChunkedFile = chunkedFileBlob.getFile();
        assertNotNull(tmpChunkedFile);
        assertTrue(tmpChunkedFile.exists());

        bm.clean(batchId);
        // Batch data has been removed from cache as well as temporary chunked file, but non chunked file is still there
        // while transient store GC is not called
        assertFalse(bm.getTransientStore().exists(batchId));
        assertFalse(bm.getTransientStore().exists(batchId + "_5"));
        assertFalse(bm.getTransientStore().exists(batchId + "_10"));
        assertFalse(bm.getTransientStore().exists(batchId + "_10_0"));
        assertFalse(bm.getTransientStore().exists(batchId + "_10_1"));
        assertFalse(tmpChunkedFile.exists());
        assertTrue(tmpFile.exists());

        TransientStore ts = bm.getTransientStore();
        ts.doGC();
        assertFalse(tmpFile.exists());
    }

}
