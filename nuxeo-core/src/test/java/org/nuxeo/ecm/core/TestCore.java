/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core;

import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestCore extends NXRuntimeTestCase {

    private SchemaManager typeManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle(CoreTestConstants.SCHEMA_BUNDLE);
        deployContrib(CoreTestConstants.CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        deployContrib(CoreTestConstants.CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");
        deployContrib(CoreTestConstants.CORE_BUNDLE, "OSGI-INF/CoreExtensions.xml");
        typeManager = NXSchema.getSchemaManager();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        typeManager = null;
    }

    public void testRepositoryManager() {
        try {
            Repository repo = NXCore.getRepository("improbable name");
            assertNull(repo);
        } catch (NoSuchRepositoryException e) {
            // do nothing
        }
    }

    // :XXX: You can't test that since other tests are registering or deleting
    // document types from here.
    public void xxxtestDocTypes() {
        assertEquals(3, typeManager.getDocumentTypesCount());

        // TODO: Iterate on docTypes and check things out

        DocumentType docType = typeManager.getDocumentType(TypeConstants.DOCUMENT);
        assertNotNull(docType);
        docType = typeManager.getDocumentType("other name");
        assertNull(docType);
    }

    // :XXX: You can't test that since other tests are registering or deleting
    // document types from here.
    public void xxxtestSchemaRegistry() {
        Schema[] schemas = typeManager.getSchemas();
        // Default schemas registry is empty
        assertEquals(0, schemas.length);
    }

    // :XXX: You can't test that since other tests are registering or deleting
    // document types from here.
    public void xxxtestTypeRegistry() {
        Type[] types = typeManager.getTypes();
        // Default types registry has 2 types
        // TODO: what is this type and what should we test?
        // This test needs to be fixed.
        assertEquals(2, types.length);
    }

}
