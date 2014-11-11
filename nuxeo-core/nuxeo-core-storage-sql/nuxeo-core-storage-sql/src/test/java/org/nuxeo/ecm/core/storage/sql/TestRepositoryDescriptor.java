/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FulltextIndexDescriptor;

public class TestRepositoryDescriptor extends TestCase {

    protected RepositoryDescriptor desc;

    protected static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    @Override
    public void setUp() throws Exception {
        XMap xmap = new XMap();
        xmap.register(RepositoryDescriptor.class);
        xmap.register(FulltextIndexDescriptor.class);
        desc = (RepositoryDescriptor) xmap.load(getResource("test-repository-descriptor.xml"));
    }

    @SuppressWarnings("unchecked")
    public void testBasic() throws Exception {
        assertEquals("foo", desc.name);
        assertTrue(desc.clusteringEnabled);
        assertEquals(1234, desc.clusteringDelay);
        assertEquals(Arrays.asList(java.lang.Integer.class,
                java.lang.Long.class), desc.queryMakerClasses);
    }

    @SuppressWarnings("unchecked")
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
        assertEquals(new HashSet(Collections.singleton("foo:bar")), fti.excludeFields);

        fti = desc.fulltextIndexes.get(3);
        assertEquals("pictures", fti.name);
        assertNull(fti.fieldType);
        assertEquals(new HashSet(
                Collections.singleton("picture:views/*/filename")), fti.fields);
        assertEquals(new HashSet(), fti.excludeFields);
        assertEquals("martian", fti.analyzer);
        assertEquals("other", fti.catalog);
    }

    public void testLargeTextFields() throws Exception {
        assertNotNull(desc.schemaFields);
        assertEquals(2, desc.schemaFields.size());
        FieldDescriptor fd;
        fd = desc.schemaFields.get(0);
        assertEquals("my:bignote", fd.field);
        assertEquals("biig", fd.type);
        fd = desc.schemaFields.get(1);
        assertEquals("foo", fd.field);
        assertEquals("xyz", fd.type);
    }

}
