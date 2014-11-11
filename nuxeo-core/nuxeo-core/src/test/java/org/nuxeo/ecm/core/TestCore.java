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

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestCore extends NXRuntimeTestCase {

    private SchemaManager typeManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle(CoreUTConstants.SCHEMA_BUNDLE);
        deployContrib(CoreUTConstants.CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        deployContrib(CoreUTConstants.CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");
        deployContrib(CoreUTConstants.CORE_BUNDLE, "OSGI-INF/CoreExtensions.xml");
        typeManager = Framework.getLocalService(SchemaManager.class);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        typeManager = null;
    }

    @Test
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
    @Test
    @Ignore
    public void testDocTypes() {
        assertEquals(3, typeManager.getDocumentTypesCount());

        // TODO: Iterate on docTypes and check things out

        DocumentType docType = typeManager.getDocumentType(TypeConstants.DOCUMENT);
        assertNotNull(docType);
        docType = typeManager.getDocumentType("other name");
        assertNull(docType);
    }


}
