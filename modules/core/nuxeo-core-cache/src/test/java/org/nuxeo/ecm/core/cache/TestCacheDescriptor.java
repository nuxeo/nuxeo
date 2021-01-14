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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.DEFAULT_MAX_SIZE;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.DEFAULT_TTL;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.OPTION_MAX_SIZE;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, ClusterFeature.class })
@Deploy("org.nuxeo.ecm.core.cache:OSGI-INF/CacheService.xml")
public class TestCacheDescriptor {

    protected static final String NAME = "myid";

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    protected CacheService service;

    protected CacheServiceImpl getService() {
        return (CacheServiceImpl) service;
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.cache.test:test-cache-config.xml")
    public void testMerge() throws Exception {
        CacheDescriptor desc = getService().getCacheDescriptor(NAME);
        assertNotNull(desc);
        assertEquals(NAME, desc.getName());
        assertEquals(123, desc.getTTL());
        assertEquals(Map.of("foo", "foovalue", "bar", "barvalue"), desc.getOptions());
        assertNotNull(getService().getCache(NAME));

        hotDeployer.deploy("org.nuxeo.ecm.core.cache.test:test-cache-config-merge.xml");

        // check result of merge
        CacheDescriptor mergedDesc = getService().getCacheDescriptor(NAME);
        assertNotNull(desc);
        assertEquals(456, mergedDesc.getTTL());
        assertEquals(Map.of("foo", "foovalue2", "bar", "barvalue"), mergedDesc.getOptions());
        assertNotNull(getService().getCache(NAME));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.cache.test:test-cache-config.xml")
    public void testRemove() throws Exception {
        assertNotNull(getService().getCacheDescriptor(NAME));
        assertNotNull(getService().getCache(NAME));

        hotDeployer.deploy("org.nuxeo.ecm.core.cache.test:test-cache-config-remove.xml");

        assertNull(getService().getCacheDescriptor(NAME));
        assertNull(getService().getCache(NAME));

        // add a new one after remove
        hotDeployer.deploy("org.nuxeo.ecm.core.cache.test:test-cache-config-merge.xml");

        CacheDescriptor desc = getService().getCacheDescriptor(NAME);
        assertNotNull(desc);
        assertEquals(456, desc.getTTL());
        assertEquals(Map.of("foo", "foovalue2"), desc.getOptions());
        assertNotNull(getService().getCache(NAME));
    }

    @Test
    public void testRegisterCache() {
        assertNull(getService().getCacheDescriptor(NAME));
        assertNull(getService().getCache(NAME));

        getService().registerCache(NAME);

        CacheDescriptor desc = getService().getCacheDescriptor(NAME);
        assertNotNull(desc);
        assertEquals(DEFAULT_TTL, desc.getTTL());
        assertEquals(Map.of(OPTION_MAX_SIZE, String.valueOf(DEFAULT_MAX_SIZE)), desc.getOptions());
        assertNotNull(getService().getCache(NAME));
    }

}
