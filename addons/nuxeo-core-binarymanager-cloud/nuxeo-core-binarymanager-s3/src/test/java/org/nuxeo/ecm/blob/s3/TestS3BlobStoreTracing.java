/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobContext;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStatus;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.BlobUpdateContext;
import org.nuxeo.ecm.core.blob.CachingBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.KeyStrategyDocId;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.TransactionalBlobStore;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.work.WorkManagerFeature;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.amazonaws.services.s3.model.StorageClass;

@RunWith(FeaturesRunner.class)
@Features({ BlobManagerFeature.class, WorkManagerFeature.class, TransactionalFeature.class, LogCaptureFeature.class,
        MockitoFeature.class })
@LogCaptureFeature.FilterOn(logLevel = "TRACE", loggerClass = AbstractBlobStore.class)
@TransactionalConfig(autoStart = false)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-tracing.xml")
public class TestS3BlobStoreTracing {

    protected static final String XPATH = "content";

    protected static final String DOCID1 = "id1";

    protected static final String DOCID2 = "id2";

    protected static final String FOO = "foo";

    protected static final String BAR = "bar";

    protected static final String FOO_MD5 = "acbd18db4cc2f85cedef654fccc4a4d8";

    protected static final String FOO_SHA256 = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae";

    protected static final String BAR_MD5 = "37b51d194a7513e45b56f6524f2d51f2";

    // from test-blob-provider-s3-tracing.xml, for cleanup
    protected static final List<String> BLOB_PROVIDER_IDS = Arrays.asList( //
            "s3", //
            "s3-other", //
            "s3-subdirs", //
            "s3-sha256-async", //
            "s3-nocache", //
            "s3-managed", //
            "s3-coldStorage", //
            "s3-record");

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Mock
    @RuntimeService
    protected RepositoryService repositoryService;

    protected Path tmpFile;

    @BeforeClass
    public static void beforeClass() {
        S3TestHelper.getProperties().forEach(S3TestHelper::setProperty);
    }

    @Before
    public void setUp() throws IOException {
        clearBlobStores();
        tmpFile = Files.createTempFile("tmp_", ".tmp");
        logCaptureResult.clear();
    }

    @Before
    public void mockRepositoryService() {
        when(repositoryService.getRepositoryNames()).thenReturn(Collections.emptyList());
    }

    @After
    public void tearDown() throws IOException {
        clearBlobStores();
        Files.deleteIfExists(tmpFile);
    }

    protected void clearBlobStores() {
        BLOB_PROVIDER_IDS.forEach(id -> getBlobStore(id).clear());
    }

    protected BlobStore getBlobStore(String id) {
        return ((BlobStoreBlobProvider) getBlobProvider(id)).store;
    }

    protected BlobProvider getBlobProvider(String id) {
        return blobManager.getBlobProvider(id);
    }

    protected void clearCache(BlobProvider blobProvider) throws IOException {
        clearCache(((BlobStoreBlobProvider) blobProvider).store);
    }

    protected void clearCache(BlobStore blobStore) throws IOException {
        if (blobStore instanceof TransactionalBlobStore) {
            TransactionalBlobStore bs = (TransactionalBlobStore) blobStore;
            clearCache(bs.store);
            clearCache(bs.transientStore);
        } else if (blobStore instanceof CachingBlobStore) {
            CachingBlobStore bs = (CachingBlobStore) blobStore;
            bs.cacheStore.clear();
        }
    }

    protected void clearTrace() {
        logCaptureResult.clear();
    }

    protected void logTrace(String message) {
        LogEvent event = Log4jLogEvent.newBuilder() //
                                      .setMessage(new SimpleMessage(message))
                                      .build();
        logCaptureResult.getCaughtEvents().add(event);
    }

    protected void checkTrace(String filename) throws IOException {
        List<String> expectedTrace = LogTracingHelper.readTrace("traces/" + filename);
        List<String> actualTrace = logCaptureResult.getCaughtEventMessages();
        Map<String, String> context = new HashMap<>();
        try {
            LogTracingHelper.assertEqualsLists(expectedTrace, actualTrace, context);
        } catch (AssertionError e) {
            System.err.println(filename);
            System.err.println(String.join("\n", actualTrace));
            throw e;
        }
    }

    protected BlobInfo blobInfo(String key) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        return blobInfo;
    }

    protected static void assertKey(String expected, String actual) {
        // allow version to be present if bucket has versioning
        int seppos = expected.indexOf(KeyStrategy.VER_SEP);
        if (seppos >= 0) {
            expected = expected.substring(0, seppos);
        }
        if (!actual.startsWith(expected + '@')) {
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testWrite() throws IOException {
        BlobProvider bp = getBlobProvider("s3");

        logTrace("== Write ==");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        checkTrace("trace-write.txt");
    }

    @Test
    public void testWriteSubDirs() throws IOException {
        BlobProvider bp = getBlobProvider("s3-subdirs");

        logTrace("== Write (subdirs) ==");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        checkTrace("trace-write-subdirs.txt");
    }

    @Test
    public void testWriteAlreadyCached() throws IOException {
        BlobProvider bp = getBlobProvider("s3");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        bp.writeBlob(blobContext);
        clearTrace();

        logTrace("== Write (already cached) ==");
        String key1 = bp.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        checkTrace("trace-write-already-cached.txt");
    }

    @Test
    public void testWriteAlreadyStored() throws IOException {
        BlobProvider bp = getBlobProvider("s3");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        bp.writeBlob(blobContext);
        clearCache(bp);
        clearTrace();

        logTrace("== Write (already stored) ==");
        String key1 = bp.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        checkTrace("trace-write-already-stored.txt");
    }

    @Test
    public void testWriteNoCache() throws IOException {
        BlobProvider bp = getBlobProvider("s3-nocache");

        logTrace("== Write (no cache) ==");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        checkTrace("trace-write-nocache.txt");
    }

    @Test
    public void testWriteNoCacheAlreadyStored() throws IOException {
        BlobProvider bp = getBlobProvider("s3-nocache");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        bp.writeBlob(blobContext);
        clearTrace();

        logTrace("== Write (no cache, already stored) ==");
        String key1 = bp.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        checkTrace("trace-write-nocache-already-stored.txt");
    }

    @Test
    public void testWriteRecord() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");

        logTrace("== Write (record) ==");
        TransactionHelper.startTransaction();
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-write-record.txt");
    }

    /**
     * @since 2021.19
     */
    @Test
    public void testUpdateToColdStoreClass() throws IOException {
        BlobProvider bp = getBlobProvider("s3-coldStorage");

        logTrace("== Update to Cold Storage class ==");
        TransactionHelper.startTransaction();
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = bp.writeBlob(blobContext);
        BlobUpdateContext blobUpdateCtx = new BlobUpdateContext(blobInfo.key).withColdStorageClass(true);
        bp.updateBlob(blobUpdateCtx);
        TransactionHelper.commitOrRollbackTransaction();

        Blob blob = bp.readBlob(blobInfo);
        BlobStatus status = bp.getStatus((ManagedBlob) blob);
        assertFalse(status.isDownloadable());
        assertEquals(StorageClass.Glacier.toString(), status.getStorageClass());
        assertFalse(status.isOngoingRestore());
        checkTrace("trace-update-coldStorage.txt");
    }

    @Test
    public void testWriteRecordNoTransaction() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");

        logTrace("== Write (record, no transaction) ==");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        checkTrace("trace-write-record-notx.txt");
    }

    @Test
    public void testWriteRecordOverwrite() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        clearTrace();

        logTrace("== Write (record, overwrite) ==");
        TransactionHelper.startTransaction();
        BlobContext blobContext2 = new BlobContext(new StringBlob(BAR), DOCID1, XPATH);
        String key2 = bp.writeBlob(blobContext2);
        assertKey(DOCID1, key2);
        assertNotEquals(key1, key2);
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-write-record-overwrite.txt");
    }

    @Test
    public void testWriteRecordRollback() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");

        logTrace("== Write (record, rollback) ==");
        TransactionHelper.startTransaction();
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-write-record-rollback.txt");

        // check content missing
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals("", blob.getString()); // no crash, error in the log
    }

    @Test
    public void testReadMissing() throws IOException {
        BlobProvider bp = getBlobProvider("s3");

        logTrace("== Read (missing) ==");
        Blob blob = bp.readBlob(blobInfo(FOO_MD5));
        assertEquals("", blob.getString()); // no crash, error in the log
        checkTrace("trace-read-missing.txt");
    }

    @Test
    public void testRead() throws IOException {
        BlobProvider bp = getBlobProvider("s3");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        clearCache(bp);
        clearTrace();

        logTrace("== Read ==");
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals(FOO, blob.getString());
        checkTrace("trace-read.txt");
    }

    @Test
    public void testReadSubdirs() throws IOException {
        BlobProvider bp = getBlobProvider("s3-subdirs");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        clearCache(bp);
        clearTrace();

        logTrace("== Read (subdirs) ==");
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals(FOO, blob.getString());
        checkTrace("trace-read-subdirs.txt");
    }

    @Test
    public void testReadAlreadyCached() throws IOException {
        BlobProvider bp = getBlobProvider("s3");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        clearTrace();

        logTrace("== Read (already cached) ==");
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals(FOO, blob.getString());
        checkTrace("trace-read-already-cached.txt");
    }

    @Test
    public void testReadNoCacheMissing() throws IOException {
        BlobProvider bp = getBlobProvider("s3-nocache");

        logTrace("== Read (no cache, missing) ==");
        Blob blob = bp.readBlob(blobInfo(FOO_MD5));
        assertEquals("", blob.getString()); // no crash, error in the log
        checkTrace("trace-read-nocache-missing.txt");
    }

    @Test
    public void testReadNoCache() throws IOException {
        BlobProvider bp = getBlobProvider("s3-nocache");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        clearTrace();

        logTrace("== Read (no cache) ==");
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals(FOO, blob.getString());
        checkTrace("trace-read-nocache.txt");
    }

    @Test
    public void testReadRecordMissing() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");

        logTrace("== Read (record, missing) ==");
        TransactionHelper.startTransaction();
        Blob blob = bp.readBlob(blobInfo(DOCID1));
        assertEquals("", blob.getString()); // no crash, error in the log
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-read-record-missing.txt");
    }

    @Test
    public void testReadRecord() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        clearCache(bp);
        clearTrace();

        logTrace("== Read (record) ==");
        TransactionHelper.startTransaction();
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals(FOO, blob.getString());
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-read-record.txt");
    }

    @Test
    public void testReadRecordAlreadyCached() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        clearTrace();

        logTrace("== Read (record, already cached) ==");
        TransactionHelper.startTransaction();
        Blob blob = bp.readBlob(blobInfo(key1));
        assertEquals(FOO, blob.getString());
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-read-record-already-cached.txt");
    }

    @Test
    public void testCopy() throws IOException {
        BlobProvider bp1 = getBlobProvider("s3");
        BlobProvider bp2 = getBlobProvider("s3-other");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp1.writeBlob(blobContext);
        Blob blob1 = bp1.readBlob(blobInfo(key1));
        clearCache(bp1);
        clearTrace();

        logTrace("== Copy ==");
        BlobContext blobContext2 = new BlobContext(blob1, DOCID2, XPATH);
        String key2 = bp2.writeBlob(blobContext2);
        assertEquals(key1, key2);
        checkTrace("trace-copy.txt");

        // check content
        Blob blob2 = bp2.readBlob(blobInfo(key2));
        assertEquals(FOO, blob2.getString());
    }

    @Test
    public void testCopyToManaged() throws IOException {
        BlobProvider bp1 = getBlobProvider("s3");
        BlobStore bs1 = getBlobStore("s3");
        BlobProvider bp2 = getBlobProvider("s3-managed");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        // we need a blob with a key that's not a digest
        KeyStrategyDocId ksdocid = new KeyStrategyDocId();
        String key1 = bs1.writeBlob(ksdocid.getBlobWriteContext(blobContext));
        assertEquals(DOCID1, key1);
        Blob blob1 = bp1.readBlob(blobInfo(key1));
        clearCache(bp1);
        clearTrace();

        logTrace("== Copy (managed) ==");
        BlobContext blobContext2 = new BlobContext(blob1, DOCID2, XPATH);
        String key2 = bp2.writeBlob(blobContext2);
        assertEquals(key1, key2);
        checkTrace("trace-copy-managed.txt");

        // check content
        Blob blob2 = bp2.readBlob(blobInfo(key2));
        assertEquals(FOO, blob2.getString());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core:OSGI-INF/asyncdigest-listener-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-asyncdigest-delete-delay-contrib.xml")
    public void testCopyAsyncDigest() throws IOException, InterruptedException {
        // destination digest algorithm is not MD5, so async will be triggered
        BlobProvider bp1 = getBlobProvider("s3");
        BlobProvider bp2 = getBlobProvider("s3-sha256-async");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp1.writeBlob(blobContext);
        assertEquals(FOO_MD5, key1);
        Blob blob1 = bp1.readBlob(blobInfo(key1));
        clearCache(bp1);
        clearTrace();

        logTrace("== Copy (async) ==");
        TransactionHelper.startTransaction();
        // copy blob from blob provider bp1 to bp2
        BlobContext blobContext2 = new BlobContext(blob1, DOCID2, XPATH);
        String key2 = bp2.writeBlob(blobContext2);
        assertNotEquals(FOO_MD5, key2);
        assertNotEquals(FOO_SHA256, key2);
        assertTrue(key2, key2.contains("-")); // this is a pseudo-digest
        Blob blob2 = bp2.readBlob(blobInfo(key2));
        assertEquals(key2, ((ManagedBlob) blob2).getKey());
        assertEquals(key2, blob2.getDigest());
        assertNull(blob2.getDigestAlgorithm()); // not a real digest
        // write the new blob again to the same blob provider bp2
        String key3 = bp2.writeBlob(blob2);
        assertEquals(key2, key3); // same pseudo-digest is used

        logTrace("== Async ==");
        txFeature.nextTransaction(); // wait for work manager
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-copy-async.txt");

        // check content
        Blob blob3 = bp2.readBlob(blobInfo(FOO_SHA256));
        assertEquals(FOO, blob3.getString());
        assertEquals(FOO_SHA256, blob3.getDigest());
        assertEquals("SHA-256", blob3.getDigestAlgorithm());

        // check that old blob still resolves (concurrent usage case)
        Blob blob4 = bp2.readBlob(blobInfo(key2));
        assertEquals(FOO, blob4.getString());

        // check that writing the old blob immediately uses the new key
        String key4 = bp2.writeBlob(blob2);
        assertEquals(FOO_SHA256, key4);

        clearTrace();
        logTrace("== Copy (async delayed) ==");
        // wait for delay to elapse (1ms in test XML config)
        Thread.sleep(10);
        // manually force delete, this is usually done by a scheduled listener
        blobManager.deleteBlobsMarkedForDeletion();
        checkTrace("trace-copy-async-delayed.txt");
    }

    @Test
    public void testCopyRecord() throws IOException {
        BlobProvider bp = getBlobProvider("s3-record");
        BlobContext blobContext = new BlobContext(new StringBlob(FOO), DOCID1, XPATH);
        String key1 = bp.writeBlob(blobContext);
        assertKey(DOCID1, key1);
        Blob blob1 = bp.readBlob(blobInfo(key1));
        clearCache(bp);
        clearTrace();

        logTrace("== Copy (record) ==");
        TransactionHelper.startTransaction();
        BlobContext blobContext2 = new BlobContext(blob1, DOCID2, XPATH);
        String key2 = bp.writeBlob(blobContext2);
        assertKey(DOCID2, key2);
        TransactionHelper.commitOrRollbackTransaction();
        checkTrace("trace-copy-record.txt");

        // check content
        Blob blob2 = bp.readBlob(blobInfo(key2));
        assertEquals(FOO, blob2.getString());
    }

}
