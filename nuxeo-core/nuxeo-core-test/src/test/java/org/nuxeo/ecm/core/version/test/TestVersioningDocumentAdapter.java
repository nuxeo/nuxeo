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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;

public class TestVersioningDocumentAdapter extends AbstractTestVersioning {

    @Test
    public void testVersionLabel() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", vdoc.getVersionLabel());

        doc.checkIn(VersioningOption.MINOR, "");
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", vdoc.getVersionLabel());

        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", vdoc.getVersionLabel());
    }

}
