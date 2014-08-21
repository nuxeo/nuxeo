package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.fail;

import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

public class AbstractTestCache {
    @Inject
    protected CacheService cacheService;

    @Inject
    protected EventService eventService;

    @Inject
    protected RuntimeHarness harness;

    protected Cache cache;

    protected static String DEFAULT_TEST_CACHE_NAME = "default-test-cache";

    protected static String MAXSIZE_TEST_CACHE_NAME = "maxsize-test-cache";

    protected String key = "key1";

    protected String val = "val1";

    @Before
    public void setUp() throws Exception {
        cache = cacheService.getCache(DEFAULT_TEST_CACHE_NAME);
        cache.put(key, val);
    }

    @Test
    public void getValue() {
        String key = "key1";
        String val = (String) cache.get(key);

        Assert.assertEquals("val1", val);
    }

    @Test
    public void keyNotExist() {
        Assert.assertNull(cache.get("key-not-exist"));
    }

    @Test
    public void putUpdateGet() {
        String val2 = "val2";
        cache.put(key, val2);
        val2 = (String) cache.get(key);
        Assert.assertEquals("val2", val2);
    }

    @Test
    public void putNullKey() {
        try {
            cache.put(null, "val-null");
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    public void putNullValue() {
        try {
            cache.put(null, "val-null");
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    public void ttlExpire() throws InterruptedException {
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) cache.get(key);
        Assert.assertNull(expiredVal);
    }

    @Test
    public void maxSizeZero() {
        Cache maxSizeCache = cacheService.getCache(MAXSIZE_TEST_CACHE_NAME);
        maxSizeCache.put(key, val);
        Assert.assertNull(maxSizeCache.get(key));
    }

    @Test
    // TODO : check the behavior with redis
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

    @Test
    public void invalidateKey() {
        Assert.assertNotNull(cache.get(key));
        cache.invalidate(key);
        Assert.assertNull(cache.get(key));
    }

    @Test
    public void invalidateAll() {
        Assert.assertNotNull(cache.get(key));
        cache.put("key2", "val2");
        Assert.assertNotNull(cache.get("key2"));
        cache.invalidateAll();
        Assert.assertNull(cache.get(key));
        Assert.assertNull(cache.get("key2"));
    }

    @Test
    public void fireInvalidateAllEvent() throws LoginException {
        Event event = new Event(Cache.CACHE_TOPIC, CacheService.INVALIDATE_ALL,
                this, null);
        eventService.sendEvent(event);
        Assert.assertNull(cache.get(key));
    }

}
