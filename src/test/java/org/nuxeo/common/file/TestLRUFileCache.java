/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import junit.framework.TestCase;

import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.common.utils.FileUtils;

public class TestLRUFileCache extends TestCase {

    public File dir;

    @Override
    public void setUp() throws Exception {
        dir = File.createTempFile("nxtestlrufilecache.", "");
        dir.delete();
        dir.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        FileUtils.deleteTree(dir);
    }

    public long getDirSize() {
        long size = 0;
        for (File f : dir.listFiles()) {
            size += f.length();
        }
        return size;
    }

    public void testLRUFileCache() throws Exception {
        LRUFileCache cache = new LRUFileCache(dir, 100);
        assertEquals(0, cache.getSize());
        assertEquals(0, getDirSize());
        assertEquals(0, cache.getNumberOfItems());

        byte[] buf = new byte[30];
        cache.putFile("1", new ByteArrayInputStream(buf));
        assertEquals(1, cache.getNumberOfItems());
        assertEquals(30, cache.getSize());
        assertEquals(30, getDirSize());
        File[] held = new File[1];
        held[0] = cache.putFile("2", new ByteArrayInputStream(buf));
        assertEquals(2, cache.getNumberOfItems());
        assertEquals(60, cache.getSize());
        assertEquals(60, getDirSize());
        cache.putFile("3", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertEquals(90, cache.getSize());
        assertEquals(90, getDirSize());
        cache.putFile("4", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertEquals(90, cache.getSize());
        System.gc();
        Thread.sleep(1000);
        assertEquals(90, getDirSize());
        assertFalse(new File(dir, "1").exists());

        // again while keep the "2" referenced
        cache.putFile("5", new ByteArrayInputStream(buf));
        assertEquals(3, cache.getNumberOfItems());
        assertEquals(90, cache.getSize());
        System.gc();
        Thread.sleep(1000);
        assertEquals(120, getDirSize()); // one file non deleted
        assertTrue(new File(dir, "2").exists());
        // clear reference
        held[0] = null;
        System.gc();
        Thread.sleep(1000);
        assertEquals(90, getDirSize());
        assertFalse(new File(dir, "2").exists());

        // store something bigger than the whole cache
        buf = new byte[150];
        cache.putFile("6", new ByteArrayInputStream(buf));
        assertEquals(1, cache.getNumberOfItems());
        assertEquals(150, cache.getSize());
        System.gc();
        Thread.sleep(1000);
        assertEquals(150, getDirSize());
        assertFalse(new File(dir, "5").exists());
        assertTrue(new File(dir, "6").exists());

        // clear
        cache.clear();
        assertEquals(0, cache.getNumberOfItems());
        assertEquals(0, cache.getSize());
        System.gc();
        Thread.sleep(1000);
        assertEquals(0, getDirSize());
    }

}
