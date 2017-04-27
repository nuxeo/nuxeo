/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ui.select2.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestSelect2Common {

    @Inject
    protected CoreSession session;

    @Test
    public void testResolveReference() {
        DocumentModel testDoc = session.createDocument(session.createDocumentModel("/", "testDoc", "File"));
        testDoc.setPropertyValue("dc:description", "My description");
        session.saveDocument(testDoc);
        session.save();

        // Document found by property value
        DocumentModel resolvedDoc = Select2Common.resolveReference("dc:description", "My description", session);
        assertEquals(testDoc, resolvedDoc);

        // Document not found by property value
        assertNull(Select2Common.resolveReference("dc:description", "My other description", session));

        // Document found by IdRef
        resolvedDoc = Select2Common.resolveReference(null, testDoc.getId(), session);
        assertEquals(testDoc, resolvedDoc);

        // Document found by PathRef
        resolvedDoc = Select2Common.resolveReference(null, testDoc.getPathAsString(), session);
        assertEquals(testDoc, resolvedDoc);

        // Document not found by DocumentRef
        assertNull(Select2Common.resolveReference(null, "/nonexistent", session));

        // Document found by property value including a single quote
        testDoc.setPropertyValue("dc:description", "It's a nice description");
        session.saveDocument(testDoc);
        session.save();
        resolvedDoc = Select2Common.resolveReference("dc:description", "It's a nice description", session);
        assertEquals(testDoc, resolvedDoc);
    }

}
