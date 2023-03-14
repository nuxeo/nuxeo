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
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Tests the pure caching aspects of the CachingBlobStore.
 *
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
public class TestCachingBlobStoreCaching {

    protected static final String XPATH = "content";

    protected static final Blob BLOB_30 = new ByteArrayBlob(new byte[30]);

    protected static final Blob BLOB_150 = new ByteArrayBlob(new byte[150]);

    protected Path dir;

    protected PathStrategy defaultPathStrategy;

    protected MutableClock clock;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("testcachingblobstore.");
        defaultPathStrategy = new PathStrategyShortened(dir);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(dir.toFile());
    }

    protected CachingBlobStore getStore(long maxSize, long maxCount, long minAge) {
        BlobStore emptyStore = new EmptyBlobStore("empty", "empty", KeyStrategyDocId.instance());
        return getStore(emptyStore, dir, "test", maxSize, maxCount, minAge);
    }

    protected CachingBlobStore getStore(BlobStore target, Path directory, String name, long maxSize, long maxCount,
            long minAge) {
        CachingConfiguration config = new CachingConfiguration(directory, maxSize, maxCount, minAge);
        CachingBlobStore store = new CachingBlobStore(name, name, target, config);
        store.clearOldBlobsInterval = 0; // clear immediately
        clock = new MutableClock();
        store.clock = clock;
        return store;
    }

    protected void advanceClock(long seconds) {
        clock.add(Duration.ofSeconds(seconds));
    }

    protected long getDirCount() {
        return dir.toFile().listFiles().length;
    }

    protected long getDirSize() {
        long size = 0;
        for (File f : dir.toFile().listFiles()) {
            size += f.length();
        }
        return size;
    }

    protected boolean exists(String key) {
        // use default path strategy
        return Files.exists(defaultPathStrategy.getPathForKey(key));
    }

    @Test
    public void testCachingBlobStoreMaxSize() throws IOException {
        CachingBlobStore store = getStore(100, 9999, 1); // 100 bytes max
        assertEquals(0, getDirCount());
        assertEquals(0, getDirSize());
        String key;

        key = store.writeBlob(new BlobContext(BLOB_30, "1", XPATH));
        assertEquals("1", key);
        assertTrue(exists("1"));
        assertEquals(1, getDirCount());
        assertEquals(30, getDirSize());

        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_30, "2", XPATH));
        assertEquals("2", key);
        assertTrue(exists("1"));
        assertTrue(exists("2"));
        assertEquals(2, getDirCount());
        assertEquals(60, getDirSize());

        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_30, "3", XPATH));
        assertEquals("3", key);
        assertTrue(exists("1"));
        assertTrue(exists("2"));
        assertTrue(exists("3"));
        assertEquals(3, getDirCount());
        assertEquals(90, getDirSize());

        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_30, "4", XPATH));
        assertEquals("4", key);
        assertFalse(exists("1"));
        assertTrue(exists("2"));
        assertTrue(exists("3"));
        assertTrue(exists("4"));
        assertEquals(3, getDirCount());
        assertEquals(90, getDirSize());

        // store something bigger than the whole cache
        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_150, "5", XPATH));
        assertEquals("5", key);
        assertFalse(exists("2"));
        assertFalse(exists("3"));
        assertFalse(exists("4"));
        assertTrue(exists("5"));
        assertEquals(1, getDirCount());
        assertEquals(150, getDirSize());

        // clear
        store.clear();
        assertEquals(0, getDirCount());
        assertEquals(0, getDirSize());
    }

    @Test
    public void testCachingBlobStoreMaxCount() throws IOException {
        CachingBlobStore store = getStore(1000, 3, 1); // 3 files max
        assertEquals(0, getDirCount());
        assertEquals(0, getDirSize());
        String key;

        key = store.writeBlob(new BlobContext(BLOB_30, "1", XPATH));
        assertEquals("1", key);
        assertTrue(exists("1"));
        assertEquals(1, getDirCount());

        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_30, "2", XPATH));
        assertEquals("2", key);
        assertTrue(exists("1"));
        assertTrue(exists("2"));
        assertEquals(2, getDirCount());

        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_30, "3", XPATH));
        assertEquals("3", key);
        assertTrue(exists("1"));
        assertTrue(exists("2"));
        assertTrue(exists("3"));
        assertEquals(3, getDirCount());

        advanceClock(2);
        key = store.writeBlob(new BlobContext(BLOB_30, "4", XPATH));
        assertEquals("4", key);
        assertFalse(exists("1"));
        assertTrue(exists("2"));
        assertTrue(exists("3"));
        assertTrue(exists("4"));
        assertEquals(3, getDirCount());
    }

    @Test
    public void testCachingBlobStoreMinAge() throws IOException {
        CachingBlobStore store = getStore(1000, 1, 10); // 10s min age, 1 file max
        assertEquals(0, getDirCount());
        assertEquals(0, getDirSize());
        String key;

        key = store.writeBlob(new BlobContext(BLOB_30, "1", XPATH));
        assertEquals("1", key);
        assertTrue(exists("1"));
        assertEquals(1, getDirCount());

        advanceClock(5);
        key = store.writeBlob(new BlobContext(BLOB_30, "2", XPATH));
        assertEquals("2", key);
        assertTrue(exists("1"));
        assertTrue(exists("2"));
        assertEquals(2, getDirCount());

        advanceClock(7); // advance beyond min age for first file
        key = store.writeBlob(new BlobContext(BLOB_30, "3", XPATH));
        assertEquals("3", key);
        assertFalse(exists("1"));
        assertTrue(exists("2"));
        assertTrue(exists("3"));
        assertEquals(2, getDirCount());
    }

    @Test
    public void testCachingBlobStoreKey() throws IOException {
        CachingBlobStore store = getStore(100, 9999, 1);
        String id;
        String key;

        // complex key allowed
        id = "10623fe9-1646-40f0-83d6-bda1ba43d305@Llxny6HoJ1_C8CJGJ1rLpRnpWJri4qAS";
        key = store.writeBlob(new BlobContext(BLOB_30, id, XPATH));
        assertEquals(id, key);

        // key with slash allowed
        id = "foo/bar/baz";
        key = store.writeBlob(new BlobContext(BLOB_30, id, XPATH));
        assertEquals(id, key);
    }

    // NXP-31730
    @Test
    public void testCachingBlobStoreInvalidateOnDelete() throws IOException {
        // Create 2 caching blob store sharing the same final store
        BlobStore inMemory = new InMemoryBlobStore("inMemory", new KeyStrategyDigest("MD5"), false, true);
        Path dir1 = Files.createTempDirectory(dir, "first.");
        Path dir2 = Files.createTempDirectory(dir, "second.");
        CachingBlobStore firstStore = getStore(inMemory, dir1, "test1", 100, 100, 60);
        CachingBlobStore secondStore = getStore(inMemory, dir2, "test2", 100, 100, 60);

        // Use first caching store to create a blob
        var key = firstStore.writeBlob(new BlobContext(BLOB_30, "foo", XPATH));
        assertTrue(firstStore.getFile(key).isPresent());
        // 2nd caching store does not know the blob yet
        assertTrue(secondStore.getFile(key).isUnknown());
        // Force load the blob into 2nd caching store
        secondStore.getStream(key);
        assertTrue(secondStore.getFile(key).isPresent());

        // delete the blob from the 1st caching blob and thus the final store
        firstStore.deleteBlob(key);
        assertFalse(firstStore.exists(key));
        assertFalse(secondStore.exists(key));

        // the blob has been effectively deleted from the final store
        assertTrue(inMemory.getFile(key).isMissing());
        // but still in the 2nd caching store
        assertTrue(secondStore.getFile(key).isPresent());
        // writing to the 2nd caching store also writes to final store
        secondStore.writeBlob(new BlobContext(BLOB_30, "bar", XPATH));
        assertTrue(inMemory.getStream(key).isPresent());
        assertTrue(firstStore.exists(key));
        assertTrue(secondStore.exists(key));
    }

    protected static class MutableClock extends Clock {

        protected Instant instant;

        protected final ZoneId zone;

        public MutableClock() {
            this(Instant.now(), ZoneId.systemDefault());
        }

        public MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void set(Instant instant) {
            this.instant = instant;
        }

        public void add(TemporalAmount amountToAdd) {
            instant = instant.plus(amountToAdd);
        }
    }

}
