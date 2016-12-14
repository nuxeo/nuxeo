/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test the {@link org.nuxeo.ecm.platform.picture.api.PictureConversion} contributions.
 *
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.actions",
        "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert" })
@LocalDeploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-listeners-override.xml")
public class TestPictureConversions {

    public static final String PICTURE_CORE = "org.nuxeo.ecm.platform.picture.core";

    private static final String PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-override-more.xml";

    private static final String PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-override.xml";

    private static final String PICTURE_CONVERSIONS_FILTERS_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-filters.xml";

    @Inject
    protected CoreSession session;

    @Inject
    protected ImagingService imagingService;

    @Inject
    protected RuntimeHarness runtimeHarness;

    protected List<String> getPictureConversionIds() {
        List<String> ids = new ArrayList<>();
        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            ids.add(pictureConversion.getId());
        }
        return ids;
    }

    @Test
    public void iHaveDefaultPictureConversionsOrder() {
        String[] defaultPictureConversionsOrder = new String[] { "Thumbnail", "Small", "Medium", "FullHD", "OriginalJpeg" };
        List<PictureConversion> pictureConversions = imagingService.getPictureConversions();

        for (int i = 0; i < defaultPictureConversionsOrder.length; i++) {
            assertEquals(defaultPictureConversionsOrder[i], pictureConversions.get(i).getId());
        }
    }

    @Test
    public void iHavePictureConversionsOrder() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        String[] expectedPictureConversionsOrder = new String[] { "ThumbnailMini", "Tiny", "Thumbnail",
                "Wide", "ThumbnailWide", "Small", "Medium", "FullHD" };
        List<PictureConversion> pictureConversions = imagingService.getPictureConversions();

        assertEquals(expectedPictureConversionsOrder.length, pictureConversions.size());

        for (int i = 0; i < expectedPictureConversionsOrder.length; i++) {
            assertEquals(expectedPictureConversionsOrder[i], pictureConversions.get(i).getId());
        }

        undeployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergePictureConversions() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION);

        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            switch (pictureConversion.getId()) {
            case "Original":
            case "OriginalJpeg":
                assertFalse(pictureConversion.getId(), pictureConversion.isEnabled());
                break;
            case "Small":
                assertEquals(50, (int) pictureConversion.getMaxSize());
                assertTrue(pictureConversion.getDescription().contains("override"));
                break;
            case "Thumbnail":
                assertEquals(320, (int) pictureConversion.getMaxSize());
                break;
            case "Medium":
                assertTrue(pictureConversion.getDescription().contains("override"));
                break;
            }
        }

        undeployContrib(PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergeMorePictureConversions() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        int count = 0;
        List<String> newPictureConversions = Arrays.asList("ThumbnailMini", "ThumbnailWide", "Tiny", "Wide");

        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            if (newPictureConversions.contains(pictureConversion.getId())) {
                count++;
            }
        }

        // Assert new picture conversions presence
        assertEquals(newPictureConversions.size(), count);

        // Assert maxSize values
        assertEquals(96, (int) imagingService.getPictureConversion("ThumbnailMini").getMaxSize());
        assertEquals(320, (int) imagingService.getPictureConversion("ThumbnailWide").getMaxSize());
        assertEquals(48, (int) imagingService.getPictureConversion("Tiny").getMaxSize());
        assertEquals(2048, (int) imagingService.getPictureConversion("Wide").getMaxSize());

        undeployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    @Test
    public void shouldFilterPictureConversions() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_FILTERS_COMPONENT_LOCATION);

        DocumentModel picture = session.createDocumentModel("/", "picture", "Picture");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"));
        blob.setFilename("MyTest.jpg");
        blob.setMimeType("image/jpeg");
        picture.setPropertyValue("file:content", (Serializable) blob);
        picture = session.createDocument(picture);

        MultiviewPicture multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        assertEquals(5, multiviewPicture.getViews().length);
        PictureView smallView = multiviewPicture.getView("Small");
        ImageInfo imageInfo = smallView.getImageInfo();
        assertNotNull(imageInfo);
        assertTrue(imageInfo.getWidth() > 0);
        assertTrue(imageInfo.getHeight() > 0);
        assertTrue(imageInfo.getDepth() > 0);
        assertNotNull(imageInfo.getFormat());
        assertNotNull(imageInfo.getColorSpace());

        // trigger 2 new conversions
        picture.setPropertyValue("file:content", (Serializable) blob);
        picture.setPropertyValue("dc:source", "Small");
        picture = session.saveDocument(picture);

        multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        assertEquals(7, multiviewPicture.getViews().length);
        assertNotNull(multiviewPicture.getView("smallConversion"));
        assertNotNull(multiviewPicture.getView("anotherSmallConversion"));

        // block the 'anotherSmallConversion' conversion
        picture.setPropertyValue("file:content", (Serializable) blob);
        picture.setPropertyValue("dc:rights", "Unauthorized");
        picture = session.saveDocument(picture);

        multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        assertEquals(6, multiviewPicture.getViews().length);
        assertNotNull(multiviewPicture.getView("smallConversion"));
        assertNull(multiviewPicture.getView("anotherSmallConversion"));

        undeployContrib(PICTURE_CONVERSIONS_FILTERS_COMPONENT_LOCATION);
    }

    private void deployContrib(String component) throws Exception {
        runtimeHarness.deployContrib(PICTURE_CORE, component);
    }

    private void undeployContrib(String component) throws Exception {
        runtimeHarness.undeployContrib(PICTURE_CORE, component);
    }

}
