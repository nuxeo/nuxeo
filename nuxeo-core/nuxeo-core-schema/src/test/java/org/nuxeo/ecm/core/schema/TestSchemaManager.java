/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchemaManager extends NXRuntimeTestCase {

    SchemaManagerImpl schemaManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        schemaManager = (SchemaManagerImpl) Framework.getLocalService(SchemaManager.class);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        schemaManager = null;
        super.tearDown();
    }

    @Test
    public void testTrivialTypeManager() {
        Collection<Type> types = schemaManager.getTypes();
        assertNotNull(types);
        assertTrue(types.size() > 0);

        DocumentType[] documentTypes = schemaManager.getDocumentTypes();
        assertNotNull(documentTypes);
        assertEquals(1, documentTypes.length);
        assertEquals("Document", documentTypes[0].getName());
        assertEquals(1, schemaManager.getDocumentTypesCount());

        Schema[] schemas = schemaManager.getSchemas();
        assertNotNull(schemas);
        assertEquals(0, schemas.length);
    }

    @Test
    public void testFacetsCache() {
        // avoid WARN, register facets
        schemaManager.registerFacet(new FacetDescriptor("parent1", null));
        schemaManager.registerFacet(new FacetDescriptor("parent2", null));
        schemaManager.registerFacet(new FacetDescriptor("child", null));

        String[] facets = { "parent1", "parent2" };
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd = new DocumentTypeDescriptor("Document",
                "Parent", schemas, facets);
        schemaManager.registerDocumentType(dtd);

        assertNotNull(schemaManager.getDocumentType("Parent"));
        assertEquals(2, schemaManager.getDocumentTypes().length);

        assertEquals("Parent",
                schemaManager.getDocumentType("Parent").getName());
        Set<String> tff = schemaManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Parent"));

        // Now adding a derived type
        facets = new String[1];
        facets[0] = "child";
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, facets);
        schemaManager.registerDocumentType(dtd);
        assertEquals(3, schemaManager.getDocumentTypes().length);

        tff = schemaManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(2, tff.size());
        assertTrue(tff.contains("Parent"));
        assertTrue(tff.contains("Child"));
        Set<String> tff2 = schemaManager.getDocumentTypeNamesForFacet("parent2");
        assertEquals(tff, tff2);

        tff = schemaManager.getDocumentTypeNamesForFacet("child");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Child"));

        // Unregister child
        schemaManager.unregisterDocumentType(dtd);
        assertNull(schemaManager.getDocumentType("Child"));
        assertNull(schemaManager.getDocumentTypeNamesForFacet("child"));
        assertEquals(2, schemaManager.getDocumentTypes().length);

        tff = schemaManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Parent"));
    }

    @Test
    public void testInheritanceCache() {
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd;
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "Parent",
                schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas,
                new String[0]);
        schemaManager.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "TopLevel",
                schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);
        checkInheritanceCache();
    }

    private void checkInheritanceCache() {
        Set<String> types;

        types = schemaManager.getDocumentTypeNamesExtending("Parent");
        assertNotNull(types);
        assertEquals(2, types.size());
        assertTrue(types.contains("Parent"));
        assertTrue(types.contains("Child"));

        types = schemaManager.getDocumentTypeNamesExtending("Document");
        assertNotNull(types);
        assertEquals(4, types.size());

        types = schemaManager.getDocumentTypeNamesExtending("Child");
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("Child"));

        types = schemaManager.getDocumentTypeNamesExtending("TopLevel");
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("TopLevel"));

        types = schemaManager.getDocumentTypeNamesExtending("Unknown");
        assertNull(types);
    }

    /*
     * Check that registering a child type before the parent works.
     */
    @Test
    public void testFacetsCacheReversedRegistration() {
        // avoid WARN, register facets
        schemaManager.registerFacet(new FacetDescriptor("parent1", null));
        schemaManager.registerFacet(new FacetDescriptor("parent2", null));
        schemaManager.registerFacet(new FacetDescriptor("child", null));

        DocumentTypeDescriptor dtd;
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        String[] facets = new String[1];
        facets[0] = "child";
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, facets);
        schemaManager.registerDocumentType(dtd);

        facets = new String[2];
        facets[0] = "parent1";
        facets[1] = "parent2";
        dtd = new DocumentTypeDescriptor("Document", "Parent", schemas, facets);
        schemaManager.registerDocumentType(dtd);

        Set<String> tff = schemaManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(2, tff.size());
        assertTrue(tff.contains("Parent"));
        assertTrue(tff.contains("Child"));
        Set<String> tff2 = schemaManager.getDocumentTypeNamesForFacet("parent2");
        assertEquals(tff, tff2);

        tff = schemaManager.getDocumentTypeNamesForFacet("child");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Child"));
    }

    @Test
    public void testInheritanceCacheReversedRegistration() {
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd;

        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas,
                new String[0]);
        schemaManager.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "Parent",
                schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "TopLevel",
                schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);

        checkInheritanceCache();
    }

    @Test
    public void testDynamicChanges() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/CoreTestExtensions.xml");
        DocumentType t = schemaManager.getDocumentType("myDoc3");
        Set<String> ts = new HashSet<String>(Arrays.asList(t.getSchemaNames()));
        assertEquals(new HashSet<String>(Arrays.asList("schema1", "schema2")),
                ts);
        assertEquals(Collections.singleton("myfacet"), t.getFacets());

        // add a new schema the myDoc2 and remove a facet
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/test-change-doctype.xml");

        // myDoc3, a child type, sees the change
        t = schemaManager.getDocumentType("myDoc3");
        ts = new HashSet<String>(Arrays.asList(t.getSchemaNames()));
        assertEquals(new HashSet<String>(Arrays.asList("schema1", "common")),
                ts);
        assertEquals(0, t.getFacets().size());
    }

    @Test
    public void testSupertypeLoop() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/test-supertype-loop.xml");
        DocumentType t = schemaManager.getDocumentType("someDocInLoop");
        DocumentType t2 = schemaManager.getDocumentType("someDocInLoop2");
        assertEquals("someDocInLoop2", t.getSuperType().getName());
        assertNull(t2.getSuperType());
    }

    @Test
    public void testMissingSupertype() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/test-missing-supertype.xml");
        DocumentType t = schemaManager.getDocumentType("someDoc");
        DocumentType t2 = schemaManager.getDocumentType("someDoc2");
        assertNull(t.getSuperType());
        assertEquals("someDoc", t2.getSuperType().getName());
    }

    @Test
    public void testFacetMissingSchema() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/test-facet-missing-schema.xml");
        CompositeType f = schemaManager.getFacet("someFacet");
        assertEquals(Collections.singletonList("common"),
                Arrays.asList(f.getSchemaNames()));
    }

    @Test
    public void testFacetNoPerDocumentQuery() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/test-facet-per-document.xml");
        assertTrue(schemaManager.getNoPerDocumentQueryFacets().contains(
                "someFacet"));
    }

    protected static List<String> schemaNames(List<Schema> schemas) {
        List<String> list = new ArrayList<String>(schemas.size());
        for (Schema s : schemas) {
            list.add(s.getName());
        }
        return list;
    }

    @Test
    public void testMergeDocumentType() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/CoreTestExtensions.xml");
        DocumentType t = schemaManager.getDocumentType("myDoc");
        assertEquals(Collections.singletonList("schema2"),
                Arrays.asList(t.getSchemaNames()));
        assertEquals(
                new HashSet<String>(Arrays.asList("viewable", "writable")),
                t.getFacets());

        t = schemaManager.getDocumentType("myDoc2");
        Set<String> ts = new HashSet<String>(Arrays.asList(t.getSchemaNames()));
        assertEquals(new HashSet<String>(Arrays.asList("schema1", "schema2")),
                ts);
        assertEquals(Collections.singleton("myfacet"), t.getFacets());

        assertEquals(Arrays.asList("schema3"),
                schemaNames(schemaManager.getProxySchemas(null)));

        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/test-merge-doctype.xml");
        t = schemaManager.getDocumentType("myDoc");
        assertEquals(Collections.singletonList("schema2"),
                Arrays.asList(t.getSchemaNames()));
        assertEquals(
                new HashSet<String>(Arrays.asList("viewable", "writable",
                        "NewFacet")), t.getFacets());

        t = schemaManager.getDocumentType("myDoc2");
        ts = new HashSet<String>(Arrays.asList(t.getSchemaNames()));
        assertEquals(
                new HashSet<String>(Arrays.asList("schema1", "schema2",
                        "newschema", "newschema2")), ts);
        assertEquals(
                new HashSet<String>(Arrays.asList("myfacet", "NewFacet2")),
                t.getFacets());

        assertEquals(Arrays.asList("schema3", "newschema"),
                schemaNames(schemaManager.getProxySchemas(null)));
    }

    @Test
    public void testDeployWithIncludeAndImport() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/testSchemaWithImportInclude.xml");

        Schema schema = schemaManager.getSchema("schemaWithIncludeAndImport");
        assertNotNull(schema);

        assertEquals("typeA", schema.getField("field3").getType().getName());
        assertEquals("typeB", schema.getField("field4").getType().getName());

        assertTrue(schema.getField("field4").getType().isComplexType());
        assertTrue(schema.getField("field3").getType().isComplexType());

        assertEquals(
                3,
                ((ComplexType) schema.getField("field3").getType()).getFieldsCount());
        assertEquals(
                2,
                ((ComplexType) schema.getField("field4").getType()).getFieldsCount());
    }

    @Test
    public void testDeploySchemaWithRebase() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "OSGI-INF/testSchemaRebase.xml");

        Schema schema = schemaManager.getSchema("employeeSchema");
        assertNotNull(schema);

        // additional fields
        assertTrue(schema.hasField("address"));
        assertTrue(schema.hasField("city"));
        assertTrue(schema.hasField("country"));

        // inherited fields
        assertTrue(schema.hasField("firstname"));
        assertTrue(schema.hasField("lastname"));

        // super parent inherited fields
        assertTrue(schema.hasField("race"));

        assertEquals(6, schema.getFieldsCount());

    }

}
