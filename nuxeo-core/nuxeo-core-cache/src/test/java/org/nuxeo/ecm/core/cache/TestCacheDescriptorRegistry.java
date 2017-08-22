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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.cache.CacheServiceImpl.CacheDescriptorRegistry;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestCacheDescriptorRegistry {

    protected static final String NAME = "myid";

    @Test
    public void testMerge() {
        CacheDescriptorRegistry registry = new CacheServiceImpl.CacheDescriptorRegistry();
        // first contrib
        CacheDescriptor desc1 = new CacheDescriptor();
        desc1.name = NAME;
        desc1.ttl = Long.valueOf(123);
        desc1.options = new HashMap<>();
        desc1.options.put("foo", "foovalue");
        desc1.options.put("bar", "barvalue");
        registry.addContribution(desc1);
        // second controb
        CacheDescriptor desc2 = new CacheDescriptor();
        desc2.name = NAME;
        desc2.ttl = Long.valueOf(456);
        desc2.options = new HashMap<>();
        desc2.options.put("foo", "foovalue2");
        registry.addContribution(desc2);
        // check result of merge
        CacheDescriptor desc = registry.getCacheDescriptor(NAME);
        assertEquals(Long.valueOf(456), desc.ttl);
        HashMap<String, Serializable> map = new HashMap<>();
        map.put("foo", "foovalue2");
        map.put("bar", "barvalue");
        assertEquals(map, desc.options);
    }

    @Test
    public void testMergeRemove() {
        CacheDescriptorRegistry registry = new CacheServiceImpl.CacheDescriptorRegistry();
        // first contrib
        CacheDescriptor desc1 = new CacheDescriptor();
        desc1.name = NAME;
        desc1.ttl = Long.valueOf(123);
        registry.addContribution(desc1);
        // second contrib
        CacheDescriptor desc2 = new CacheDescriptor();
        desc2.name = NAME;
        desc2.remove = true;
        registry.addContribution(desc2);
        // check result of merge, contrib removed
        CacheDescriptor desc = registry.getCacheDescriptor(NAME);
        assertNull(desc);
        // add a new one after remove
        CacheDescriptor desc3 = new CacheDescriptor();
        desc3.name = NAME;
        desc3.options = new HashMap<>();
        desc3.options.put("foo", "foovalue");
        registry.addContribution(desc3);
        // check new one is visible
        desc = registry.getCacheDescriptor(NAME);
        assertNotNull(desc);
        assertNull(desc.ttl); // after a remove original contributions is forgotten
        assertEquals(Collections.singletonMap("foo", "foovalue"), desc.options);
    }

}
