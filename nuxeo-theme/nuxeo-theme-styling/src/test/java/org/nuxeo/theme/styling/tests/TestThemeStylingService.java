/*
 * (C) Copyright 2011-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.IconDescriptor;
import org.nuxeo.theme.styling.service.descriptors.LogoDescriptor;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;
import org.nuxeo.theme.styling.service.descriptors.PalettePreview;

/**
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.web.resources.core")
@Deploy("org.nuxeo.theme.styling")
public class TestThemeStylingService {

    public static final String THEME_NAME = "testStyling";

    public static final String DEFAULT_PAGE_NAME = THEME_NAME + "/default";

    public static final String PRINT_PAGE_NAME = THEME_NAME + "/print";

    @Inject
    public ThemeStylingService service;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config.xml")
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-addon-config.xml")
    public void testStylesRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by adding additional nuxeo_dm_default2 css to the
        // page
        hotDeployer.deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config2.xml");

        assertEquals("default", service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        assertEquals("default", service.getDefaultFlavorName(PRINT_PAGE_NAME));

        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        ResourceBundle bundle = wrm.getResourceBundle(PageDescriptor.RESOURCE_BUNDLE_PREFIX + "testStyling_default");
        assertNotNull(bundle);
        assertEquals(4, bundle.getResources().size());
        assertEquals("nuxeo_dm_default.css", bundle.getResources().get(0));
        assertEquals("jquery.fancybox.js", bundle.getResources().get(1));
        assertEquals("nuxeo_dm_default2.css", bundle.getResources().get(2));
        assertEquals("jquery.fancybox.style.css", bundle.getResources().get(3));

        ResourceBundle globalBundle = wrm.getResourceBundle(PageDescriptor.RESOURCE_BUNDLE_PREFIX + "*");
        assertNotNull(globalBundle);
        assertEquals(2, globalBundle.getResources().size());
        assertEquals("addon_style.css", globalBundle.getResources().get(0));
        assertEquals("jquery.addon.js", globalBundle.getResources().get(1));

        hotDeployer.undeploy("org.nuxeo.theme.styling.tests:theme-styling-test-config2.xml");

        checkOriginalTheme();
    }

    @Test
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config.xml")
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-addon-config.xml")
    public void testFlavorsRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by changing dark flavor colors and default flavor
        hotDeployer.deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config3.xml");

        assertEquals("dark", service.getDefaultFlavorName(DEFAULT_PAGE_NAME));

        List<String> flavorNames = service.getFlavorNames(DEFAULT_PAGE_NAME);
        assertNotNull(flavorNames);
        assertEquals(2, flavorNames.size());
        assertEquals("dark", flavorNames.get(0));
        assertEquals("addon_flavor", flavorNames.get(1));

        List<FlavorDescriptor> flavors = service.getFlavors(DEFAULT_PAGE_NAME);
        assertNotNull(flavors);
        assertEquals(2, flavors.size());
        assertEquals("dark", flavors.get(0).getName());
        assertEquals("addon_flavor", flavors.get(1).getName());
        assertEquals("default", service.getDefaultFlavorName(PRINT_PAGE_NAME));

        flavorNames = service.getFlavorNames(PRINT_PAGE_NAME);
        assertNotNull(flavorNames);
        assertEquals("fl: " + flavorNames, 5, flavorNames.size());
        assertEquals("default", flavorNames.get(0));
        assertEquals("dark", flavorNames.get(1));
        assertEquals("subDark", flavorNames.get(2));
        assertEquals("nonExistingFlavor", flavorNames.get(3));
        assertEquals("addon_flavor", flavorNames.get(4));

        flavors = service.getFlavors(PRINT_PAGE_NAME);
        assertNotNull(flavors);
        assertEquals(4, flavors.size());
        assertEquals("default", flavors.get(0).getName());
        assertEquals("dark", flavors.get(1).getName());
        assertEquals("subDark", flavors.get(2).getName());
        assertEquals("addon_flavor", flavors.get(3).getName());
        // non existing flavors are omitted

        FlavorDescriptor flavor = service.getFlavor("*");
        assertNull(flavor);

        LogoDescriptor logo = service.getLogo("*");
        assertNull(logo);

        flavor = service.getFlavor("default");
        assertEquals("default", flavor.getName());
        assertEquals("Default flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_logo.png", logo.getPath());
        assertEquals("92", logo.getWidth());
        assertEquals("36", logo.getHeight());
        assertEquals("Nuxeo", logo.getTitle());

        List<IconDescriptor> icons = flavor.getFavicons();
        assertEquals(2, icons.size());
        assertEquals("icon", icons.get(0).getName());
        assertEquals("/icons/favicon.png", icons.get(0).getValue());
        assertEquals("shortcut icon", icons.get(1).getName());
        assertEquals("/icons/favicon.ico", icons.get(1).getValue());

        flavor = service.getFlavor("dark");
        assertEquals("dark", flavor.getName());
        assertEquals("Dark flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_dark_logo.png", logo.getPath());
        assertEquals("100", logo.getWidth());
        assertEquals("666", logo.getHeight());
        // title merged
        assertEquals("Darxeo", logo.getTitle());

        icons = flavor.getFavicons();
        assertEquals(2, icons.size());
        assertEquals("icon", icons.get(0).getName());
        assertEquals("/icons/dark_favicon.png", icons.get(0).getValue());
        assertEquals("shortcut icon", icons.get(1).getName());
        assertEquals("/icons/dark_favicon.ico", icons.get(1).getValue());

        flavor = service.getFlavor("subDark");
        assertEquals("subDark", flavor.getName());
        assertEquals("SubDark flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_dark_logo.png", logo.getPath());
        assertEquals("100", logo.getWidth());
        assertEquals("666", logo.getHeight());
        // title merged
        assertEquals("Darxeo", logo.getTitle());

        icons = flavor.getFavicons();
        assertEquals(2, icons.size());
        assertEquals("icon", icons.get(0).getName());
        assertEquals("/icons/dark_favicon.png", icons.get(0).getValue());
        assertEquals("shortcut icon", icons.get(1).getName());
        assertEquals("/icons/dark_favicon.ico", icons.get(1).getValue());

        hotDeployer.undeploy("org.nuxeo.theme.styling.tests:theme-styling-test-config3.xml");

        checkOriginalTheme();
    }

    @Test
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config.xml")
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-addon-config.xml")
    public void testPageRegistration() throws Exception {
        PageDescriptor page = service.getPage("testStyling/default");
        assertNotNull(page);
        assertEquals("testStyling/default", page.getName());
        assertEquals("default", page.getDefaultFlavor());
        assertEquals(4, page.getFlavors().size());
        assertEquals("default", page.getFlavors().get(0));
        assertEquals("dark", page.getFlavors().get(1));
        assertEquals("subDark", page.getFlavors().get(2));
        assertEquals("addon_flavor", page.getFlavors().get(3));
        assertEquals(1, page.getResourceBundles().size());
        assertEquals("pageResourceBundle_testStyling_default", page.getResourceBundles().get(0));
        assertEquals(4, page.getResources().size());
        assertEquals("nuxeo_dm_default.css", page.getResources().get(0));
        assertEquals("addon_style.css", page.getResources().get(1));
        assertEquals("jquery.fancybox.js", page.getResources().get(2));
        assertEquals("jquery.addon.js", page.getResources().get(3));
    }

    @Test
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config.xml")
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-addon-config.xml")
    public void testNegotiator() throws Exception {
        assertNull(service.negotiate("foo", null));
        assertEquals("bar", service.negotiate("testNegotiation", null));
    }

    @Test
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-config.xml")
    @Deploy("org.nuxeo.theme.styling.tests:theme-styling-test-addon-config.xml")
    public void testUnregister() throws Exception {
        checkOriginalTheme();

        // undeploy => check web resources manage service status
        hotDeployer.undeploy("org.nuxeo.theme.styling.tests:theme-styling-test-config.xml");

        assertNull(service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        assertNull(service.getFlavorNames(DEFAULT_PAGE_NAME));
        assertNull(service.getFlavors(DEFAULT_PAGE_NAME));

        assertNull(service.getDefaultFlavorName(PRINT_PAGE_NAME));
        assertNull(service.getFlavorNames(PRINT_PAGE_NAME));
        assertNull(service.getFlavors(PRINT_PAGE_NAME));

        // check page is null
        assertNull(service.getPage(DEFAULT_PAGE_NAME));

        // check resources are not registered on resource manager anymore
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        ResourceBundle bundle = wrm.getResourceBundle(PageDescriptor.RESOURCE_BUNDLE_PREFIX + "testStyling_default");
        assertNull(bundle);
        ResourceBundle includes = wrm.getResourceBundle("nuxeo_includes");
        assertNull(includes);

        // check global resources from addon are still there
        ResourceBundle global = wrm.getResourceBundle(PageDescriptor.RESOURCE_BUNDLE_PREFIX + "*");
        assertNotNull(global);
        assertEquals(2, global.getResources().size());
        assertEquals("addon_style.css", global.getResources().get(0));
        assertEquals("jquery.addon.js", global.getResources().get(1));

        // check presets are not registered on service anymore
        Map<String, String> presets = service.getPresetVariables("default");
        assertNotNull(presets);
        assertEquals(0, presets.size());
    }

    protected void checkOriginalTheme() throws Exception {
        assertEquals("default", service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        List<String> flavorNames = service.getFlavorNames(DEFAULT_PAGE_NAME);
        assertNotNull(flavorNames);
        assertEquals(4, flavorNames.size());
        assertEquals("default", flavorNames.get(0));
        assertEquals("dark", flavorNames.get(1));
        assertEquals("subDark", flavorNames.get(2));
        assertEquals("addon_flavor", flavorNames.get(3));
        List<FlavorDescriptor> flavors = service.getFlavors(DEFAULT_PAGE_NAME);
        assertNotNull(flavors);
        assertEquals(4, flavors.size());
        assertEquals("default", flavors.get(0).getName());
        assertEquals("dark", flavors.get(1).getName());
        assertEquals("subDark", flavors.get(2).getName());
        assertEquals("addon_flavor", flavors.get(3).getName());

        assertEquals("default", service.getDefaultFlavorName(PRINT_PAGE_NAME));
        flavorNames = service.getFlavorNames(PRINT_PAGE_NAME);
        assertNotNull(flavorNames);
        assertEquals(3, flavorNames.size());
        assertEquals("default", flavorNames.get(0));
        assertEquals("dark", flavorNames.get(1));
        assertEquals("addon_flavor", flavorNames.get(2));
        flavors = service.getFlavors(PRINT_PAGE_NAME);
        assertNotNull(flavors);
        assertEquals(3, flavors.size());
        assertEquals("default", flavors.get(0).getName());
        assertEquals("dark", flavors.get(1).getName());
        assertEquals("addon_flavor", flavors.get(2).getName());

        FlavorDescriptor flavor = service.getFlavor("*");
        assertNull(flavor);

        PalettePreview pp;
        LogoDescriptor logo = service.getLogo("*");
        assertNull(logo);

        flavor = service.getFlavor("default");
        assertEquals("default", flavor.getName());
        assertEquals("Default flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_logo.png", logo.getPath());
        assertEquals("92", logo.getWidth());
        assertEquals("36", logo.getHeight());
        assertEquals("Nuxeo", logo.getTitle());

        pp = flavor.getPalettePreview();
        assertNotNull(pp);
        assertNotNull(pp.getColors());
        assertEquals(9, pp.getColors().size());
        assertEquals("#cfecff", pp.getColors().get(0));
        assertEquals("#70bbff", pp.getColors().get(1));

        flavor = service.getFlavor("dark");
        assertEquals("dark", flavor.getName());
        assertEquals("Dark flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_dark_logo.png", logo.getPath());
        assertEquals("100", logo.getWidth());
        assertEquals("666", logo.getHeight());
        assertEquals("Dark Nuxeo", logo.getTitle());

        pp = flavor.getPalettePreview();
        assertNull(pp);

        flavor = service.getFlavor("subDark");
        assertEquals("subDark", flavor.getName());
        assertEquals("SubDark flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_dark_logo.png", logo.getPath());
        assertEquals("100", logo.getWidth());
        assertEquals("666", logo.getHeight());
        assertEquals("Dark Nuxeo", logo.getTitle());

        pp = flavor.getPalettePreview();
        assertNull(pp);

        Map<String, String> presets = service.getPresetVariables("default");
        assertNotNull(presets);
        assertEquals(42, presets.size());

        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        ResourceBundle bundle = wrm.getResourceBundle(PageDescriptor.RESOURCE_BUNDLE_PREFIX + "testStyling_default");
        assertNotNull(bundle);
        assertEquals(2, bundle.getResources().size());
        assertEquals("nuxeo_dm_default.css", bundle.getResources().get(0));
        assertEquals("jquery.fancybox.js", bundle.getResources().get(1));

    }

}
