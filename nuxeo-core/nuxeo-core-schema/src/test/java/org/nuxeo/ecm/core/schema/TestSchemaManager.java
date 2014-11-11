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

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchemaManager extends NXRuntimeTestCase {

    SchemaManagerImpl schemaManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        schemaManager = (SchemaManagerImpl) getSchemaManager();
    }

    @After
    public void tearDown() throws Exception {
        schemaManager = null;
        super.tearDown();
    }

    public static SchemaManager getSchemaManager() throws Exception {
        return Framework.getService(SchemaManager.class);
    }

    @Test
    public void testTrivialTypeManager() {
        Type[] types = schemaManager.getTypes();
        assertNotNull(types);
        assertEquals(types.length, schemaManager.getTypesCount());
        assertTrue(types.length > 0);

        DocumentType[] documentTypes = schemaManager.getDocumentTypes();
        assertNotNull(documentTypes);
        assertEquals(1, documentTypes.length);
        assertEquals("Document", documentTypes[0].getName());
        assertEquals(1, schemaManager.getDocumentTypesCount());

        Schema[] schemas = schemaManager.getSchemas();
        assertNotNull(schemas);
        assertEquals(0, schemas.length);
        assertEquals(0, schemaManager.getSchemasCount());
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

        assertEquals("Parent", schemaManager.getDocumentType("Parent").getName());
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
        schemaManager.unregisterDocumentType("Child");
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
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "Parent", schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor("Parent",
                "Child", schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "TopLevel", schemas, new String[0]);
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
        dtd = new DocumentTypeDescriptor("Document",
                "Parent", schemas, facets);
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

        dtd = new DocumentTypeDescriptor("Parent",
                "Child", schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "Parent", schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "TopLevel", schemas, new String[0]);
        schemaManager.registerDocumentType(dtd);

        checkInheritanceCache();
    }

}
