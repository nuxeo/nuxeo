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

package org.nuxeo.ecm.core.api;

import java.util.Collections;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDocumentModel extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
    }

    @SuppressWarnings({"ObjectEqualsNull", "SimplifiableJUnitAssertion"})
    public void testDocumentModelImpl() throws Exception {
        DocumentModel model = new DocumentModelImpl("my type");

        assertEquals("my type", model.getType());

        // assertNull(model.getACP());
        // assertNull(model.getAdapter(Object.class));

        assertNull(model.getDataModel("toto"));
        assertTrue(model.getDataModels().isEmpty());
        assertTrue(model.getDataModelsCollection().isEmpty());

        assertEquals(Collections.emptySet(), model.getDeclaredFacets());
        assertEquals(0, model.getDeclaredSchemas().length);
        assertEquals(Collections.emptySet(), model.getFacets());
        assertEquals(0, model.getSchemas().length);
        assertNull(model.getId());
        assertNull(model.getLock()); // old
        assertNull(model.getLockInfo());
        assertNull(model.getName());
        assertNull(model.getParentRef());
        assertNull(model.getPath());
        assertNull(model.getPathAsString());
        assertNull(model.getProperties(""));
        assertNull(model.getProperty("", ""));
        assertNull(model.getRef());
        assertNull(model.getSessionId());

        assertFalse(model.hasFacet(""));
        assertFalse(model.hasSchema(""));
        assertFalse(model.isDownloadable());
        assertFalse(model.isFolder());
        assertFalse(model.isLocked());
        assertFalse(model.isVersionable());
        assertFalse(model.isVersion());
        assertNull(model.getRepositoryName());
        assertNull(model.getSessionId());
        //assertNull(model.getLifeCyclePolicy());

        assertTrue(model.equals(model));
        assertFalse(model.equals(null));

        assertNotNull(model.toString());
    }

}
