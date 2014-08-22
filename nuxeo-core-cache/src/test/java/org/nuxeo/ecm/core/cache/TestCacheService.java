/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mhilaire
 *
 * $Id: TestMultiDirectory.java 30378 2008-02-20 17:37:26Z gracinet $
 */

package org.nuxeo.ecm.core.cache;

import java.io.Serializable;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Maxime Hilaire
 *
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.core.cache" })
public class TestCacheService extends AbstractTestCache {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.core.cache.tests";

    @Before
    public void setUp() throws Exception {
        // Config for the tested bundle, first to set the implementation cache
        harness.deployContrib(TEST_BUNDLE, "OSGI-INF/cache-config.xml");
        // Call the abstract setUp once the specific contrib has been deployed
        super.setUp();
    }

    @Test
    public void testCast() {
        cache = ((CacheImpl) cacheService.getCache(DEFAULT_TEST_CACHE_NAME));
        Assert.assertNotNull(cache);
    }

    @Test
    public void getGuavaCache() {
        com.google.common.cache.Cache<String, Serializable> guavaCache = null;
        guavaCache = ((CacheImpl) cache).getCache();
        Assert.assertNotNull(guavaCache);
    }

    @Test
    public void maxSizeZero() {
        Cache maxSizeCache = cacheService.getCache(MAXSIZE_TEST_CACHE_NAME);
        maxSizeCache.put(key, val);
        Assert.assertNull(maxSizeCache.get(key));
    }

    @Test
    public void maxSizeExceeded() {
        // Default test config set to 3 the maxSize, and the cache already
        // contains the key1
        cache.put("key2", "val2");
        cache.put("key3", "val3");

        // Value inserted afterwards will remove the first inserted (size-based
        // eviction system)
        cache.put("key4", "val4");
        cache.put("key5", "val5");

        // Check that new values have been stored
        Assert.assertNotNull(cache.get("key4"));
        Assert.assertNotNull(cache.get("key5"));

        // Check that the oldest values have been evicted
        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));
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
