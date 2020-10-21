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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.RENDER_VIEWS_PROPERTY;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.TRANSMISSIONS_PROPERTY;
import static org.nuxeo.ecm.platform.threed.rendition.ThreeDRenditionDefinitionProvider.THREED_RENDER_VIEW_RENDITION_KIND;
import static org.nuxeo.ecm.platform.threed.rendition.ThreeDRenditionDefinitionProvider.THREED_TRANSMISSION_RENDITION_KIND;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.threed.BatchConverterHelper;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.ThreeDRenderView;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test 3D renditions
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.threed")
@Deploy("org.nuxeo.ecm.platform.threed.test:OSGI-INF/threed-service-contrib-override.xml")
@Deploy("org.nuxeo.ecm.platform.threed.test:OSGI-INF/threed-disable-listeners-contrib.xml")
@ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class)
public class TestThreeDRenditions {

    public static final List<String> OVERRIDDEN_RENDITION_DEFINITION_NAMES = Arrays.asList("mini_top", "mini_left",
            "mini_front", "geo_100_tex_100", "geo_100_tex_050", "geo_050_tex_050", "geo_025_tex_025", "not_visible");

    public static final List<String> THREED_RENDITION_DEFINITION_KINDS = Arrays.asList(
            THREED_RENDER_VIEW_RENDITION_KIND, THREED_TRANSMISSION_RENDITION_KIND);

    protected static final String TEST_MODEL = "suzanne";

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Inject
    protected ThreeDService threeDService;

    private static final Log log = LogFactory.getLog(TestThreeDRenditions.class);

    protected void updateThreeDDocument(DocumentModel doc, ThreeD threeD) {
        BlobHolder results = threeDService.batchConvert(threeD);

        List<BlobHolder> resources = BatchConverterHelper.getResources(results);
        List<TransmissionThreeD> colladaThreeDs = BatchConverterHelper.getTransmissions(results, resources);
        List<TransmissionThreeD> transmissionThreeDs = colladaThreeDs.stream()
                                                                     .map(threeDService::convertColladaToglTF)
                                                                     .collect(Collectors.toList());

        List<Map<String, Serializable>> transmissionList = transmissionThreeDs.stream()
                                                                              .map(TransmissionThreeD::toMap)
                                                                              .collect(Collectors.toList());
        doc.setPropertyValue(TRANSMISSIONS_PROPERTY, (Serializable) transmissionList);

        List<ThreeDRenderView> threeDRenderViews = BatchConverterHelper.getRenders(results);
        List<Map<String, Serializable>> renderViewList = threeDRenderViews.stream()
                                                                          .map(ThreeDRenderView::toMap)
                                                                          .collect(Collectors.toList());
        doc.setPropertyValue(RENDER_VIEWS_PROPERTY, (Serializable) renderViewList);

    }

    protected List<RenditionDefinition> getThreeDRenditionDefinitions(DocumentModel doc) {
        return renditionService.getAvailableRenditionDefinitions(doc)
                               .stream()
                               .filter(renditionDefinition -> THREED_RENDITION_DEFINITION_KINDS.contains(
                                       renditionDefinition.getKind()))
                               .collect(Collectors.toList());
    }

    protected List<Rendition> getThreeDAvailableRenditions(DocumentModel doc, boolean onlyVisible) {
        return renditionService.getAvailableRenditions(doc, onlyVisible)
                               .stream()
                               .filter(rendition -> THREED_RENDITION_DEFINITION_KINDS.contains(rendition.getKind()))
                               .collect(Collectors.toList());
    }

    protected static ThreeD getTestThreeD() throws IOException {
        List<Blob> resources = new ArrayList<>();
        Blob blob, main;
        try (InputStream is = TestThreeDRenditions.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".obj")) {
            assertNotNull(String.format("Failed to load resource: %s.obj", TEST_MODEL), is);
            main = Blobs.createBlob(is);
            main.setFilename(TEST_MODEL + ".obj");

        }

        try (InputStream is = TestThreeDRenditions.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".mtl")) {
            assertNotNull(String.format("Failed to load resource: %s.mtl", TEST_MODEL), is);
            blob = Blobs.createBlob(is);
            blob.setFilename(TEST_MODEL + ".mtl");
            resources.add(blob);
        }
        return new ThreeD(main, resources, null);
    }

    @Test
    public void shouldExposeOnlyExposedAsRenditions() throws Exception {
        ThreeD threeD = getTestThreeD();
        DocumentModel doc = session.createDocumentModel("/", "threed", "ThreeD");
        doc = session.createDocument(doc);

        assertEquals(0, getThreeDRenditionDefinitions(doc).size());
        Date timeBefore = new Date();
        updateThreeDDocument(doc, threeD);

        List<RenditionDefinition> renditionDefinitions = getThreeDRenditionDefinitions(doc);
        long timeDelta = (new Date()).getTime() - timeBefore.getTime();
        if (renditionDefinitions.size() == 0) {
            log.warn(String.format("[NXP-21450] memory max: %dMB", Runtime.getRuntime().maxMemory() / 1024 / 1024));
            log.warn(String.format("[NXP-21450] memory total: %dMB", Runtime.getRuntime().totalMemory() / 1024 / 1024));
            log.warn(String.format("[NXP-21450] memory free: %dMB", Runtime.getRuntime().freeMemory() / 1024 / 1024));
            log.warn(String.format("[NXP-21450] duration: %dms", timeDelta));
        }
        assertEquals(8, renditionDefinitions.size());
        for (RenditionDefinition definition : renditionDefinitions) {
            assertTrue(OVERRIDDEN_RENDITION_DEFINITION_NAMES.contains(definition.getName()));
        }

        List<Rendition> availableRenditions = getThreeDAvailableRenditions(doc, false);
        assertEquals(8, availableRenditions.size());
        // they are all visible but one
        availableRenditions = getThreeDAvailableRenditions(doc, true);
        assertEquals(7, availableRenditions.size());

    }

    @Test
    public void testBatchConverterHelper() throws Exception {
        ThreeD threeD = getTestThreeD();
        BlobHolder results = threeDService.batchConvert(threeD);
        List<ThreeDRenderView> renderviews = BatchConverterHelper.getRenders(results);
        List<BlobHolder> resources = BatchConverterHelper.getResources(results);
        List<TransmissionThreeD> transmissions = BatchConverterHelper.getTransmissions(results, resources);
        for (ThreeDRenderView rV : renderviews) {
            assertEquals(1,
                    threeDService.getAutomaticRenderViews()
                                 .stream()
                                 .filter(aRV -> aRV.getName().equals(rV.getTitle()))
                                 .count());
        }
        for (TransmissionThreeD tTD : transmissions) {
            assertEquals(1,
                    threeDService.getAvailableLODs()
                                 .stream()
                                 .filter(aLOD -> aLOD.getName().equals(tTD.getName()))
                                 .count());
        }
    }

}
