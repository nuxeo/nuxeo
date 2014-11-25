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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test the {@link org.nuxeo.ecm.platform.picture.api.PictureConversion}
 * contributions.
 *
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy({ TestPictureConversions.PICTURE_CORE })
public class TestPictureConversions {

    public static final String PICTURE_CORE = "org.nuxeo.ecm.platform.picture.core";

    private static final String PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-override-more.xml";

    private static final String PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-override.xml";

    protected static final List<String> DEFAULT_PICTURE_CONVERSIONS = Arrays.asList(
            "Small", "Medium", "Original", "Thumbnail", "OriginalJpeg");

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected ImagingService imagingService;

    @Test
    public void iHaveTheDefaultPictureConversionsRegistered() {
        checkDefaultPictureConversionsPresence();
    }

    protected void checkDefaultPictureConversionsPresence() {
        List<String> pictureConversionIds = getPictureConversionIds();
        assertTrue(pictureConversionIds.containsAll(DEFAULT_PICTURE_CONVERSIONS));
    }

    @Test
    public void iHaveDefaultPictureConversionsOrder() {
        String[] defaultPictureConversionsOrder = new String[] { "Medium",
                "Original", "Small", "Thumbnail", "OriginalJpeg" };
        List<PictureConversion> pictureConversions = imagingService.getPictureConversions();

        for (int i = 0; i < defaultPictureConversionsOrder.length; i++) {
            assertEquals(defaultPictureConversionsOrder[i],
                    pictureConversions.get(i).getId());
        }
    }

    @Test
    public void iHavePictureConversionsOrder() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        String[] expectedPictureConversionsOrder = new String[] {
                "ThumbnailMini", "Tiny", "Medium", "OriginalJpeg", "Thumbnail",
                "Wide", "Small", "Original", "ThumbnailWide" };
        List<PictureConversion> pictureConversions = imagingService.getPictureConversions();

        assertEquals(expectedPictureConversionsOrder.length,
                pictureConversions.size());

        for (int i = 0; i < expectedPictureConversionsOrder.length; i++) {
            assertEquals(expectedPictureConversionsOrder[i],
                    pictureConversions.get(i).getId());
        }

        undeployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergePictureConversions() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION);

        checkDefaultPictureConversionsPresence();

        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            switch (pictureConversion.getId()) {
            case "Small":
                assertEquals(50, (int) pictureConversion.getMaxSize());
                Assert.assertTrue(pictureConversion.getDescription().contains(
                        "override"));
                break;
            case "Thumbnail":
                assertEquals(320, (int) pictureConversion.getMaxSize());
                break;
            case "Medium":
                Assert.assertTrue(pictureConversion.getDescription().contains(
                        "override"));
                break;
            }
        }

        undeployContrib(PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergeMorePictureConversions() throws Exception {
        deployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        checkDefaultPictureConversionsPresence();

        int count = 0;
        List<String> newPictureConversions = Arrays.asList("ThumbnailMini",
                "ThumbnailWide", "Tiny", "Wide");

        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            if (newPictureConversions.contains(pictureConversion.getId())) {
                count++;
            }
        }

        // Assert new picture conversions presence
        assertEquals(newPictureConversions.size(), count);

        // Assert maxSize values
        assertEquals(
                96,
                (int) imagingService.getPictureConversion("ThumbnailMini").getMaxSize());
        assertEquals(
                320,
                (int) imagingService.getPictureConversion("ThumbnailWide").getMaxSize());
        assertEquals(48,
                (int) imagingService.getPictureConversion("Tiny").getMaxSize());
        assertEquals(2048,
                (int) imagingService.getPictureConversion("Wide").getMaxSize());

        undeployContrib(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    protected List<String> getPictureConversionIds() {
        List<String> ids = new ArrayList<>();
        for (PictureConversion pictureConversion : imagingService.getPictureConversions()) {
            ids.add(pictureConversion.getId());
        }
        return ids;
    }

    private void deployContrib(String component) throws Exception {
        runtimeHarness.deployContrib(PICTURE_CORE, component);
    }

    private void undeployContrib(String component) throws Exception {
        runtimeHarness.undeployContrib(PICTURE_CORE, component);
    }

}
