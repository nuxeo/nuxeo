/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.model.DescriptorRegistry;

public class TestDescriptorRegistry {

    public static class TestDescriptor implements Descriptor {

        public String id;

        public String name;

        public String desc;

        public TestDescriptor() {
            // do nothing
        }

        public TestDescriptor(String id, String name, String desc) {
            super();
            this.id = id;
            this.name = name;
            this.desc = desc;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Descriptor merge(Descriptor o) {
            TestDescriptor other = (TestDescriptor) o;
            TestDescriptor merged = new TestDescriptor();
            merged.id = other.id != null ? other.id : id;
            merged.name = other.name != null ? other.name : name;
            merged.desc = other.desc != null ? other.desc : desc;
            return merged;
        }

    }

    private static final String EP = "ep1";

    private static final String TARGET = "target1";

    @Test
    public void testGetDescriptors() {
        DescriptorRegistry registry = new DescriptorRegistry();
        registry.register(TARGET, EP, new TestDescriptor("id1", "name1", "desc1"));
        registry.register(TARGET, EP, new TestDescriptor("id1", "name11", "desc1"));
        registry.register(TARGET, EP, new TestDescriptor("id1", null, "desc11"));
        registry.register(TARGET, EP, new TestDescriptor("id2", "name2", "desc2"));
        registry.register(TARGET, EP, new TestDescriptor("id2", "name22", "desc2"));
        registry.register(TARGET, EP, new TestDescriptor("id2", null, "desc22"));
        List<TestDescriptor> descs = registry.getDescriptors(TARGET, EP);
        assertEquals(2, descs.size());
        assertValues(descs.get(0), "id1", "name11", "desc11");
        assertValues(descs.get(1), "id2", "name22", "desc22");

    }

    @Test
    public void testGetSingleDescriptor() {
        DescriptorRegistry registry = new DescriptorRegistry();
        registry.register(TARGET, EP, new TestDescriptor("id1", "name1", "desc1"));
        assertValues((TestDescriptor) registry.getDescriptor(TARGET, EP, "id1"), "id1", "name1", "desc1");
        registry.register(TARGET, EP, new TestDescriptor("id1", "name2", "desc1"));
        assertValues((TestDescriptor) registry.getDescriptor(TARGET, EP, "id1"), "id1", "name2", "desc1");
        registry.register(TARGET, EP, new TestDescriptor("id1", null, "desc2"));
        assertValues((TestDescriptor) registry.getDescriptor(TARGET, EP, "id1"), "id1", "name2", "desc2");
    }

    @Test
    public void testRemove() {
        DescriptorRegistry registry = new DescriptorRegistry();
        registry.register(TARGET, EP, new TestDescriptor("id0", "", ""));
        assertValues(registry.getDescriptor(TARGET, EP, "id0"), "id0", "", "");
        registry.register(TARGET, EP, new TestDescriptor("id0", "merged", "merged"));
        assertValues(registry.getDescriptor(TARGET, EP, "id0"), "id0", "merged", "merged");
        registry.register(TARGET, EP, new TestDescriptor("id0", "", "") {
            @Override
            public boolean doesRemove() {
                return true;
            }
        });
        assertNull(registry.getDescriptor(TARGET, EP, "id0"));
        registry.register(TARGET, EP, new TestDescriptor("id0", "final", null));
        assertValues(registry.getDescriptor(TARGET, EP, "id0"), "id0", "final", null);
    }

    protected void assertValues(TestDescriptor d, String id, String name, String desc) {
        assertEquals(id, d.id);
        assertEquals(name, d.name);
        assertEquals(desc, d.desc);
    }

}
