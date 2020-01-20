/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
public class TestSchemaManager {

    @Inject
    public SchemaManager schemaManager;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testTrivialTypeManager() {
        Collection<Type> types = ((SchemaManagerImpl) schemaManager).getTypes();
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
        SchemaManagerImpl schemaManagerImpl = (SchemaManagerImpl) schemaManager;
        schemaManagerImpl.registerFacet(new FacetDescriptor("parent1", null));
        schemaManagerImpl.registerFacet(new FacetDescriptor("parent2", null));
        schemaManagerImpl.registerFacet(new FacetDescriptor("child", null));

        String[] facets = { "parent1", "parent2" };
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd = new DocumentTypeDescriptor("Document", "Parent", schemas, facets);
        schemaManagerImpl.registerDocumentType(dtd);

        assertNotNull(schemaManager.getDocumentType("Parent"));
        assertEquals(2, schemaManager.getDocumentTypes().length);

        assertEquals("Parent", schemaManager.getDocumentType("Parent").getName());
        Set<String> tff = schemaManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Parent"));

        // Now adding a derived type
        facets = new String[1];
        facets[0] = "child";
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, facets);
        schemaManagerImpl.registerDocumentType(dtd);
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
        schemaManagerImpl.unregisterDocumentType(dtd);
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
        SchemaManagerImpl schemaManagerImpl = (SchemaManagerImpl) schemaManager;
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd;
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "Parent", schemas, new String[0]);
        schemaManagerImpl.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, new String[0]);
        schemaManagerImpl.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "TopLevel", schemas, new String[0]);
        schemaManagerImpl.registerDocumentType(dtd);
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
        SchemaManagerImpl schemaManagerImpl = (SchemaManagerImpl) schemaManager;
        schemaManagerImpl.registerFacet(new FacetDescriptor("parent1", null));
        schemaManagerImpl.registerFacet(new FacetDescriptor("parent2", null));
        schemaManagerImpl.registerFacet(new FacetDescriptor("child", null));

        DocumentTypeDescriptor dtd;
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        String[] facets = new String[1];
        facets[0] = "child";
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, facets);
        schemaManagerImpl.registerDocumentType(dtd);

        facets = new String[2];
        facets[0] = "parent1";
        facets[1] = "parent2";
        dtd = new DocumentTypeDescriptor("Document", "Parent", schemas, facets);
        schemaManagerImpl.registerDocumentType(dtd);

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
        SchemaManagerImpl schemaManagerImpl = (SchemaManagerImpl) schemaManager;
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd;

        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, new String[0]);
        schemaManagerImpl.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "Parent", schemas, new String[0]);
        schemaManagerImpl.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT, "TopLevel", schemas, new String[0]);
        schemaManagerImpl.registerDocumentType(dtd);

        checkInheritanceCache();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/CoreTestExtensions.xml")
    public void testDynamicChanges() throws Exception {

        DocumentType t = schemaManager.getDocumentType("myDoc3");
        assertEquals(Set.of("schema1", "schema2"), Set.of(t.getSchemaNames()));
        assertEquals(Set.of("myfacet"), t.getFacets());

        // add a new schema the myDoc2 and remove a facet
        hotDeployer.deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-change-doctype.xml");

        // myDoc3, a child type, sees the change
        t = schemaManager.getDocumentType("myDoc3");
        assertEquals(Set.of("schema1", "common"), Set.of(t.getSchemaNames()));
        assertEquals(0, t.getFacets().size());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-supertype-loop.xml")
    public void testSupertypeLoop() {
        DocumentType t = schemaManager.getDocumentType("someDocInLoop");
        DocumentType t2 = schemaManager.getDocumentType("someDocInLoop2");
        assertEquals("someDocInLoop2", t.getSuperType().getName());
        assertNull(t2.getSuperType());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-missing-supertype.xml")
    public void testMissingSupertype() {
        DocumentType t = schemaManager.getDocumentType("someDoc");
        DocumentType t2 = schemaManager.getDocumentType("someDoc2");
        assertNull(t.getSuperType());
        assertEquals("someDoc", t2.getSuperType().getName());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-facet-missing-schema.xml")
    public void testFacetMissingSchema() {
        CompositeType f = schemaManager.getFacet("someFacet");
        assertEquals(List.of("common"), List.of(f.getSchemaNames()));
    }

    @Test
    public void testFacetNoPerDocumentQuery() throws Exception {
        assertFalse(schemaManager.getNoPerDocumentQueryFacets().contains("someFacet"));

        hotDeployer.deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-facet-per-document.xml");

        assertTrue(schemaManager.getNoPerDocumentQueryFacets().contains("someFacet"));

        hotDeployer.deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-facet-per-document-override.xml");

        assertFalse(schemaManager.getNoPerDocumentQueryFacets().contains("someFacet"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-facet-disabled.xml")
    public void testFacetDisabled() {
        CompositeType f = schemaManager.getFacet("someFacet");
        assertNull(f);
        DocumentType t = schemaManager.getDocumentType("myDoc");
        assertNotNull(t);
        assertEquals(Collections.emptySet(), t.getFacets());
        assertEquals(Collections.emptySet(), Set.of(t.getSchemaNames()));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-schema-disabled.xml")
    public void testSchemaDisabled() {
        Schema s = schemaManager.getSchema("someSchema");
        assertNull(s);
        CompositeType f = schemaManager.getFacet("someFacet");
        assertEquals(List.of("common"), List.of(f.getSchemaNames()));
        DocumentType t = schemaManager.getDocumentType("myDoc");
        assertEquals(List.of("common"), List.of(t.getSchemaNames()));
        DocumentType t2 = schemaManager.getDocumentType("myDoc2");
        assertEquals(List.of("common"), List.of(t2.getSchemaNames()));
    }

    protected static List<String> schemaNames(List<Schema> schemas) {
        return schemas.stream().map(Schema::getName).collect(Collectors.toList());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/CoreTestExtensions.xml")
    public void testMergeDocumentType() throws Exception {

        DocumentType t = schemaManager.getDocumentType("myDoc");
        assertEquals(List.of("schema2"), List.of(t.getSchemaNames()));
        assertEquals(Set.of("viewable", "writable"), t.getFacets());

        t = schemaManager.getDocumentType("myDoc2");
        assertEquals(Set.of("schema1", "schema2"), Set.of(t.getSchemaNames()));
        assertEquals(Set.of("myfacet"), t.getFacets());

        assertEquals(List.of("schema3"), schemaNames(schemaManager.getProxySchemas(null)));

        hotDeployer.deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-merge-doctype.xml");

        t = schemaManager.getDocumentType("myDoc");
        assertEquals(List.of("schema2"), List.of(t.getSchemaNames()));
        assertEquals(Set.of("viewable", "writable", "NewFacet"), t.getFacets());

        t = schemaManager.getDocumentType("myDoc2");
        assertEquals(Set.of("schema1", "schema2", "newschema", "newschema2"), Set.of(t.getSchemaNames()));
        assertEquals(Set.of("myfacet", "NewFacet2"), t.getFacets());

        assertEquals(List.of("schema3", "newschema"), schemaNames(schemaManager.getProxySchemas(null)));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/testSchemaWithImportInclude.xml")
    public void testDeployWithIncludeAndImport() {

        Schema schema = schemaManager.getSchema("schemaWithIncludeAndImport");
        assertNotNull(schema);

        assertEquals("typeA", schema.getField("field3").getType().getName());
        assertEquals("typeB", schema.getField("field4").getType().getName());

        assertTrue(schema.getField("field4").getType().isComplexType());
        assertTrue(schema.getField("field3").getType().isComplexType());

        assertEquals(3, ((ComplexType) schema.getField("field3").getType()).getFieldsCount());
        assertEquals(2, ((ComplexType) schema.getField("field4").getType()).getFieldsCount());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/testSchemaRebase.xml")
    public void testDeploySchemaWithRebase() {

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

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/CoreTestExtensions.xml")
    public void testHasSuperType() {

        assertTrue(schemaManager.hasSuperType("Document", "Document"));
        assertTrue(schemaManager.hasSuperType("myDoc", "Document"));
        assertTrue(schemaManager.hasSuperType("myDoc3", "myDoc2"));

        assertFalse(schemaManager.hasSuperType("myDoc", null));
        assertFalse(schemaManager.hasSuperType(null, "Document"));
        assertFalse(schemaManager.hasSuperType("myDoc4", "myDoc2"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/CoreTestExtensions.xml")
    public void testGetAllowedSubTypes() throws Exception {

        Collection<String> subtypes = schemaManager.getAllowedSubTypes("myFolder");
        assertNotNull(subtypes);
        assertEquals(3, subtypes.size());
        assertTrue(subtypes.contains("myDoc"));
        assertTrue(subtypes.contains("myDoc2"));
        assertTrue(subtypes.contains("myDoc3"));

        subtypes = schemaManager.getAllowedSubTypes("myFolder2");
        assertNotNull(subtypes);
        assertEquals(1, subtypes.size());
        assertTrue(subtypes.contains("myDoc4"));

        subtypes = schemaManager.getAllowedSubTypes("mySpecialFolder");
        assertNotNull(subtypes);
        assertEquals(5, subtypes.size());
        assertTrue(subtypes.contains("myDoc"));
        assertTrue(subtypes.contains("myDoc2"));
        assertTrue(subtypes.contains("myDoc3"));
        assertTrue(subtypes.contains("myDoc4"));
        assertTrue(subtypes.contains("myFolder"));

        // let's append types to myFolder and override mySpecialFoder
        hotDeployer.deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-merge-doctype.xml");

        subtypes = schemaManager.getAllowedSubTypes("myFolder");
        assertNotNull(subtypes);
        assertEquals(4, subtypes.size());
        assertTrue(subtypes.contains("myDoc"));
        assertTrue(subtypes.contains("myDoc2"));
        assertTrue(subtypes.contains("myDoc3"));
        assertTrue(subtypes.contains("myDoc4"));

        subtypes = schemaManager.getAllowedSubTypes("myFolder2");
        assertNotNull(subtypes);
        assertEquals(3, subtypes.size());
        assertTrue(subtypes.contains("myDoc"));
        assertTrue(subtypes.contains("myDoc2"));
        assertTrue(subtypes.contains("myDoc3"));

        subtypes = schemaManager.getAllowedSubTypes("mySpecialFolder");
        assertNotNull(subtypes);
        assertEquals(1, subtypes.size());
        assertTrue(subtypes.contains("myDoc"));

    }

    @Test
    @Deploy("org.nuxeo.ecm.core.schema.tests:OSGI-INF/test-extends-append-doctypes.xml")
    public void testExtendsAppendTypes() {
        // test appending types which extend another document
        DocumentType t = schemaManager.getDocumentType("MyMergeableFolder");
        assertEquals(t.getFacets(), Set.of("specdoc", "specdoc2", "specdoc3", "specdoc4"));
        assertEquals(t.getAllowedSubtypes(), Set.of("myDoc2", "myDoc3", "myDoc4"));
    }

}
