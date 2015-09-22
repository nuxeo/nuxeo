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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchChunkEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
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

        // Check transient store
        StorageEntry se = bm.getTransientStore().get(batchId);
        assertTrue(se instanceof Batch);
        assertEquals(batchId, ((Batch) se).getId());

        // Init with a batch id
        batchId = bm.initBatch("testBatchId", null);
        assertEquals("testBatchId", batchId);
        assertTrue(bm.hasBatch("testBatchId"));

        // Check transient store
        se = bm.getTransientStore().get("testBatchId");
        assertTrue(se instanceof Batch);
        assertEquals("testBatchId", ((Batch) se).getId());

        // Check unicity
        batchId = bm.initBatch("testBatchId", null);
        assertEquals("testBatchId", batchId);
        assertEquals(se, bm.getTransientStore().get("testBatchId"));
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
        StorageEntry batchSE = bm.getTransientStore().get(batchId);
        assertTrue(batchSE instanceof Batch);
        Batch batch = (Batch) batchSE;
        assertEquals(batchId, batch.getId());
        assertEquals(blob1, batch.getBlob("0"));
        assertEquals(blob2, batch.getBlob("1"));
        assertTrue(ListUtils.isEqualList(blobs, batch.getBlobs()));
        // Batch file entries
        assertEquals(2, batch.getParameters().size());
        assertEquals(batchId + "_0", batch.get("0"));
        assertEquals(batchId + "_1", batch.get("1"));

        StorageEntry fileSE1 = bm.getTransientStore().get(batchId + "_0");
        assertTrue(fileSE1 instanceof BatchFileEntry);
        BatchFileEntry fileEntry1 = (BatchFileEntry) fileSE1;
        assertEquals(batchId + "_0", fileEntry1.getId());
        assertFalse(fileEntry1.isChunked());
        assertEquals("Mon doc 1.txt", fileEntry1.getFileName());
        assertEquals("text/plain", fileEntry1.getMimeType());
        assertEquals(17, fileEntry1.getFileSize());
        assertEquals(blob1, fileEntry1.getBlob());
        assertTrue(ListUtils.isEqualList(Collections.singletonList(blob1), fileEntry1.getBlobs()));

        StorageEntry fileSE2 = bm.getTransientStore().get(batchId + "_1");
        assertTrue(fileSE2 instanceof BatchFileEntry);
        BatchFileEntry fileEntry2 = (BatchFileEntry) fileSE2;
        assertEquals(batchId + "_1", fileEntry2.getId());
        assertFalse(fileEntry2.isChunked());
        assertEquals("Mon doc 2.txt", fileEntry2.getFileName());
        assertEquals("text/plain", fileEntry2.getMimeType());
        assertEquals(23, fileEntry2.getFileSize());
        assertEquals(blob2, fileEntry2.getBlob());
        assertTrue(ListUtils.isEqualList(Collections.singletonList(blob2), fileEntry2.getBlobs()));
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
        StorageEntry batchSE = bm.getTransientStore().get(batchId);
        assertTrue(batchSE instanceof Batch);
        Batch batch = (Batch) batchSE;
        assertEquals(batchId, batch.getId());
        assertEquals(blob, batch.getBlob("0"));
        // Batch file entries
        assertEquals(1, batch.getParameters().size());
        assertEquals(batchId + "_0", batch.get("0"));
        StorageEntry fileSE = bm.getTransientStore().get(batchId + "_0");
        assertTrue(fileSE instanceof BatchFileEntry);
        BatchFileEntry fileEntry = (BatchFileEntry) fileSE;
        assertEquals(batchId + "_0", fileEntry.getId());
        assertTrue(fileEntry.isChunked());
        assertEquals("Mon doc.txt", fileEntry.getFileName());
        assertEquals("text/plain", fileEntry.getMimeType());
        assertEquals(fileSize, fileEntry.getFileSize());
        assertEquals(3, fileEntry.getChunkCount());
        assertEquals(Arrays.asList(0, 1, 2), fileEntry.getOrderedChunkIds());
        assertEquals(blob, fileEntry.getBlob());
        assertTrue(ListUtils.isEqualList(Collections.singletonList(blob), fileEntry.getBlobs()));

        // Batch chunk entries
        Collection<String> chunkEntryIds = fileEntry.getChunkEntryIds();
        assertEquals(3, chunkEntryIds.size());

        String chunkEntryId1 = batchId + "_0_0";
        assertTrue(chunkEntryIds.contains(chunkEntryId1));
        StorageEntry chunkSE1 = bm.getTransientStore().get(chunkEntryId1);
        assertTrue(chunkSE1 instanceof BatchChunkEntry);
        BatchChunkEntry chunkEntry1 = (BatchChunkEntry) chunkSE1;
        assertEquals(batchId + "_0_0", chunkEntry1.getId());
        Blob blob1 = chunkEntry1.getBlob();
        assertEquals(chunk1, blob1.getString());
        assertEquals(15, blob1.getLength());
        assertTrue(ListUtils.isEqualList(Collections.singletonList(blob1), chunkEntry1.getBlobs()));

        String chunkEntryId2 = batchId + "_0_1";
        assertTrue(chunkEntryIds.contains(chunkEntryId2));
        StorageEntry chunkSE2 = bm.getTransientStore().get(chunkEntryId2);
        assertTrue(chunkSE2 instanceof BatchChunkEntry);
        BatchChunkEntry chunkEntry2 = (BatchChunkEntry) chunkSE2;
        assertEquals(batchId + "_0_1", chunkEntry2.getId());
        Blob blob2 = chunkEntry2.getBlob();
        assertEquals(chunk2, blob2.getString());
        assertEquals(15, blob2.getLength());
        assertTrue(ListUtils.isEqualList(Collections.singletonList(blob2), chunkEntry2.getBlobs()));

        String chunkEntryId3 = batchId + "_0_2";
        assertTrue(chunkEntryIds.contains(chunkEntryId3));
        StorageEntry chunkSE3 = bm.getTransientStore().get(chunkEntryId3);
        assertTrue(chunkSE3 instanceof BatchChunkEntry);
        BatchChunkEntry chunkEntry3 = (BatchChunkEntry) chunkSE3;
        assertEquals(batchId + "_0_2", chunkEntry3.getId());
        Blob blob3 = chunkEntry3.getBlob();
        assertEquals(chunk3, blob3.getString());
        assertEquals(8, blob3.getLength());
        assertTrue(ListUtils.isEqualList(Collections.singletonList(blob3), chunkEntry3.getBlobs()));

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
        assertNotNull(bm.getTransientStore().get(batchId));
        assertNotNull(bm.getTransientStore().get(batchId + "_5"));
        assertNotNull(bm.getTransientStore().get(batchId + "_10"));
        assertNotNull(bm.getTransientStore().get(batchId + "_10_0"));
        assertNotNull(bm.getTransientStore().get(batchId + "_10_1"));

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
        assertNull(bm.getTransientStore().get(batchId));
        assertNull(bm.getTransientStore().get(batchId + "_5"));
        assertNull(bm.getTransientStore().get(batchId + "_10"));
        assertNull(bm.getTransientStore().get(batchId + "_10_0"));
        assertNull(bm.getTransientStore().get(batchId + "_10_1"));
        assertFalse(tmpChunkedFile.exists());
        assertTrue(tmpFile.exists());

        TransientStore ts = bm.getTransientStore();
        ts.doGC();
        assertFalse(tmpFile.exists());
    }

}
