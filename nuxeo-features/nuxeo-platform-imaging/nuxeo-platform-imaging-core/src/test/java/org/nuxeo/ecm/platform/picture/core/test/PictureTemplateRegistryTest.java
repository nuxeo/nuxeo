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
import java.util.Set;

import javax.inject.Inject;

import junit.framework.Assert;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link PictureTemplateRegistry} class
 *
 * @since 5.9.6
 */
@Deploy("org.nuxeo.ecm.platform.picture.core")
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
public class PictureTemplateRegistryTest {

    private static final String PICTURE_TEMPLATES_OVERRIDE_MORE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-templates-override-more.xml";

    private static final String PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-templates-override.xml";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    RuntimeHarness runtimeHarness;

    @Test
    public void iHaveTheImagingComponentRegistered() {
        Assert.assertNotNull(getImagingComponent());
    }

    @Test
    public void iHaveTheDefaultPictureTemplatesRegistered() {
        checkDefaultPictureTemplatesPresence();
    }

    private void checkDefaultPictureTemplatesPresence() {
        PictureTemplateRegistry pictureTemplateRegistry = getPictureTemplateRegistry();
        Set<String> requiredPictureTemplates = pictureTemplateRegistry.getDefaultPictureTemplates();
        int count = 0;

        for (PictureTemplate pictureTemplate : pictureTemplateRegistry.getPictureTemplates()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Check picture template '{}'",
                        pictureTemplate.getTitle());
            }

            if (requiredPictureTemplates.contains(pictureTemplate.getTitle())) {
                count++;
            }
        }

        Assert.assertEquals(requiredPictureTemplates.size(), count);
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

        if (logger.isDebugEnabled()) {
            logger.debug("Picture template expected: {}",
                    newPictureTemplates.size());
            logger.debug("Picture template got: {}", count);
        }

        // Assert picture templates presence
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
        runtimeHarness.deployContrib("org.nuxeo.ecm.platform.picture.core",
                component);
    }

    private void undeploy(String component) throws Exception {
        runtimeHarness.undeployContrib("org.nuxeo.ecm.platform.picture.core",
                component);
    }

    private PictureTemplateRegistry getPictureTemplateRegistry() {
        return getImagingComponent().getPictureTemplateRegistry();
    }

    private ImagingComponent getImagingComponent() {
        return (ImagingComponent) Framework.getRuntime().getComponent(
                ImagingComponent.class.getName());
    }
}
