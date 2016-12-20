/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.actions",
        "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert", "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.platform.rendition.core" })
@LocalDeploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-listeners-override.xml")
public class TestPictureRenditions {

    public static final List<String> EXPECTED_ALL_RENDITION_DEFINITION_NAMES = Arrays.asList("xmlExport", "zipExport",
            "zipTreeExport", "Thumbnail", "Small", "Medium", "FullHD", "OriginalJpeg");

    public static final List<String> EXPECTED_FILTERED_RENDITION_DEFINITION_NAMES = Arrays.asList("xmlExport",
            "zipExport", "zipTreeExport", "Small", "FullHD", "OriginalJpeg");

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Inject
    protected ImagingService imagingService;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Test
    public void shouldExposeAllPictureViewsAsRenditions() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "picture", "Picture");
        doc = session.createDocument(doc);

        List<RenditionDefinition> availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(7, availableRenditionDefinitions.size());
        for (RenditionDefinition definition : availableRenditionDefinitions) {
            assertTrue(EXPECTED_ALL_RENDITION_DEFINITION_NAMES.contains(definition.getName()));
        }

        List<Rendition> availableRenditions = renditionService.getAvailableRenditions(doc);
        assertEquals(7, availableRenditions.size());
        // they are all visible
        availableRenditions = renditionService.getAvailableRenditions(doc, true);
        assertEquals(7, availableRenditions.size());
    }

    @Test
    public void shouldExposeOnlyMarkedPictureViewsAsRenditions() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "picture", "Picture");
        doc = session.createDocument(doc);

        List<RenditionDefinition> availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(7, availableRenditionDefinitions.size());

        runtimeHarness.deployContrib("org.nuxeo.ecm.platform.picture.core",
                "OSGI-INF/imaging-picture-renditions-override.xml");

        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(5, availableRenditionDefinitions.size());
        for (RenditionDefinition definition : availableRenditionDefinitions) {
            assertTrue(EXPECTED_FILTERED_RENDITION_DEFINITION_NAMES.contains(definition.getName()));
        }

        List<Rendition> availableRenditions = renditionService.getAvailableRenditions(doc);
        assertEquals(5, availableRenditions.size());
        availableRenditions = renditionService.getAvailableRenditions(doc, true);
        assertEquals(4, availableRenditions.size());

        runtimeHarness.undeployContrib("org.nuxeo.ecm.platform.picture.core",
                "OSGI-INF/imaging-picture-renditions-override.xml");
    }

    @Test
    public void shouldDeclareRenditionDefinitionImageToPDF() {
        List<RenditionDefinition> renditionDefinitions = renditionService.getDeclaredRenditionDefinitions();
        List<RenditionDefinition> imageToPDFRenditionDefinitions = renditionDefinitions.stream()
            .filter(rD -> rD.getName().equals("imageToPDF")).collect(Collectors.toList());
        assertEquals(1, imageToPDFRenditionDefinitions.size());
        RenditionDefinition imageToPDFRenditionDefinition = imageToPDFRenditionDefinitions.get(0);
        assertEquals("Image.Blob.ConvertToPDF", imageToPDFRenditionDefinition.getOperationChain());
        assertEquals(1, imageToPDFRenditionDefinition.getFilterIds().size());
        assertEquals("hasPictureFacet", imageToPDFRenditionDefinition.getFilterIds().get(0));
    }

    @Test
    public void shouldMakeRenditionAvailableImageToPDF() throws Exception {
        Blob source = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"));
        DocumentModel doc = session.createDocumentModel("/", "picture", "Picture");
        doc.setProperty("file", "content", source);

        Rendition imageToPDFRendition = renditionService.getRendition(doc, "imageToPDF");
        assertNotNull(imageToPDFRendition);
        Blob pdfRendition = imageToPDFRendition.getBlob();
        assertNotNull(pdfRendition);
        assertEquals("pdf", FilenameUtils.getExtension(pdfRendition.getFilename()));
    }
}
