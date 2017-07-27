/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
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

    protected static final List<String> DEFAULT_PICTURE_CONVERSIONS = Arrays.asList("Thumbnail", "Small", "Medium",
            "OriginalJpeg");

    protected static final List<String> DEFAULT_PICTURE_CONVERSIONS_WITHOUT_ORIGINAL_JPEG = Arrays.asList("Thumbnail",
            "Small", "Medium");

    @Inject
    protected CoreSession session;

    @Inject
    protected ImagingService imagingService;

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected EventService eventService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void iHaveTheDefaultPictureConversionsRegistered() {
        checkPictureConversionsPresence(DEFAULT_PICTURE_CONVERSIONS);
    }

    protected void checkPictureConversionsPresence(List<String> pictureConversions) {
        List<String> pictureConversionIds = getPictureConversionIds();
        org.junit.Assert.assertTrue(pictureConversionIds.containsAll(pictureConversions));
    }

    protected List<String> getPictureConversionIds() {
        List<String> ids = new ArrayList<>();
        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            ids.add(pictureConversion.getId());
        }
        return ids;
    }

    @Test
    public void iHaveDefaultPictureConversionsOrder() {
        String[] defaultPictureConversionsOrder = new String[] { "Thumbnail", "Small", "Medium", "OriginalJpeg" };
        List<PictureConversion> pictureConversions = imagingService.getPictureConversions();

        for (int i = 0; i < defaultPictureConversionsOrder.length; i++) {
            assertEquals(defaultPictureConversionsOrder[i], pictureConversions.get(i).getId());
        }
    }

    @Test
    public void iHavePictureConversionsOrder() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        String[] expectedPictureConversionsOrder = new String[] { "ThumbnailMini", "Tiny", "OriginalJpeg", "Thumbnail",
                "Wide", "ThumbnailWide", "Small", "Medium" };
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

        checkPictureConversionsPresence(DEFAULT_PICTURE_CONVERSIONS_WITHOUT_ORIGINAL_JPEG);

        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            switch (pictureConversion.getId()) {
            case "Original":
                assertTrue(pictureConversion.getId(), pictureConversion.isEnabled());
                break;
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

        checkPictureConversionsPresence(DEFAULT_PICTURE_CONVERSIONS);

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
        assertEquals(4, multiviewPicture.getViews().length);
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
        assertEquals(6, multiviewPicture.getViews().length);
        assertNotNull(multiviewPicture.getView("smallConversion"));
        assertNotNull(multiviewPicture.getView("anotherSmallConversion"));

        // block the 'anotherSmallConversion' conversion
        picture.setPropertyValue("file:content", (Serializable) blob);
        picture.setPropertyValue("dc:rights", "Unauthorized");
        picture = session.saveDocument(picture);

        multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        assertEquals(5, multiviewPicture.getViews().length);
        assertNotNull(multiviewPicture.getView("smallConversion"));
        assertNull(multiviewPicture.getView("anotherSmallConversion"));

        undeployContrib(PICTURE_CONVERSIONS_FILTERS_COMPONENT_LOCATION);
    }

    @Test
    public void pictureConversionsAlwaysHaveExtensions() throws IOException {
        DocumentModel picture = session.createDocumentModel("/", "picture", "Picture");
        // Use a small image so the biggest conversions will have the same result and it will be fetched from the cache
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/cat.gif"));
        blob.setFilename("Cat.gif");
        blob.setMimeType("image/gif");
        picture.setPropertyValue("file:content", (Serializable) blob);
        picture = session.createDocument(picture);
        txFeature.nextTransaction();

        // Wait for the end of all the async works
        eventService.waitForAsyncCompletion();

        // Fetch the picture views
        MultiviewPicture multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        assertEquals(4, multiviewPicture.getViews().length);
        for (PictureView pictureView : multiviewPicture.getViews()) {
            assertEquals("jpg", FilenameUtils.getExtension(pictureView.getFilename()));
            assertTrue(StringUtils.containsIgnoreCase(pictureView.getFilename(), "cat"));
        }
    }

    private void deployContrib(String component) throws Exception {
        runtimeHarness.deployContrib(PICTURE_CORE, component);
    }

    private void undeployContrib(String component) throws Exception {
        runtimeHarness.undeployContrib(PICTURE_CORE, component);
    }

}
