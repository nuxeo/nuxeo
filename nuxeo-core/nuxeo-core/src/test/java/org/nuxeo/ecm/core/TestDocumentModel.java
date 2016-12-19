/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.Prefetch;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDocumentModel extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
    }

    @Test
    public void testDocumentModelImpl() throws Exception {
        DocumentModel model = new DocumentModelImpl("my type");

        assertEquals("my type", model.getType());

        // assertNull(model.getACP());
        // assertNull(model.getAdapter(Object.class));

        assertNull(model.getDataModel("toto"));
        assertTrue(model.getDataModels().isEmpty());
        assertTrue(model.getDataModelsCollection().isEmpty());

        @SuppressWarnings("deprecation")
        Set<String> declaredFacets = model.getDeclaredFacets();
        assertEquals(Collections.emptySet(), declaredFacets);
        @SuppressWarnings("deprecation")
        String[] declaredSchemas = model.getDeclaredSchemas();
        assertEquals(0, declaredSchemas.length);
        assertEquals(Collections.emptySet(), model.getFacets());
        assertEquals(0, model.getSchemas().length);
        assertNull(model.getId());
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
        // assertNull(model.getLifeCyclePolicy());

        assertTrue(model.equals(model));
        assertFalse(model.equals(null));

        assertNotNull(model.toString());
    }

    // this test needs the CoreSessionService available (for DocumentModelImpl.writeReplace)
    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        DocumentModelImpl original = new DocumentModelImpl("my type");
        original.setPrefetch(new Prefetch());
        original.attach("somesessionid");
        // check it's attached
        checkAttached(original, true);
        // write it
        byte[] buffer = SerializationUtils.serialize(original);
        original = null;
        // read it
        Object rehydrated = SerializationUtils.deserialize(buffer);
        // check it's a document and it's detached
        assertNotNull(rehydrated);
        assertTrue(rehydrated instanceof DocumentModelImpl);
        checkAttached((DocumentModelImpl) rehydrated, false);
    }

    private void checkAttached(DocumentModelImpl original, boolean expectAttached) {
        try {
            original.attach("someother");
            if (expectAttached) {
                Assert.fail();
            }
            original.detach(false);
        } catch (NuxeoException ne) {
            if (!expectAttached) {
                Assert.fail();
            }
        }
    }
}
