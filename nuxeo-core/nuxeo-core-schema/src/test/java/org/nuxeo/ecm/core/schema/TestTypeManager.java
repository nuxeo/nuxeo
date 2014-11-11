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

import java.util.Set;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestTypeManager extends NXRuntimeTestCase {

    SchemaManagerImpl typeManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        typeManager = (SchemaManagerImpl) getTypeManager();
    }

    @After
    public void tearDown() throws Exception {
        typeManager = null;
        super.tearDown();
    }

    public static TypeService getTypeService() {
        return (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
    }

    public static SchemaManager getTypeManager() throws Exception {
        return Framework.getService(SchemaManager.class);
    }

    @Test
    public void testTrivialTypeManager() {
        Type[] types = typeManager.getTypes();
        assertNotNull(types);
        assertEquals(types.length, typeManager.getTypesCount());
        assertTrue(types.length > 0);

        DocumentType[] documentTypes = typeManager.getDocumentTypes();
        assertNotNull(documentTypes);
        assertEquals(1, documentTypes.length);
        assertEquals("Document", documentTypes[0].getName());
        assertEquals(1, typeManager.getDocumentTypesCount());

        Schema[] schemas = typeManager.getSchemas();
        assertNotNull(schemas);
        assertEquals(0, schemas.length);
        assertEquals(0, typeManager.getSchemasCount());
    }

    @Test
    public void testFacetsCache() {
        // avoid WARN, register facets
        typeManager.registerFacet(new FacetDescriptor("parent1", null));
        typeManager.registerFacet(new FacetDescriptor("parent2", null));
        typeManager.registerFacet(new FacetDescriptor("child", null));

        String[] facets = { "parent1", "parent2" };
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        DocumentTypeDescriptor dtd = new DocumentTypeDescriptor("Document",
                "Parent", schemas, facets);
        typeManager.registerDocumentType(dtd);

        assertNotNull(typeManager.getDocumentType("Parent"));
        assertEquals(2, typeManager.getDocumentTypes().length);

        assertEquals("Parent", typeManager.getDocumentType("Parent").getName());
        Set<String> tff = typeManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Parent"));

        // Now adding a derived type
        facets = new String[1];
        facets[0] = "child";
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, facets);
        typeManager.registerDocumentType(dtd);
        assertEquals(3, typeManager.getDocumentTypes().length);

        tff = typeManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(2, tff.size());
        assertTrue(tff.contains("Parent"));
        assertTrue(tff.contains("Child"));
        Set<String> tff2 = typeManager.getDocumentTypeNamesForFacet("parent2");
        assertEquals(tff, tff2);

        tff = typeManager.getDocumentTypeNamesForFacet("child");
        assertNotNull(tff);
        assertEquals(1, tff.size());
        assertTrue(tff.contains("Child"));

        // Unregister child
        typeManager.unregisterDocumentType("Child");
        assertNull(typeManager.getDocumentType("Child"));
        assertNull(typeManager.getDocumentTypeNamesForFacet("child"));
        assertEquals(2, typeManager.getDocumentTypes().length);

        tff = typeManager.getDocumentTypeNamesForFacet("parent1");
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
        typeManager.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor("Parent",
                "Child", schemas, new String[0]);
        typeManager.registerDocumentType(dtd);
        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "TopLevel", schemas, new String[0]);
        typeManager.registerDocumentType(dtd);
        checkInheritanceCache();
    }

    private void checkInheritanceCache() {
        Set<String> types;

        types = typeManager.getDocumentTypeNamesExtending("Parent");
        assertNotNull(types);
        assertEquals(2, types.size());
        assertTrue(types.contains("Parent"));
        assertTrue(types.contains("Child"));

        types = typeManager.getDocumentTypeNamesExtending("Document");
        assertNotNull(types);
        assertEquals(4, types.size());

        types = typeManager.getDocumentTypeNamesExtending("Child");
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("Child"));

        types = typeManager.getDocumentTypeNamesExtending("TopLevel");
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("TopLevel"));

        types = typeManager.getDocumentTypeNamesExtending("Unknown");
        assertNull(types);
    }

    /*
     * Check that registering a child type before the parent works.
     */
    @Test
    public void testFacetsCacheReversedRegistration() {
        // avoid WARN, register facets
        typeManager.registerFacet(new FacetDescriptor("parent1", null));
        typeManager.registerFacet(new FacetDescriptor("parent2", null));
        typeManager.registerFacet(new FacetDescriptor("child", null));

        DocumentTypeDescriptor dtd;
        SchemaDescriptor[] schemas = new SchemaDescriptor[0];
        String[] facets = new String[1];
        facets[0] = "child";
        dtd = new DocumentTypeDescriptor("Parent", "Child", schemas, facets);
        typeManager.registerDocumentType(dtd);

        facets = new String[2];
        facets[0] = "parent1";
        facets[1] = "parent2";
        dtd = new DocumentTypeDescriptor("Document",
                "Parent", schemas, facets);
        typeManager.registerDocumentType(dtd);

        Set<String> tff = typeManager.getDocumentTypeNamesForFacet("parent1");
        assertNotNull(tff);
        assertEquals(2, tff.size());
        assertTrue(tff.contains("Parent"));
        assertTrue(tff.contains("Child"));
        Set<String> tff2 = typeManager.getDocumentTypeNamesForFacet("parent2");
        assertEquals(tff, tff2);

        tff = typeManager.getDocumentTypeNamesForFacet("child");
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
        typeManager.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "Parent", schemas, new String[0]);
        typeManager.registerDocumentType(dtd);

        dtd = new DocumentTypeDescriptor(TypeConstants.DOCUMENT,
                "TopLevel", schemas, new String[0]);
        typeManager.registerDocumentType(dtd);

        checkInheritanceCache();
    }

}
