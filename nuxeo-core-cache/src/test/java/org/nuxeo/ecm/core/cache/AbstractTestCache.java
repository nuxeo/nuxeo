package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.fail;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.core.cache" })
public class AbstractTestCache {
    @Inject
    protected CacheService cacheService;

    @Inject
    protected EventService eventService;

    @Inject
    protected RuntimeHarness harness;

    protected CacheAttributesChecker cacheChecker;

    protected static String DEFAULT_TEST_CACHE_NAME = "default-test-cache";

    protected static String MAXSIZE_TEST_CACHE_NAME = "maxsize-test-cache";

    protected String key = "key1";

    protected String val = "val1";

    @Before
    public void setUp() throws Exception {
        cacheChecker = cacheService.getCache(DEFAULT_TEST_CACHE_NAME);
        Assert.assertNotNull(cacheChecker);
        cacheChecker.put(key, val);
    }

    @After
    public void tearDown() throws IOException {
        if (cacheChecker == null) {
            return;
        }
        cacheChecker.invalidateAll();
    }

    @Test
    public void getValue() throws IOException {
        String cachedVal = (String) cacheChecker.get(key);
        Assert.assertEquals(val, cachedVal);
    }

    @Test
    public void keyNotExist() throws IOException {
        Assert.assertNull(cacheChecker.get("key-not-exist"));
    }

    @Test
    public void putUpdateGet() throws IOException {
        String val2 = "val2";
        cacheChecker.put(key, val2);
        val2 = (String) cacheChecker.get(key);
        Assert.assertEquals("val2", val2);
    }

    @Test
    public void putNullKey() throws IOException {
        try {
            cacheChecker.put(null, "val-null");
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    public void putNullValue() throws Exception {
        try {
            cacheChecker.put("key-null", null);
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    public void ttlExpire() throws InterruptedException, IOException {
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) cacheChecker.get(key);
        Assert.assertNull(expiredVal);
    }

    @Test
    public void invalidateKey() throws IOException {
        Assert.assertNotNull(cacheChecker.get(key));
        cacheChecker.invalidate(key);
        Assert.assertNull(cacheChecker.get(key));
    }

    @Test
    public void invalidateAll() throws IOException {
        Assert.assertNotNull(cacheChecker.get(key));
        cacheChecker.put("key2", "val2");
        Assert.assertNotNull(cacheChecker.get("key2"));
        cacheChecker.invalidateAll();
        Assert.assertNull(cacheChecker.get(key));
        Assert.assertNull(cacheChecker.get("key2"));
    }

}
