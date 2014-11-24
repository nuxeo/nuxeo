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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.ImagingComponent;
import org.nuxeo.ecm.platform.picture.PictureConversionRegistry;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test the {@link PictureConversionRegistry} class
 *
 * @since 7.1
 */
@Deploy("org.nuxeo.ecm.platform.picture.core")
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
public class TestPictureConversionRegistry {

    private static final String PICTURE_CORE = "org.nuxeo.ecm.platform.picture.core";

    private static final String IMAGING_SERVICE_CONTRIB_COMPONENT = "org.nuxeo.ecm.platform.picture.ImagingComponent.default.config";

    private static final String PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-override-more.xml";

    private static final String PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-conversions-override.xml";

    private static final Log log = LogFactory.getLog(TestPictureConversionRegistry.class);

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Test
    public void iHaveTheImagingServiceContribComponentDeployed() {
        Assert.assertNotNull(Framework.getRuntime().getComponent(
                IMAGING_SERVICE_CONTRIB_COMPONENT));
    }

    @Test
    public void iHaveTheImagingComponentServiceRegistered() {
        Assert.assertNotNull(getImagingComponent());
    }

    @Test
    public void iHaveTheDefaultPictureConversionsRegistered() {
        checkDefaultPictureConversionsPresence();
    }

    protected void checkDefaultPictureConversionsPresence() {
        PictureConversionRegistry pictureConversionRegistry = getPictureConversionRegistry();
        List<String> requiredPictureConversions = pictureConversionRegistry.getDefaultPictureConversions();
        int count = 0;

        for (PictureConversion pictureConversion : pictureConversionRegistry.getPictureConversions()) {
            if (log.isDebugEnabled()) {
                log.debug("Check picture conversion "
                        + pictureConversion.getId());
            }

            if (requiredPictureConversions.contains(pictureConversion.getId())) {
                count++;
            }
        }

        Assert.assertEquals(requiredPictureConversions.size(), count);
    }

    @Test
    public void iHaveDefaultPictureConversionsOrder() {
        String[] defaultPictureConversionsOrder = new String[] { "Medium",
                "Original", "Small", "Thumbnail", "OriginalJpeg" };
        List<PictureConversion> pictureConversions = getPictureConversionRegistry().getPictureConversions();

        for (int i = 0; i < defaultPictureConversionsOrder.length; i++) {
            Assert.assertEquals(defaultPictureConversionsOrder[i],
                    pictureConversions.get(i).getId());
        }
    }

    @Test
    public void iHavePictureConversionsOrder() throws Exception {
        deploy(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        String[] expectedPictureConversionsOrder = new String[] {
                "ThumbnailMini", "Tiny", "Medium", "OriginalJpeg", "Thumbnail",
                "Wide", "Small", "Original", "ThumbnailWide" };
        List<PictureConversion> pictureConversions = getPictureConversionRegistry().getPictureConversions();

        Assert.assertEquals(pictureConversions.size(),
                expectedPictureConversionsOrder.length);

        for (int i = 0; i < expectedPictureConversionsOrder.length; i++) {
            Assert.assertEquals(expectedPictureConversionsOrder[i],
                    pictureConversions.get(i).getId());
        }

        undeploy(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergePictureConversions() throws Exception {
        deploy(PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION);

        checkDefaultPictureConversionsPresence();

        PictureConversionRegistry registry = getPictureConversionRegistry();
        for (PictureConversion pictureConversion : registry.getPictureConversions()) {
            if (pictureConversion.getId().equals("Small")) {
                Assert.assertEquals(50, (int) pictureConversion.getMaxSize());
                Assert.assertTrue(pictureConversion.getDescription().contains(
                        "override"));
            } else if (pictureConversion.getId().equals("Thumbnail")) {
                Assert.assertEquals(320, (int) pictureConversion.getMaxSize());
            } else if (pictureConversion.getId().equals("Medium")) {
                Assert.assertTrue(pictureConversion.getDescription().contains(
                        "override"));
            }
        }

        undeploy(PICTURE_CONVERSIONS_OVERRIDE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergeMorePictureConversions() throws Exception {
        deploy(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);

        checkDefaultPictureConversionsPresence();

        int count = 0;
        List<String> newPictureConversions = Arrays.asList("ThumbnailMini",
                "ThumbnailWide", "Tiny", "Wide");

        PictureConversionRegistry registry = getPictureConversionRegistry();
        for (PictureConversion pictureConversion : registry.getPictureConversions()) {
            if (newPictureConversions.contains(pictureConversion.getId())) {
                count++;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Picture conversion expected: "
                    + newPictureConversions.size());
            log.debug("Picture conversion got: " + count);
        }

        // Assert new picture conversions presence
        Assert.assertEquals(count, newPictureConversions.size());

        // Assert maxSize values

        Assert.assertEquals(96,
                (int) registry.getById("ThumbnailMini").getMaxSize());
        Assert.assertEquals(320,
                (int) registry.getById("ThumbnailWide").getMaxSize());
        Assert.assertEquals(48, (int) registry.getById("Tiny").getMaxSize());
        Assert.assertEquals(2048, (int) registry.getById("Wide").getMaxSize());

        undeploy(PICTURE_CONVERSIONS_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    private void deploy(String component) throws Exception {
        runtimeHarness.deployContrib(PICTURE_CORE, component);
    }

    private void undeploy(String component) throws Exception {
        runtimeHarness.undeployContrib(PICTURE_CORE, component);
    }

    private PictureConversionRegistry getPictureConversionRegistry() {
        return getImagingComponent().getPictureConversionRegistry();
    }

    private ImagingComponent getImagingComponent() {
        return (ImagingComponent) Framework.getRuntime().getComponent(
                ImagingComponent.class.getName());
    }
}
