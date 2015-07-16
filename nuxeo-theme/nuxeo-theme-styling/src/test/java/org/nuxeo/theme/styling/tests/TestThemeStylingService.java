/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.tests;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.Logo;
import org.nuxeo.theme.styling.service.descriptors.Page;
import org.nuxeo.theme.styling.service.descriptors.PalettePreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @since 5.5
 */
public class TestThemeStylingService extends NXRuntimeTestCase {

    public static final String THEME_NAME = "testStyling";

    public static final String DEFAULT_PAGE_NAME = THEME_NAME + "/default";

    public static final String PRINT_PAGE_NAME = THEME_NAME + "/print";

    protected ThemeStylingService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.web.resources.core");
        deployBundle("org.nuxeo.theme.styling");
        deployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-config.xml");
        deployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-addon-config.xml");

        // force application start
        fireFrameworkStarted();

        service = Framework.getService(ThemeStylingService.class);
        assertNotNull(service);
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
        List<Flavor> flavors = service.getFlavors(DEFAULT_PAGE_NAME);
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

        Flavor flavor = service.getFlavor("*");
        assertNull(flavor);

        PalettePreview pp;
        Logo logo = service.getLogo("*");
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
        ResourceBundle bundle = wrm.getResourceBundle(Page.RESOURCE_BUNDLE_PREFIX + "testStyling_default");
        assertNotNull(bundle);
        assertEquals(2, bundle.getResources().size());
        assertEquals("nuxeo_dm_default.css", bundle.getResources().get(0));
        assertEquals("jquery.fancybox.js", bundle.getResources().get(1));

    }

    @Test
    public void testStylesRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by adding additional nuxeo_dm_default2 css to the
        // page
        deployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-config2.xml");

        assertEquals("default", service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        assertEquals("default", service.getDefaultFlavorName(PRINT_PAGE_NAME));

        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        ResourceBundle bundle = wrm.getResourceBundle(Page.RESOURCE_BUNDLE_PREFIX + "testStyling_default");
        assertNotNull(bundle);
        assertEquals(4, bundle.getResources().size());
        assertEquals("nuxeo_dm_default.css", bundle.getResources().get(0));
        assertEquals("jquery.fancybox.js", bundle.getResources().get(1));
        assertEquals("nuxeo_dm_default2.css", bundle.getResources().get(2));
        assertEquals("jquery.fancybox.style.css", bundle.getResources().get(3));

        ResourceBundle globalBundle = wrm.getResourceBundle(Page.RESOURCE_BUNDLE_PREFIX + "*");
        assertNotNull(globalBundle);
        assertEquals(2, globalBundle.getResources().size());
        assertEquals("addon_style.css", globalBundle.getResources().get(0));
        assertEquals("jquery.addon.js", globalBundle.getResources().get(1));

        // undeploy, check theme styling is back to first definition
        undeployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-config2.xml");

        checkOriginalTheme();
    }

    @Test
    public void testFlavorsRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by changing dark flavor colors and default flavor
        deployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-config3.xml");

        assertEquals("dark", service.getDefaultFlavorName(DEFAULT_PAGE_NAME));

        List<String> flavorNames = service.getFlavorNames(DEFAULT_PAGE_NAME);
        assertNotNull(flavorNames);
        assertEquals(2, flavorNames.size());
        assertEquals("dark", flavorNames.get(0));
        assertEquals("addon_flavor", flavorNames.get(1));

        List<Flavor> flavors = service.getFlavors(DEFAULT_PAGE_NAME);
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

        Flavor flavor = service.getFlavor("*");
        assertNull(flavor);

        Logo logo = service.getLogo("*");
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

        // undeploy, check theme styling is back to first definition
        undeployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-config3.xml");
        checkOriginalTheme();
    }

    @Test
    public void testUnregister() throws Exception {
        checkOriginalTheme();

        // undeploy => check web resources manage service status
        undeployContrib("org.nuxeo.theme.styling.tests", "theme-styling-test-config.xml");

        assertNull(service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        assertNull(service.getFlavorNames(DEFAULT_PAGE_NAME));
        assertNull(service.getFlavors(DEFAULT_PAGE_NAME));

        assertNull(service.getDefaultFlavorName(PRINT_PAGE_NAME));
        assertNull(service.getFlavorNames(PRINT_PAGE_NAME));
        assertNull(service.getFlavors(PRINT_PAGE_NAME));

        // check page is null
        Page page = service.getPage(DEFAULT_PAGE_NAME);
        assertNull(page);

        // check resources are not registered on resource manager anymore
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        ResourceBundle bundle = wrm.getResourceBundle(Page.RESOURCE_BUNDLE_PREFIX + "testStyling_default");
        assertNull(bundle);
        ResourceBundle includes = wrm.getResourceBundle("nuxeo_includes");
        assertNull(includes);

        // check global resources from addon are still there
        ResourceBundle global = wrm.getResourceBundle(Page.RESOURCE_BUNDLE_PREFIX + "*");
        assertNotNull(global);
        assertEquals(2, global.getResources().size());
        assertEquals("addon_style.css", global.getResources().get(0));
        assertEquals("jquery.addon.js", global.getResources().get(1));

        // check presets are not registered on service anymore
        Map<String, String> presets = service.getPresetVariables("default");
        assertNotNull(presets);
        assertEquals(0, presets.size());
    }

    @Test
    public void testPageRegistration() throws Exception {
        Page page = service.getPage("testStyling/default");
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
        assertEquals(2, page.getFavicons().size());
        assertEquals("icon", page.getFavicons().get(0).getName());
        assertEquals("/nuxeo/icons/favicon.png", page.getFavicons().get(0).getValue());
        assertEquals("shortcut icon", page.getFavicons().get(1).getName());
        assertEquals("/nuxeo/icons/favicon.ico", page.getFavicons().get(1).getValue());
    }

    @Test
    public void testNegotiator() throws Exception {
        assertNull(service.negotiate("foo", null));
        assertEquals("bar", service.negotiate("testNegotiation", null));
    }

}
