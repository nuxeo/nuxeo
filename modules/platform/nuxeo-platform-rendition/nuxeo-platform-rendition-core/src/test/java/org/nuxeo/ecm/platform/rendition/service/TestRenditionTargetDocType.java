/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.rendition.service.TestRenditionService.PDF_RENDITION_DEFINITION;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-default-rendition-schemas.xml")
public class TestRenditionTargetDocType {

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    protected DocumentModel document;

    protected DocumentModel section;

    @Before
    public void setUp() {
        document = session.createDocumentModel("/", "filederish", "CustomFolderish");
        document.setPropertyValue("dc:title", "TestFilederish");
        document.setPropertyValue("filederish:dummy", "foo");
        document = session.createDocument(document);
        Blob blob = Blobs.createBlob("I am a Blob");
        document.setPropertyValue("file:content", (Serializable) blob);
        document = session.saveDocument(document);

        section = session.createDocumentModel("/", "section", "Section");
        section = session.createDocument(section);
    }

    @Test
    public void testRenditionTargetDocumentType() {
        DocumentModel publishedRendition = renditionService.publishRendition(document, section,
                PDF_RENDITION_DEFINITION, false);
        assertNotNull(publishedRendition);
        assertTrue(publishedRendition.isProxy());
        assertEquals(section.getRef(), publishedRendition.getParentRef());
        assertEquals("foo", publishedRendition.getPropertyValue("filederish:dummy"));
        assertEquals("CustomRendition", publishedRendition.getType());
        assertFalse(publishedRendition.hasFacet(FacetNames.FOLDERISH));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.rendition.core:test-retention-invalid-target-type-contrib.xml")
    public void testInvalidRenditionTargetDocumentType() {
        assertThrows(NuxeoException.class,
                () -> renditionService.publishRendition(document, section, PDF_RENDITION_DEFINITION, false));
    }
}
