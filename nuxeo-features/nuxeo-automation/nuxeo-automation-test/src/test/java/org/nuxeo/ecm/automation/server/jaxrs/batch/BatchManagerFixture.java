/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.ListUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, TransientStoreFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.platform.web.common", "org.nuxeo.ecm.webengine.core",
        "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.automation.server" })
public class BatchManagerFixture {

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
    public void testBatchInit() throws Exception {
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();
        assertNotNull(batchId);
        assertTrue(bm.hasBatch(batchId));
        Batch batch = bm.getBatch(batchId);
        assertNotNull(batch);
        assertEquals(batchId, batch.getKey());

        // Check TransientStore storage size
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        assertEquals(0, tsm.getStorageSize());
    }

    @Test(expected = NuxeoException.class)
    public void testBatchInitClientGeneratedIdNotAllowed() throws Exception {
        ((BatchManagerComponent) Framework.getService(BatchManager.class)).initBatchInternal("testBatchId");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:test-batchmanager-client-generated-id-allowed-contrib.xml")
    public void testBatchInitClientGeneratedIdAllowed() throws Exception {
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = ((BatchManagerComponent) bm).initBatchInternal("testBatchId").getKey();
        assertEquals("testBatchId", batchId);
        assertTrue(bm.hasBatch("testBatchId"));
        Batch batch = bm.getBatch("testBatchId");
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

        // Check TransientStore storage size
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        assertEquals(40, tsm.getStorageSize());
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
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        List<Blob> chunkEntryBlobs = ts.getBlobs(chunkEntryKey1);
        assertEquals(1, chunkEntryBlobs.size());
        Blob blob1 = chunkEntryBlobs.get(0);
        assertEquals(chunk1, blob1.getString());
        assertEquals(15, blob1.getLength());

        String chunkEntryKey2 = batchId + "_0_1";
        assertTrue(chunkEntryKeys.contains(chunkEntryKey2));
        chunkEntryBlobs = ts.getBlobs(chunkEntryKey2);
        assertEquals(1, chunkEntryBlobs.size());
        Blob blob2 = chunkEntryBlobs.get(0);
        assertEquals(chunk2, blob2.getString());
        assertEquals(15, blob2.getLength());

        String chunkEntryKey3 = batchId + "_0_2";
        assertTrue(chunkEntryKeys.contains(chunkEntryKey3));
        chunkEntryBlobs = ts.getBlobs(chunkEntryKey3);
        assertEquals(1, chunkEntryBlobs.size());
        Blob blob3 = chunkEntryBlobs.get(0);
        assertEquals(chunk3, blob3.getString());
        assertEquals(8, blob3.getLength());

        // Check TransientStore storage size
        assertEquals(38, tsm.getStorageSize());

        // Clean batch
        bm.clean(batchId);
        assertEquals(0, tsm.getStorageSize());
    }

    @Test
    public void testBatchCleanup() throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);

        String batchId = bm.initBatch();
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
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        assertTrue(ts.exists(batchId));
        assertTrue(ts.exists(batchId + "_5"));
        assertTrue(ts.exists(batchId + "_10"));
        assertTrue(ts.exists(batchId + "_10_0"));
        assertTrue(ts.exists(batchId + "_10_1"));

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
        // Batch data has been removed from cache as well as temporary chunked file, and non chunked file
        assertFalse(ts.exists(batchId));
        assertFalse(ts.exists(batchId + "_5"));
        assertFalse(ts.exists(batchId + "_10"));
        assertFalse(ts.exists(batchId + "_10_0"));
        assertFalse(ts.exists(batchId + "_10_1"));
        assertFalse(tmpChunkedFile.exists());
        assertFalse(tmpFile.exists());
        assertEquals(0, tsm.getStorageSize());
    }

    @Test
    public void testBatchConcurrency() throws Exception {

        BatchManager bm = Framework.getService(BatchManager.class);

        // Initialize batches with one file concurrently
        int nbBatches = 100;
        String[] batchIds = new String[nbBatches];
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(nbBatches + 1));

        for (int i = 0; i < nbBatches; i++) {
            final int batchIndex = i;
            tpe.submit(() -> {
                try {
                    String batchId = bm.initBatch();
                    bm.addStream(batchId, "0",
                            new ByteArrayInputStream(("SomeContent_" + batchId).getBytes(StandardCharsets.UTF_8)),
                            "MyBatchFile.txt", "text/plain");
                    batchIds[batchIndex] = batchId;
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue("timeout", finish);

        // Check batches
        for (String batchId : batchIds) {
            assertNotNull(batchId);
        }
        // Test indexes 0, 9, 99, ..., nbFiles - 1
        int nbDigits = (int) (Math.log10(nbBatches) + 1);
        int divisor = nbBatches;
        for (int i = 0; i < nbDigits; i++) {
            int batchIndex = nbBatches / divisor - 1;
            String batchId = batchIds[batchIndex];
            Blob blob = bm.getBlob(batchId, "0");
            assertNotNull(blob);
            assertEquals("MyBatchFile.txt", blob.getFilename());
            assertEquals("SomeContent_" + batchId, blob.getString());
            divisor = divisor / 10;
        }

        // Check storage size
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        assertTrue(tsm.getStorageSize() > 12 * nbBatches);

        // Clean batches
        for (String batchId : batchIds) {
            bm.clean(batchId);
        }
        assertEquals(tsm.getStorageSize(), 0);
    }

    @Test
    public void testFileConcurrency() throws Exception {

        // Initialize a batch
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();

        // Add files concurrently
        int nbFiles = 100;
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(nbFiles + 1));

        for (int i = 0; i < nbFiles; i++) {
            final String fileIndex = String.valueOf(i);
            tpe.submit(() -> {
                try {
                    bm.addStream(batchId, fileIndex,
                            new ByteArrayInputStream(("SomeContent_" + fileIndex).getBytes(StandardCharsets.UTF_8)),
                            fileIndex + ".txt", "text/plain");
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue("timeout", finish);

        // Check blobs
        List<Blob> blobs = bm.getBlobs(batchId);
        assertEquals(nbFiles, blobs.size());
        // Test indexes 0, 9, 99, ..., nbFiles - 1
        int nbDigits = (int) (Math.log10(nbFiles) + 1);
        int divisor = nbFiles;
        for (int i = 0; i < nbDigits; i++) {
            int fileIndex = nbFiles / divisor - 1;
            assertEquals(fileIndex + ".txt", blobs.get(fileIndex).getFilename());
            assertEquals("SomeContent_" + fileIndex, blobs.get(fileIndex).getString());
            divisor = divisor / 10;
        }

        // Check storage size
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        assertTrue(tsm.getStorageSize() > 12 * nbFiles);

        // Clean batch
        bm.clean(batchId);
        assertEquals(tsm.getStorageSize(), 0);
    }

    @Test
    public void testChunkConcurrency() throws Exception {

        // Initialize a batch
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();

        // Add chunks concurrently
        int nbChunks = 100;
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(nbChunks + 1));

        for (int i = 0; i < nbChunks; i++) {
            final int chunkIndex = i;
            tpe.submit(() -> {
                try {
                    bm.addStream(batchId, "0",
                            new ByteArrayInputStream(
                                    ("SomeChunkContent_" + chunkIndex + " ").getBytes(StandardCharsets.UTF_8)),
                            nbChunks, chunkIndex, "MyChunkedFile.txt", "text/plain", 0);
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue("timeout", finish);

        // Check chunked file
        Blob blob = bm.getBlob(batchId, "0");
        assertNotNull(blob);
        int nbOccurrences = 0;
        Pattern p = Pattern.compile("SomeChunkContent_");
        Matcher m = p.matcher(blob.getString());
        while (m.find()) {
            nbOccurrences++;
        }
        assertEquals(nbChunks, nbOccurrences);

        // Check storage size
        TransientStore ts = bm.getTransientStore();
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        assertTrue(tsm.getStorageSize() > 17 * nbChunks);

        // Clean batch
        bm.clean(batchId);
        assertEquals(tsm.getStorageSize(), 0);
    }
}
