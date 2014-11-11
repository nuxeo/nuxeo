/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FulltextIndexDescriptor;

public class TestRepositoryDescriptor {

    protected XMap xmap;

    protected RepositoryDescriptor desc;

    protected static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
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
        assertTrue(desc.getClusteringEnabled());
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
        assertEquals("french", desc.fulltextAnalyzer);
        assertEquals("nuxeo", desc.fulltextCatalog);
        assertNotNull(desc.fulltextIndexes);
        assertEquals(4, desc.fulltextIndexes.size());
        FulltextIndexDescriptor fti;

        fti = desc.fulltextIndexes.get(0);
        assertNull(fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(), fti.fields);
        assertEquals(new HashSet(Collections.singleton("dc:creator")),
                fti.excludeFields);

        fti = desc.fulltextIndexes.get(1);
        assertEquals("titraille", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(Arrays.asList("dc:title", "dc:description")),
                fti.fields);
        assertEquals(new HashSet(), fti.excludeFields);

        fti = desc.fulltextIndexes.get(2);
        assertEquals("blobs", fti.name);
        assertEquals("blob", fti.fieldType);
        assertEquals(new HashSet(), fti.fields);
        assertEquals(new HashSet(Collections.singleton("foo:bar")),
                fti.excludeFields);

        fti = desc.fulltextIndexes.get(3);
        assertEquals("pictures", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(
                Collections.singleton("picture:views/*/filename")), fti.fields);
        assertEquals(new HashSet(), fti.excludeFields);
        assertEquals("martian", fti.analyzer);
        assertEquals("other", fti.catalog);

        assertTrue(desc.fulltextExcludedTypes.contains("Folder"));
        assertTrue(desc.fulltextExcludedTypes.contains("Workspace"));
        assertTrue(desc.fulltextIncludedTypes.contains("File"));
        assertTrue(desc.fulltextIncludedTypes.contains("Note"));
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
    public void testBinaryStorePath() throws Exception {
        assertEquals("/foo/bar", desc.binaryStorePath);
    }

    @Test
    public void testBinaryStorePathCopy() throws Exception {
        desc = new RepositoryDescriptor(desc);
        testBinaryStorePath();
    }

    @Test
    public void testMerge() throws Exception {
        RepositoryDescriptor desc2 = (RepositoryDescriptor) xmap.load(getResource("test-repository-descriptor2.xml"));
        desc.merge(desc2);
        assertEquals("/foo/bar2", desc.binaryStorePath);
        assertFalse(desc.getClusteringEnabled());
        assertEquals(Arrays.asList("file1", "file2", "file3"),
                desc.sqlInitFiles);
        assertTrue(desc.getPathOptimizationsEnabled());
        assertEquals(2, desc.getPathOptimizationsVersion());

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

        assertEquals("english", desc.fulltextAnalyzer);
        assertEquals("nuxeo", desc.fulltextCatalog);
        assertEquals(5, desc.fulltextIndexes.size());
        FulltextIndexDescriptor fti;

        fti = desc.fulltextIndexes.get(0);
        assertNull(fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet<String>(), fti.fields);
        assertEquals(Collections.singleton("dc:creator"), fti.excludeFields);

        fti = desc.fulltextIndexes.get(1);
        assertEquals("titraille", fti.name);
        assertNull(fti.fieldType);
        assertEquals(
                new HashSet<String>(Arrays.asList("dc:title", "dc:description",
                        "my:desc")), fti.fields);
        assertEquals(new HashSet<String>(), fti.excludeFields);

        fti = desc.fulltextIndexes.get(2);
        assertEquals("blobs", fti.name);
        assertEquals("blob", fti.fieldType);
        assertEquals(new HashSet<String>(), fti.fields);
        assertEquals(Collections.singleton("foo:bar"), fti.excludeFields);

        fti = desc.fulltextIndexes.get(3);
        assertEquals("pictures", fti.name);
        assertNull(fti.fieldType);
        assertEquals(Collections.singleton("picture:views/*/filename"),
                fti.fields);
        assertEquals(new HashSet<String>(), fti.excludeFields);
        assertEquals("venusian", fti.analyzer);
        assertEquals("other", fti.catalog);

        fti = desc.fulltextIndexes.get(4);
        assertEquals("other", fti.name);
        assertNull(fti.fieldType);
        assertEquals(Collections.singleton("my:other"), fti.fields);
        assertEquals(new HashSet<String>(), fti.excludeFields);

        assertEquals(
                new HashSet<String>(Arrays.asList("Folder", "Workspace",
                        "OtherExcluded")), desc.fulltextExcludedTypes);
        assertEquals(
                new HashSet<String>(Arrays.asList("Note", "File",
                        "OtherIncluded")), desc.fulltextIncludedTypes);
    }

}
