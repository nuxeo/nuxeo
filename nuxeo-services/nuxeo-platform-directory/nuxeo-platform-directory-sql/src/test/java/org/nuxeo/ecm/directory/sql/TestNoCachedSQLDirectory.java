package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class TestNoCachedSQLDirectory extends TestSQLDirectory {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");

        fireFrameworkStarted();
    }

    @Test
    public void testGetFromCache() throws DirectoryException, Exception {
        Session sqlSession = getSQLDirectory().getSession();

        MetricRegistry metrics = SharedMetricRegistries.getOrCreate(
                MetricsService.class.getName());
        Counter hitsCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", "userDirectory", "cache", "hits"));
        Counter missesCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", "userDirectory", "cache", "misses"));
        long baseHitsCount = hitsCounter.getCount();
        long baseMissesCount = missesCounter.getCount();

        // First call will miss the cache
        DocumentModel entry = sqlSession.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount, missesCounter.getCount());

        // Again
        entry = sqlSession.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount, missesCounter.getCount());

        // Again
        entry = sqlSession.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount, missesCounter.getCount());

        sqlSession.close();
    }

}