/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert", "org.nuxeo.ecm.core.convert.plugins",
        "org.nuxeo.ecm.platform.convert", "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.platform.rendition.core", "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.platform.mimetype.api", "org.nuxeo.ecm.platform.mimetype.core" })
@LocalDeploy({ "org.nuxeo.ecm.platform.rendition.core:test-renditionprovider-contrib.xml" })
public class TestRenditionProvider {

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    /** Sort by name. */
    private static final Comparator<RenditionDefinition> RENDITION_DEFINITION_CMP = new Comparator<RenditionDefinition>() {
        @Override
        public int compare(RenditionDefinition a, RenditionDefinition b) {
            return a.getName().compareTo(b.getName());
        }
    };

    @Test
    public void testDummyRendition() throws Exception {
        DocumentModel file = createBlobDoc("File");
        Renderable renderable = file.getAdapter(Renderable.class);
        assertNotNull(renderable);

        List<RenditionDefinition> defs = renderable.getAvailableRenditionDefinitions();
        assertEquals(2, defs.size());

        Collections.sort(defs, RENDITION_DEFINITION_CMP);
        RenditionDefinition def = defs.get(0);
        assertEquals("dummyRendition", def.getName());
        assertEquals("dummy/pdf", def.getContentType());

        List<Rendition> renditions = renditionService.getAvailableRenditions(file);
        assertEquals(2, renditions.size());

        Rendition ren = renditionService.getRendition(file, "dummyRendition");
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals(blob.getString(), file.getTitle());
        assertEquals("dummy/pdf", blob.getMimeType());
    }

    @Test
    public void testPdfRendition() throws Exception {
        DocumentModel file = createBlobDoc("File");
        Renderable renderable = file.getAdapter(Renderable.class);
        assertNotNull(renderable);

        List<RenditionDefinition> defs = renderable.getAvailableRenditionDefinitions();
        assertEquals(2, defs.size());

        Collections.sort(defs, RENDITION_DEFINITION_CMP);
        RenditionDefinition def = defs.get(1);
        assertEquals("pdf", def.getName());
        assertEquals("application/pdf", def.getContentType());

        ConversionService conversionService = Framework.getLocalService(ConversionService.class);
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
        DocumentModel note = createBlobDoc("Note");
        Renderable renderable = note.getAdapter(Renderable.class);
        assertNotNull(renderable);

        List<RenditionDefinition> defs = renderable.getAvailableRenditionDefinitions();
        assertEquals(2, defs.size());

        Collections.sort(defs, RENDITION_DEFINITION_CMP);
        RenditionDefinition def = defs.get(1);
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

    protected DocumentModel createBlobDoc(String typeName) {
        DocumentModel file = session.createDocumentModel("/", "dummy-file", typeName);
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        Blob blob = Blobs.createBlob("Dummy text");
        blob.setFilename("dummy.txt");
        bh.setBlob(blob);
        return session.createDocument(file);
    }

}
