/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.common.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.common.file.LRUFileCache;

public class TestLRUFileCache {

    public File dir;

    @Before
    public void setUp() throws Exception {
        dir = File.createTempFile("nxtestlrufilecache.", "", new File(System.getProperty("java.io.tmpdir")));
        dir.delete();
        dir.mkdir();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(dir);
    }

    public long getDirSize() {
        long size = 0;
        for (File f : dir.listFiles()) {
            size += f.length();
        }
        return size;
    }

    @Test
    public void testLRUFileCache() throws Exception {
        LRUFileCache cache = new LRUFileCache(dir, 100, 9999, 1); // 100 bytes max
        cache.setClearOldEntriesIntervalMillis(0); // clear immediately
        assertEquals(0, cache.getSize());
        assertEquals(0, getDirSize());
        assertEquals(0, cache.getNumberOfItems());

        byte[] buf = new byte[30];

        cache.putFile("1", new ByteArrayInputStream(buf));
        assertEquals(1, cache.getNumberOfItems());
        assertEquals(30, cache.getSize());
        assertEquals(30, getDirSize());
        assertTrue(new File(dir, "1").exists());

        Thread.sleep(2000);
        cache.putFile("2", new ByteArrayInputStream(buf));
        assertEquals(2, cache.getNumberOfItems());
        assertEquals(60, cache.getSize());
        assertEquals(60, getDirSize());
        assertTrue(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());

        Thread.sleep(2000);
        cache.putFile("3", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertEquals(90, cache.getSize());
        assertEquals(90, getDirSize());
        assertTrue(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());
        assertTrue(new File(dir, "3").exists());

        Thread.sleep(2000);
        cache.putFile("4", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertEquals(90, cache.getSize());
        assertEquals(90, getDirSize());
        assertFalse(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());
        assertTrue(new File(dir, "3").exists());
        assertTrue(new File(dir, "4").exists());

        // store something bigger than the whole cache
        buf = new byte[150];

        Thread.sleep(2000);
        cache.putFile("5", new ByteArrayInputStream(buf));
        assertEquals(1, cache.getNumberOfItems());
        assertEquals(150, cache.getSize());
        assertEquals(150, getDirSize());
        assertFalse(new File(dir, "2").exists());
        assertFalse(new File(dir, "3").exists());
        assertFalse(new File(dir, "4").exists());
        assertTrue(new File(dir, "5").exists());

        // clear
        cache.clear();
        assertEquals(0, cache.getNumberOfItems());
        assertEquals(0, cache.getSize());
        assertEquals(0, getDirSize());
    }

    @Test
    public void testLRUFileCacheMaxCount() throws Exception {
        LRUFileCache cache = new LRUFileCache(dir, 10000, 3, 1); // 3 files max
        cache.setClearOldEntriesIntervalMillis(0); // clear immediately
        assertEquals(0, cache.getNumberOfItems());

        byte[] buf = new byte[30];

        cache.putFile("1", new ByteArrayInputStream(buf));
        assertEquals(1, cache.getNumberOfItems());
        assertTrue(new File(dir, "1").exists());

        Thread.sleep(1000);
        cache.putFile("2", new ByteArrayInputStream(buf));
        assertEquals(2, cache.getNumberOfItems());
        assertTrue(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());

        Thread.sleep(1000);
        cache.putFile("3", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertTrue(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());
        assertTrue(new File(dir, "3").exists());

        Thread.sleep(2000);
        cache.putFile("4", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertFalse(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());
        assertTrue(new File(dir, "3").exists());
        assertTrue(new File(dir, "4").exists());

        // clear
        cache.clear();
        assertEquals(0, cache.getNumberOfItems());
    }

    @Test
    public void testLRUFileCacheExternalCleanup() throws Exception {
        LRUFileCache cache = new LRUFileCache(dir, 100, 9999, 1); // 100 bytes max
        cache.setClearOldEntriesIntervalMillis(0); // clear immediately

        // create one file
        cache.putFile("1", new ByteArrayInputStream(new byte[30]));
        assertEquals(1, cache.getNumberOfItems());
        assertEquals(30, cache.getSize());
        assertEquals(30, getDirSize());
        assertTrue(new File(dir, "1").exists());

        // simulate external process doing cleanup
        FileUtils.deleteDirectory(dir);

        // we can still create entries without crashing
        cache.putFile("2", new ByteArrayInputStream(new byte[40]));
        // we only see the new one in stats
        assertEquals(1, cache.getNumberOfItems());
        assertEquals(40, cache.getSize());
        assertEquals(40, getDirSize());
        assertFalse(new File(dir, "1").exists());
        assertTrue(new File(dir, "2").exists());
    }

    @Test
    public void testLRUFileCacheKeyCheck() throws IOException {
        LRUFileCache cache = new LRUFileCache(dir, 100, 9999, 1);
        byte[] buf = new byte[30];
        // complex key allowed
        cache.putFile("10623fe9-1646-40f0-83d6-bda1ba43d305@Llxny6HoJ1_C8CJGJ1rLpRnpWJri4qAS",
                new ByteArrayInputStream(buf));
        // other characters forbidden
        try {
            cache.putFile("..", new ByteArrayInputStream(buf));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invalid key"));
        }
        try {
            cache.putFile("../myfile", new ByteArrayInputStream(buf));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invalid key"));
        }
        try {
            cache.putFile("foo/bar", new ByteArrayInputStream(buf));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invalid key"));
        }
    }

}
