/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.core.cache;

import java.io.IOException;
import java.io.Serializable;

import junit.framework.Assert;

import org.junit.Test;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Maxime Hilaire
 *
 */

@LocalDeploy("org.nuxeo.ecm.core.cache:inmemory-cache-config.xml")
public class TestInMemoryCacheService extends AbstractTestCache {

    @Test
    public void testGetCache() {
        cacheChecker = (cacheService.getCache(DEFAULT_TEST_CACHE_NAME));
        Assert.assertNotNull(cacheChecker);
    }

    @Test
    public void getGuavaCache() {
        com.google.common.cache.Cache<String, Serializable> guavaCache = null;
        guavaCache = ((InMemoryCacheImpl) cacheChecker.getCache()).getGuavaCache();
        Assert.assertNotNull(guavaCache);
    }

    @Test
    public void maxSizeZero() throws IOException {
        Cache maxSizeCache = cacheService.getCache(MAXSIZE_TEST_CACHE_NAME);
        maxSizeCache.put(key, val);
        Assert.assertNull(maxSizeCache.get(key));
    }

    @Test
    public void maxSizeExceeded() throws IOException {
        // Default test config set to 3 the maxSize, and the cache already
        // contains the key1
        cacheChecker.put("key2", "val2");
        cacheChecker.put("key3", "val3");

        // Value inserted afterwards will remove the first inserted (size-based
        // eviction system)
        cacheChecker.put("key4", "val4");
        cacheChecker.put("key5", "val5");

        // Check that new values have been stored
        Assert.assertNotNull(cacheChecker.get("key4"));
        Assert.assertNotNull(cacheChecker.get("key5"));

        // Check that the oldest values have been evicted
        Assert.assertNull(cacheChecker.get("key1"));
        Assert.assertNull(cacheChecker.get("key2"));
    }

    // protected class TestCacheConcurrency implements Runnable
    // {
    // protected boolean oneThreadDone = false;
    //
    // @Override
    // public void run() {
    //
    // Random rand = new Random();
    //
    // int id = rand.nextInt(100) + 1;
    //
    // Cache currentCache = cacheService.getCache(DEFAULT_TEST_CACHE_NAME);
    // currentCache.put("key-concurrent-"+id,"val-"+id);
    // System.out.println(id);
    // //int sec = rand.nextInt((max - min) + 1) + min;
    // //Thread.sleep((long)(Math.random() * 1000));
    // String value = (String) currentCache.get("key-concurrent-"+id);
    // synchronized (this) {
    // if(!oneThreadDone)
    // {
    // Assert.assertNotNull(value);
    // }else
    // {
    // Assert.assertNull(value);
    // }
    // }
    // }
    //
    // }
    // @Test
    // public void concurrencyExceeded() {
    // //Default config set to only one thread the concurrency level
    // ExecutorService es = Executors.newFixedThreadPool(10);
    // es.execute(new TestCacheConcurrency());
    // es.shutdown();
    // while (!es.isTerminated()) {
    // }
    //
    // }

}
