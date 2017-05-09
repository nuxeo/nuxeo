/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@LocalDeploy({ "org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.tests:test-directories-bundle.xml" })
public class TestNoCachedDirectory {

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testGetWithNoCache() throws Exception {

        Directory dir = directoryService.getDirectory("userDirectory");

        Session session = dir.getSession();

        MetricRegistry metrics = SharedMetricRegistries.getOrCreate(
                MetricsService.class.getName());
        Counter hitsCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", "userDirectory", "cache", "hits"));
        Counter missesCounter = metrics.counter(MetricRegistry.name("nuxeo",
                "directories", "userDirectory", "cache", "misses"));
        long baseHitsCount = hitsCounter.getCount();
        long baseMissesCount = missesCounter.getCount();

        // Each call does not use the cache, because it is disabled
        DocumentModel entry = session.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount, missesCounter.getCount());

        // Again
        entry = session.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount, missesCounter.getCount());

        // Again
        entry = session.getEntry("user_1");
        assertNotNull(entry);
        assertEquals(baseHitsCount, hitsCounter.getCount());
        assertEquals(baseMissesCount, missesCounter.getCount());

        session.close();
    }

}