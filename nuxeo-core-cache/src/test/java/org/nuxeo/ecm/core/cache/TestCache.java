package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.inject.Named;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ CacheFeature.class })
public class TestCache {

    @Inject @Named(CacheFeature.DEFAULT_TEST_CACHE_NAME) Cache defaultCache;

    @Test
    public void getValue() throws IOException {
        String cachedVal = (String) defaultCache.get(CacheFeature.KEY);
        Assert.assertEquals(CacheFeature.VAL, cachedVal);
    }

    @Test
    public void keyNotExist() throws IOException {
        Assert.assertNull(defaultCache.get("key-not-exist"));
    }

    @Test
    public void putUpdateGet() throws IOException {
        String val2 = "val2";
        defaultCache.put(CacheFeature.KEY, val2);
        val2 = (String) defaultCache.get(CacheFeature.KEY);
        Assert.assertEquals("val2", val2);
    }

    @Test
    public void putNullKey() throws IOException {
        try {
            defaultCache.put(null, "val-null");
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    public void putNullValue() throws Exception {
        try {
            defaultCache.put("key-null", null);
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    @Ignore
    public void ttlExpire() throws InterruptedException, IOException {
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) defaultCache.get(CacheFeature.KEY);
        Assert.assertNull(expiredVal);
    }

    @Test
    public void invalidateKey() throws IOException {
        Assert.assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.invalidate(CacheFeature.KEY);
        Assert.assertNull(defaultCache.get(CacheFeature.KEY));
    }

    @Test
    public void invalidateAll() throws IOException {
        Assert.assertNotNull(defaultCache.get(CacheFeature.KEY));
        defaultCache.put("key2", "val2");
        Assert.assertNotNull(defaultCache.get("key2"));
        defaultCache.invalidateAll();
        Assert.assertNull(defaultCache.get(CacheFeature.KEY));
        Assert.assertNull(defaultCache.get("key2"));
    }

}
