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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 *
 *
 * @since TODO
 */
@Deploy("org.nuxeo.ecm.platform.picture.core")
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
public class PictureTemplateRegistryTest {

    private static final String PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION = "OSGI-INF/imaging-picture-templates-override.xml";

    private final Log log = LogFactory.getLog(getClass());

    @Inject
    private FeaturesRunner runner;

    @Test
    public void iHaveTheImagingComponentRegistered() {
        Assert.assertNotNull(getImagingComponent());
    }

    @Test
    public void iHaveTheDefaultPictureTemplatesRegistered() {
        Set<String> requiredPictureTemplates = getDefaultPictureTemplateTitles();

        for (PictureTemplate pictureTemplate : getPictureTemplates()) {
            Assert.assertTrue(requiredPictureTemplates.contains(pictureTemplate.getTitle()));
        }
    }

    @Test
    public void iCanMergePictureTemplates() throws Exception {
        deployPictureTemplatesOverrideComponent();
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

        /*
         * This templates shouldn't be registered
         */
        Assert.assertNull(registry.getById("Original"));
        Assert.assertNull(registry.getById("OriginalJpeg"));
    }

    /**
     * Deploy the picture templates override component located at the
     * {@link PictureTemplateRegistryTest#PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION}
     * location
     *
     * @throws Exception
     */
    private void deployPictureTemplatesOverrideComponent() throws Exception {
        runner.getFeature(RuntimeFeature.class).getHarness().deployTestContrib(
                "org.nuxeo.ecm.platform.picture.core",
                PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION);
    }

    private Collection<PictureTemplate> getPictureTemplates() {
        return getPictureTemplateRegistry().getPictureTemplates();
    }

    private PictureTemplateRegistry getPictureTemplateRegistry() {
        return getImagingComponent().getPictureTemplateRegistry();
    }

    /**
     * Should matches to the pictureTemplates titles in the component files
     * pointed by the constant
     * {@link PictureTemplateRegistryTest#PICTURE_TEMPLATES_OVERRIDE_COMPONENT_LOCATION}
     *
     * @return
     *
     * @since TODO
     */
    private Set<String> getDefaultPictureTemplateTitles() {
        Set<String> requiredPictureTemplates = new HashSet<String>(5);
        requiredPictureTemplates.add("Original");
        requiredPictureTemplates.add("OriginalJpeg");
        requiredPictureTemplates.add("Medium");
        requiredPictureTemplates.add("Small");
        requiredPictureTemplates.add("Thumbnail");

        return requiredPictureTemplates;
    }

    private ImagingComponent getImagingComponent() {
        return (ImagingComponent) Framework.getRuntime().getComponent(
                ImagingComponent.class.getName());
    }
}
