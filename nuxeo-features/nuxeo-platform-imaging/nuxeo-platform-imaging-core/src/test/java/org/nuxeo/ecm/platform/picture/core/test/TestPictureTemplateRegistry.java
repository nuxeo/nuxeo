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
import org.nuxeo.ecm.platform.picture.PictureTemplateRegistry;
import org.nuxeo.ecm.platform.picture.api.PictureTemplate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test the {@link PictureTemplateRegistry} class
 *
 * @since 5.9.6
 */
@Deploy("org.nuxeo.ecm.platform.picture.core")
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
public class TestPictureTemplateRegistry {

    private static final String PICTURE_CORE = "org.nuxeo.ecm.platform.picture.core";

    private static final String IMAGING_SERVICE_CONTRIB_COMPONENT = "org.nuxeo.ecm.platform.picture.ImagingComponent.default.config";

    private static final String PICTURE_TEMPLATES_OVERRIDE_MORE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-templates-override-more.xml";

    private static final String PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-templates-override.xml";

    private static final Log log = LogFactory.getLog(TestPictureTemplateRegistry.class);

    @Inject
    RuntimeHarness runtimeHarness;

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
    public void iHaveTheDefaultPictureTemplatesRegistered() {
        checkDefaultPictureTemplatesPresence();
    }

    private void checkDefaultPictureTemplatesPresence() {
        PictureTemplateRegistry pictureTemplateRegistry = getPictureTemplateRegistry();
        List<String> requiredPictureTemplates = pictureTemplateRegistry.getDefaultPictureTemplates();
        int count = 0;

        for (PictureTemplate pictureTemplate : pictureTemplateRegistry.getPictureTemplates()) {
            if (log.isDebugEnabled()) {
                log.debug("Check picture template "
                        + pictureTemplate.getTitle());
            }

            if (requiredPictureTemplates.contains(pictureTemplate.getTitle())) {
                count++;
            }
        }

        Assert.assertEquals(requiredPictureTemplates.size(), count);
    }

    @Test
    public void iHaveDefaultPictureTemplatesOrder() {
        String[] defaultPictureTemplatesOrder = new String[] { "Medium",
                "Original", "Small", "Thumbnail", "OriginalJpeg" };
        List<PictureTemplate> pictureTemplates = getPictureTemplateRegistry().getPictureTemplates();

        for (int i = 0; i < defaultPictureTemplatesOrder.length; i++) {
            Assert.assertEquals(defaultPictureTemplatesOrder[i],
                    pictureTemplates.get(i).getTitle());
        }
    }

    @Test
    public void iHavePictureTemplatesOrder() throws Exception {
        deploy(PICTURE_TEMPLATES_OVERRIDE_MORE_COMPONENT_LOCATION);

        String[] expectedPictureTemplatesOrder = new String[] {
                "ThumbnailMini", "Tiny", "Medium", "OriginalJpeg", "Thumbnail",
                "Wide", "Small", "Original", "ThumbnailWide" };
        List<PictureTemplate> pictureTemplates = getPictureTemplateRegistry().getPictureTemplates();

        Assert.assertEquals(pictureTemplates.size(),
                expectedPictureTemplatesOrder.length);

        for (int i = 0; i < expectedPictureTemplatesOrder.length; i++) {
            Assert.assertEquals(expectedPictureTemplatesOrder[i],
                    pictureTemplates.get(i).getTitle());
        }

        undeploy(PICTURE_TEMPLATES_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergePictureTemplates() throws Exception {
        deploy(PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION);

        checkDefaultPictureTemplatesPresence();

        PictureTemplateRegistry registry = getPictureTemplateRegistry();
        for (PictureTemplate pictureTemplate : registry.getPictureTemplates()) {
            if (pictureTemplate.getTitle().equals("Small")) {
                Assert.assertEquals(50, (int) pictureTemplate.getMaxSize());
                Assert.assertTrue(pictureTemplate.getDescription().contains(
                        "override"));
            } else if (pictureTemplate.getTitle().equals("Thumbnail")) {
                Assert.assertEquals(320, (int) pictureTemplate.getMaxSize());
            } else if (pictureTemplate.getTitle().equals("Medium")) {
                Assert.assertTrue(pictureTemplate.getDescription().contains(
                        "override"));
            }
        }

        undeploy(PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION);
    }

    @Test
    public void iCanMergeMorePictureTemplates() throws Exception {
        deploy(PICTURE_TEMPLATES_OVERRIDE_MORE_COMPONENT_LOCATION);

        checkDefaultPictureTemplatesPresence();

        int count = 0;
        List<String> newPictureTemplates = Arrays.asList("ThumbnailMini",
                "ThumbnailWide", "Tiny", "Wide");

        PictureTemplateRegistry registry = getPictureTemplateRegistry();
        for (PictureTemplate pictureTemplate : registry.getPictureTemplates()) {
            if (newPictureTemplates.contains(pictureTemplate.getTitle())) {
                count++;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Picture template expected: "
                    + newPictureTemplates.size());
            log.debug("Picture template got: " + count);
        }

        // Assert new picture templates presence
        Assert.assertEquals(count, newPictureTemplates.size());

        // Assert maxSize values

        Assert.assertEquals(96,
                (int) registry.getById("ThumbnailMini").getMaxSize());
        Assert.assertEquals(320,
                (int) registry.getById("ThumbnailWide").getMaxSize());
        Assert.assertEquals(48, (int) registry.getById("Tiny").getMaxSize());
        Assert.assertEquals(2048, (int) registry.getById("Wide").getMaxSize());

        undeploy(PICTURE_TEMPLATES_OVERRIDE_MORE_COMPONENT_LOCATION);
    }

    private void deploy(String component) throws Exception {
        runtimeHarness.deployContrib(PICTURE_CORE, component);
    }

    private void undeploy(String component) throws Exception {
        runtimeHarness.undeployContrib(PICTURE_CORE, component);
    }

    private PictureTemplateRegistry getPictureTemplateRegistry() {
        return getImagingComponent().getPictureTemplateRegistry();
    }

    private ImagingComponent getImagingComponent() {
        return (ImagingComponent) Framework.getRuntime().getComponent(
                ImagingComponent.class.getName());
    }
}
