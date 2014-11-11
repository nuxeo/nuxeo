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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.html.ui.ThemeStyles;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.Logo;
import org.nuxeo.theme.styling.service.descriptors.PalettePreview;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

/**
 * @since 5.5
 */
public class TestThemeStylingService extends NXRuntimeTestCase {

    public static final String THEME_NAME = "testStyling";

    public static final String DEFAULT_PAGE_NAME = THEME_NAME + "/default";

    public static final String PRINT_PAGE_NAME = THEME_NAME + "/print";

    public static final String THEME_DEFAULT_URL = "nxtheme://theme/default/*/jsf-facelets/testStyling/default/default/default";

    protected ThemeStylingService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.theme.core");
        deployBundle("org.nuxeo.theme.fragments");
        deployBundle("org.nuxeo.theme.styling");
        deployContrib("org.nuxeo.theme.styling.tests", "theme-test-config.xml");
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config.xml");
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-addon-config.xml");

        // force application start
        fireFrameworkStarted();

        service = Framework.getService(ThemeStylingService.class);
        assertNotNull(service);
    }

    protected String getRenderedCssFileContent(String collection) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("themeName", THEME_NAME);
        params.put("basepath", "/nuxeo/myBasePath");
        params.put("collection", collection);
        params.put("includeDate", "false");
        return ThemeStyles.render(params, false, true, false);
    }

    protected static String getTestFileContent(String filePath)
            throws Exception {
        return getTestFileContent(filePath, true);
    }

    protected static String getTestFileContent(String filePath,
            boolean checkStyles) throws Exception {
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext(filePath));
        String content = FileUtils.read(expected);
        // replacements needed for generated ids
        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(THEME_NAME, "style",
                DEFAULT_PAGE_NAME + ThemeStylingService.PAGE_STYLE_NAME_SUFFIX);
        if (checkStyles) {
            assertNotNull(style);
        }
        if (style != null) {
            content = content.replace("${default_suid}",
                    CSSUtils.computeCssClassName(style));
        }
        style = (Style) themeManager.getNamedObject(THEME_NAME, "style",
                PRINT_PAGE_NAME + ThemeStylingService.PAGE_STYLE_NAME_SUFFIX);
        if (checkStyles) {
            assertNotNull(style);
        }
        if (style != null) {
            content = content.replace("${print_suid}",
                    CSSUtils.computeCssClassName(style));
        }
        return content;
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

        String res = getRenderedCssFileContent("*");
        String expected = getTestFileContent("css_no_flavor_rendering.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        Flavor flavor = service.getFlavor("*");
        assertNull(flavor);

        PalettePreview pp;
        Logo logo = service.getLogo("*");
        assertNull(logo);

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

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

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

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

        res = getRenderedCssFileContent("subDark");
        expected = getTestFileContent("css_sub_dark_rendering.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

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

        ResourceManager rm = Manager.getResourceManager();
        List<String> resources = rm.getGlobalResourcesFor(THEME_DEFAULT_URL);
        assertNotNull(resources);
        assertEquals(2, resources.size());
        assertEquals("jquery.fancybox.js", resources.get(0));
        assertEquals("jquery.addon.js", resources.get(1));
    }

    public void testStylesRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by adding additional nuxeo_dm_default2 css to the
        // page
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config2.xml");

        assertEquals("default", service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        assertEquals("default", service.getDefaultFlavorName(PRINT_PAGE_NAME));

        String res = getRenderedCssFileContent("*");
        String expected = getTestFileContent("css_no_flavor_rendering2.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering2.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering2.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        res = getRenderedCssFileContent("subDark");
        expected = getTestFileContent("css_sub_dark_rendering2.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        ResourceManager rm = Manager.getResourceManager();
        List<String> resources = rm.getGlobalResourcesFor(THEME_DEFAULT_URL);
        assertNotNull(resources);
        assertEquals(3, resources.size());
        assertEquals("jquery.fancybox.js", resources.get(0));
        assertEquals("jquery.fancybox.style.css", resources.get(1));
        assertEquals("jquery.addon.js", resources.get(2));

        // undeploy, check theme styling is back to first definition
        undeployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config2.xml");

        checkOriginalTheme();
    }

    public void testFlavorsRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by changing dark flavor colors and default flavor
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config3.xml");

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

        String res = getRenderedCssFileContent("*");
        String expected = getTestFileContent("css_no_flavor_rendering3.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        Flavor flavor = service.getFlavor("*");
        assertNull(flavor);

        Logo logo = service.getLogo("*");
        assertNull(logo);

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering3.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

        flavor = service.getFlavor("default");
        assertEquals("default", flavor.getName());
        assertEquals("Default flavor", flavor.getLabel());

        logo = flavor.getLogo();
        assertNotNull(logo);
        assertEquals("/img/nuxeo_logo.png", logo.getPath());
        assertEquals("92", logo.getWidth());
        assertEquals("36", logo.getHeight());
        assertEquals("Nuxeo", logo.getTitle());

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering3.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

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

        res = getRenderedCssFileContent("subDark");
        // no change wrt to dark as the same property is overriden with same
        // value
        expected = getTestFileContent("css_dark_rendering3.txt").trim();
        assertTrue(FileUtils.areFilesContentEquals(expected, res));

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
        undeployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config3.xml");
        checkOriginalTheme();
    }

    public void testUnregister() throws Exception {
        checkOriginalTheme();

        // undeploy => check theme service status
        undeployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config.xml");

        assertNull(service.getDefaultFlavorName(DEFAULT_PAGE_NAME));
        assertNull(service.getFlavorNames(DEFAULT_PAGE_NAME));
        assertNull(service.getFlavors(DEFAULT_PAGE_NAME));

        assertNull(service.getDefaultFlavorName(PRINT_PAGE_NAME));
        assertNull(service.getFlavorNames(PRINT_PAGE_NAME));
        assertNull(service.getFlavors(PRINT_PAGE_NAME));

        // check styles are not registered on theme service anymore
        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(THEME_NAME, "style",
                DEFAULT_PAGE_NAME + ThemeStylingService.PAGE_STYLE_NAME_SUFFIX);
        assertNull(style);

        style = (Style) themeManager.getNamedObject(THEME_NAME, "style",
                PRINT_PAGE_NAME + ThemeStylingService.PAGE_STYLE_NAME_SUFFIX);
        assertNull(style);

        // check presets are not registered on theme service anymore
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        List<Type> registeredPresets = typeRegistry.getTypes(TypeFamily.PRESET);
        assertEquals(0, registeredPresets.size());

        // check resources are not registered on resource manager anymore
        ResourceManager rm = Manager.getResourceManager();
        List<String> resources = rm.getGlobalResourcesFor(THEME_DEFAULT_URL);
        assertNotNull(resources);
        assertEquals(0, resources.size());

        // bug from the theme service hot reload management
        // ThemeElement theme = themeManager.getThemeByName("testStyling");
        // assertNull(theme);

        // check generated style
        String res = getRenderedCssFileContent("*");
        String emptyCss = "<style type=\"text/css\">\n"
                + "/* CSS styles for theme 'testStyling' */\n\n" + "</style>";
        assertEquals(emptyCss, res);
        assertNull(service.getFlavor("*"));
        assertNull(service.getLogo("*"));

        res = getRenderedCssFileContent("default");
        assertEquals(emptyCss, res);
        assertNull(service.getFlavor("default"));
        assertNull(service.getLogo("default"));

        res = getRenderedCssFileContent("dark");
        assertEquals(emptyCss, res);
        assertNull(service.getFlavor("dark"));
        assertNull(service.getLogo("dark"));

        res = getRenderedCssFileContent("subDark");
        assertEquals(emptyCss, res);
        assertNull(service.getFlavor("subDark"));
        assertNull(service.getLogo("subDark"));
    }

}
