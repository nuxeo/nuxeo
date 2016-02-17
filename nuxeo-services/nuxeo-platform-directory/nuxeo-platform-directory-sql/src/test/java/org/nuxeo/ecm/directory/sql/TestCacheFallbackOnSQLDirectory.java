package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.cache.CacheAttributesChecker;
import org.nuxeo.ecm.core.cache.InMemoryCacheImpl;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class TestCacheFallbackOnSQLDirectory extends TestSQLDirectory {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");
        deployTestContrib("org.nuxeo.ecm.directory.sql.tests",
                "sql-directory-default-user-contrib.xml");

        fireFrameworkStarted();
    }

    @Test
    public void testGetFromCache() throws DirectoryException, Exception {
        Session sqlSession = getSQLDirectory().getSession();

        DirectoryCache cache = getSQLDirectory().getCache();
        assertNotNull(cache.getEntryCache());
        assertEquals("cache-" + getSQLDirectory().name,
                cache.getEntryCache().getName());
        assertEquals(InMemoryCacheImpl.class,
                ((CacheAttributesChecker) cache.getEntryCache()).getCache().getClass());
        assertNotNull(cache.getEntryCacheWithoutReferences());
        assertEquals("cacheWithoutReference-" + getSQLDirectory().name,
                cache.getEntryCacheWithoutReferences().getName());

        MetricRegistry metrics = SharedMetricRegistries.getOrCreate(
                MetricsService.class.getName());
        Counter hitsCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", "userDirectory", "cache", "hits"));
        Counter missesCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", "userDirectory", "cache", "misses"));
        long baseHitsCount = hitsCounter.getCount();
        long baseMissesCount = missesCounter.getCount();

        // First call will update cache
        DocumentModel entry = sqlSession.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount + 1, missesCounter.getCount());

        // Second call will use the cache
        entry = sqlSession.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount + 1, hitsCounter.getCount());
        assertEquals(baseMissesCount + 1, missesCounter.getCount());

        // Test if cache size (set to 1) is taken into account
        entry = sqlSession.getEntry("user_3");
        assertNotNull(entry);
        assertEquals(baseHitsCount + 1, hitsCounter.getCount());
        assertEquals(baseMissesCount + 2, missesCounter.getCount());

        entry = sqlSession.getEntry("user_3");
        assertNotNull(entry);
        assertEquals(baseHitsCount + 2, hitsCounter.getCount());
        assertEquals(baseMissesCount + 2, missesCounter.getCount());

        sqlSession.close();
    }
}