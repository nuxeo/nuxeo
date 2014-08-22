package org.nuxeo.ecm.core.cache;

import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
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
        String cachedVal = (String) cache.get(key);
        Assert.assertEquals(val, cachedVal);
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
        // The aim of the test here is to make sure no exception is raised
        cache.put(null, "val-null");
        Assert.assertNull(cache.get(null));
    }

    @Test
    public void putNullValue() {
        // The aim of the test here is to make sure no exception is raised
        cache.put("key-null", null);
        Assert.assertNull(cache.get("key-null"));
    }

    @Test
    public void ttlExpire() throws InterruptedException {
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) cache.get(key);
        Assert.assertNull(expiredVal);
    }

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
        Assert.assertNotNull(cache.get(key));
        Event event = new Event(Cache.CACHE_TOPIC, CacheService.INVALIDATE_ALL,
                this, null);
        eventService.sendEvent(event);
        Assert.assertNull(cache.get(key));
    }

}
