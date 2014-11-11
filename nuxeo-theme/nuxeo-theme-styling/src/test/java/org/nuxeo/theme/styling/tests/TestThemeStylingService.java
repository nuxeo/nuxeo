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
import org.nuxeo.theme.styling.service.ThemeStylingService;
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
        assertEquals("default", service.getDefaultFlavor(DEFAULT_PAGE_NAME));
        assertEquals("default", service.getDefaultFlavor(PRINT_PAGE_NAME));

        String res = getRenderedCssFileContent("*");
        String expected = getTestFileContent("css_no_flavor_rendering.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering.txt");
        assertEquals(expected, res);
    }

    public void testStylesRegistration() throws Exception {
        checkOriginalTheme();

        // override conf, by adding additional nuxeo_dm_default2 css to the
        // page
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config2.xml");

        assertEquals("default", service.getDefaultFlavor(DEFAULT_PAGE_NAME));
        assertEquals("default", service.getDefaultFlavor(PRINT_PAGE_NAME));

        String res = getRenderedCssFileContent("*");
        String expected = getTestFileContent("css_no_flavor_rendering2.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering2.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering2.txt");
        assertEquals(expected, res);

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

        assertEquals("dark", service.getDefaultFlavor(DEFAULT_PAGE_NAME));
        assertEquals("default", service.getDefaultFlavor(PRINT_PAGE_NAME));

        String res = getRenderedCssFileContent("*");
        String expected = getTestFileContent("css_no_flavor_rendering3.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering3.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering3.txt");
        assertEquals(expected, res);

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

        assertNull(service.getDefaultFlavor(DEFAULT_PAGE_NAME));
        assertNull(service.getDefaultFlavor(PRINT_PAGE_NAME));

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

        // bug from the theme service hot reload management
        // ThemeElement theme = themeManager.getThemeByName("testStyling");
        // assertNull(theme);

        // check generated style
        String res = getRenderedCssFileContent("*");
        String emptyCss = "<style type=\"text/css\">\n"
                + "/* CSS styles for theme 'testStyling' */\n\n" + "</style>";
        assertEquals(emptyCss, res);

        res = getRenderedCssFileContent("default");
        assertEquals(emptyCss, res);

        res = getRenderedCssFileContent("dark");
        assertEquals(emptyCss, res);
    }

}
