/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.FulltextDescriptor.FulltextIndexDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;

public class TestRepositoryDescriptor {

    protected XMap xmap;

    protected RepositoryDescriptor desc;

    protected static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    @Before
    public void setUp() throws Exception {
        xmap = new XMap();
        xmap.register(RepositoryDescriptor.class);
        xmap.register(FulltextIndexDescriptor.class);
        desc = (RepositoryDescriptor) xmap.load(getResource("test-repository-descriptor.xml"));
    }

    @Test
    public void testBasic() throws Exception {
        assertEquals("foo", desc.name);
        assertEquals(1234, desc.getClusteringDelay());
    }

    @Test
    public void testBasicCopy() throws Exception {
        desc = new RepositoryDescriptor(desc);
        testBasic();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFulltext() throws Exception {
        assertEquals("french", desc.getFulltextAnalyzer());
        assertEquals("nuxeo", desc.getFulltextCatalog());
        FulltextDescriptor fulltextDescriptor = desc.getFulltextDescriptor();
        List<FulltextIndexDescriptor> fulltextIndexes = fulltextDescriptor.getFulltextIndexes();
        assertNotNull(fulltextIndexes);
        assertEquals(4, fulltextIndexes.size());
        FulltextIndexDescriptor fti;

        fti = fulltextIndexes.get(0);
        assertNull(fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet<>(), fti.fields);
        assertEquals(new HashSet<>(Collections.singleton("dc:creator")), fti.excludeFields);

        fti = fulltextIndexes.get(1);
        assertEquals("titraille", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet<>(Arrays.asList("dc:title", "dc:description")), fti.fields);
        assertEquals(new HashSet<>(), fti.excludeFields);

        fti = fulltextIndexes.get(2);
        assertEquals("blobs", fti.name);
        assertEquals("blob", fti.fieldType);
        assertEquals(new HashSet<>(), fti.fields);
        assertEquals(new HashSet<>(Collections.singleton("foo:bar")), fti.excludeFields);

        fti = fulltextIndexes.get(3);
        assertEquals("pictures", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet<>(Collections.singleton("picture:views/*/filename")), fti.fields);
        assertEquals(new HashSet<>(), fti.excludeFields);

        assertTrue(fulltextDescriptor.getFulltextExcludedTypes().contains("Folder"));
        assertTrue(fulltextDescriptor.getFulltextExcludedTypes().contains("Workspace"));
        assertTrue(fulltextDescriptor.getFulltextIncludedTypes().contains("File"));
        assertTrue(fulltextDescriptor.getFulltextIncludedTypes().contains("Note"));
    }

    @Test
    public void testFulltextCopy() throws Exception {
        desc = new RepositoryDescriptor(desc);
        testFulltext();
    }

    @Test
    public void testSchemaFields() throws Exception {
        assertNotNull(desc.schemaFields);
        assertEquals(3, desc.schemaFields.size());
        FieldDescriptor fd;
        fd = desc.schemaFields.get(0);
        assertEquals("my:bignote", fd.field);
        assertEquals("biig", fd.type);
        assertNull(fd.table);
        assertNull(fd.column);
        fd = desc.schemaFields.get(1);
        assertEquals("foo", fd.field);
        assertEquals("xyz", fd.type);
        assertNull(fd.table);
        assertNull(fd.column);
        fd = desc.schemaFields.get(2);
        assertEquals("bar", fd.field);
        assertEquals("bartype", fd.type);
        assertEquals("bartable", fd.table);
        assertEquals("barcol", fd.column);
    }

    @Test
    public void testSchemaFieldsCopy() throws Exception {
        desc = new RepositoryDescriptor(desc);
        testSchemaFields();
    }

    @Test
    public void testMerge() throws Exception {
        RepositoryDescriptor desc2 = (RepositoryDescriptor) xmap.load(getResource("test-repository-descriptor2.xml"));
        desc.merge(desc2);
        assertEquals(Arrays.asList("file1", "file2", "file3"), desc.sqlInitFiles);
        assertTrue(desc.getPathOptimizationsEnabled());
        assertEquals(2, desc.getPathOptimizationsVersion());

        // pool

        NuxeoConnectionManagerConfiguration pool = desc.pool;
        assertEquals(111, pool.getMinPoolSize());
        assertEquals(222, pool.getMaxPoolSize());
        assertEquals(3, pool.getBlockingTimeoutMillis());
        assertEquals(4, pool.getIdleTimeoutMinutes());

        // schema fields

        assertNotNull(desc.schemaFields);
        assertEquals(4, desc.schemaFields.size());
        FieldDescriptor fd;
        fd = desc.schemaFields.get(0);
        assertEquals("my:bignote", fd.field);
        assertEquals("other", fd.type);
        fd = desc.schemaFields.get(1);
        assertEquals("foo", fd.field);
        assertEquals("xyz", fd.type);
        fd = desc.schemaFields.get(2);
        assertEquals("bar", fd.field);
        assertEquals("bartype2", fd.type);
        assertEquals("bartable2", fd.table);
        assertEquals("barcol2", fd.column);
        fd = desc.schemaFields.get(3);
        assertEquals("def", fd.field);
        assertEquals("abc", fd.type);

        // fulltext indexes

        assertEquals("english", desc.getFulltextAnalyzer());
        assertEquals("nuxeo", desc.getFulltextCatalog());
        FulltextDescriptor fulltextDescriptor = desc.getFulltextDescriptor();
        List<FulltextIndexDescriptor> fulltextIndexes = fulltextDescriptor.getFulltextIndexes();
        assertEquals(5, fulltextIndexes.size());
        FulltextIndexDescriptor fti;

        fti = fulltextIndexes.get(0);
        assertNull(fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet<String>(), fti.fields);
        assertEquals(Collections.singleton("dc:creator"), fti.excludeFields);

        fti = fulltextIndexes.get(1);
        assertEquals("titraille", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet<>(Arrays.asList("dc:title", "dc:description", "my:desc")), fti.fields);
        assertEquals(new HashSet<String>(), fti.excludeFields);

        fti = fulltextIndexes.get(2);
        assertEquals("blobs", fti.name);
        assertEquals("blob", fti.fieldType);
        assertEquals(new HashSet<String>(), fti.fields);
        assertEquals(Collections.singleton("foo:bar"), fti.excludeFields);

        fti = fulltextIndexes.get(3);
        assertEquals("pictures", fti.name);
        assertNull(fti.fieldType);
        assertEquals(Collections.singleton("picture:views/*/filename"), fti.fields);
        assertEquals(new HashSet<String>(), fti.excludeFields);

        fti = fulltextIndexes.get(4);
        assertEquals("other", fti.name);
        assertNull(fti.fieldType);
        assertEquals(Collections.singleton("my:other"), fti.fields);
        assertEquals(new HashSet<String>(), fti.excludeFields);

        assertEquals(new HashSet<>(Arrays.asList("Folder", "Workspace", "OtherExcluded")),
                fulltextDescriptor.getFulltextExcludedTypes());
        assertEquals(new HashSet<>(Arrays.asList("Note", "File", "OtherIncluded")),
                fulltextDescriptor.getFulltextIncludedTypes());
    }

}
