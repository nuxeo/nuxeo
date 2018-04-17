/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-renditionprovider-contrib.xml")
public class TestRenditionProvider {

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    /** Sort by name. */
    public static final Comparator<RenditionDefinition> RENDITION_DEFINITION_CMP = new Comparator<RenditionDefinition>() {
        @Override
        public int compare(RenditionDefinition a, RenditionDefinition b) {
            return a.getName().compareTo(b.getName());
        }
    };

    @Test
    public void testDummyRendition() throws Exception {
        DocumentModel file = createBlobDoc("File", session);
        Renderable renderable = file.getAdapter(Renderable.class);
        assertNotNull(renderable);

        List<RenditionDefinition> defs = renderable.getAvailableRenditionDefinitions();
        assertEquals(5, defs.size());

        Collections.sort(defs, RENDITION_DEFINITION_CMP);
        RenditionDefinition def = defs.get(0);
        assertEquals("dummyRendition", def.getName());
        assertEquals("dummy/pdf", def.getContentType());

        List<Rendition> renditions = renditionService.getAvailableRenditions(file);
        assertEquals(5, renditions.size());

        Rendition ren = renditionService.getRendition(file, "dummyRendition");
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals(blob.getString(), file.getTitle());
        assertEquals("dummy/pdf", blob.getMimeType());
    }

    @Test
    public void testPdfRendition() throws Exception {
        DocumentModel file = createBlobDoc("File", session);
        Renderable renderable = file.getAdapter(Renderable.class);
        assertNotNull(renderable);

        List<RenditionDefinition> defs = renderable.getAvailableRenditionDefinitions();
        assertEquals(5, defs.size());

        Collections.sort(defs, RENDITION_DEFINITION_CMP);
        RenditionDefinition def = defs.get(2);
        assertEquals("pdf", def.getName());
        assertEquals("application/pdf", def.getContentType());

        ConversionService conversionService = Framework.getService(ConversionService.class);
        ConverterCheckResult check = conversionService.isConverterAvailable("any2pdf");
        if (!check.isAvailable()) {
            return;
        }
        Rendition ren = renditionService.getRendition(file, "pdf");
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals("application/pdf", blob.getMimeType());
    }

    @Test
    public void testPdfRenditionStoredFromNote() throws Exception {
        DocumentModel note = createBlobDoc("Note", session);
        Renderable renderable = note.getAdapter(Renderable.class);
        assertNotNull(renderable);

        List<RenditionDefinition> defs = renderable.getAvailableRenditionDefinitions();
        assertEquals(5, defs.size());

        Collections.sort(defs, RENDITION_DEFINITION_CMP);
        RenditionDefinition def = defs.get(2);
        assertEquals("pdf", def.getName());
        assertEquals("application/pdf", def.getContentType());

        ConversionService conversionService = Framework.getService(ConversionService.class);
        ConverterCheckResult check = conversionService.isConverterAvailable("any2pdf");
        assumeTrue("converter any2pdf not available", check.isAvailable());
        Rendition ren = renditionService.getRendition(note, "pdf", true); // store
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals("application/pdf", blob.getMimeType());
    }

    public static DocumentModel createBlobDoc(String typeName, CoreSession session) {
        return createBlobDoc("/", "dummy-file", "dummy.txt", typeName, session);
    }

    public static DocumentModel createBlobDoc(String parentPath, String name, String filename, String typeName,
            CoreSession session) {
        DocumentModel file = session.createDocumentModel(parentPath, name, typeName);
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        Blob blob = Blobs.createBlob("Dummy text");
        blob.setFilename(filename);
        bh.setBlob(blob);
        return session.createDocument(file);
    }

}
