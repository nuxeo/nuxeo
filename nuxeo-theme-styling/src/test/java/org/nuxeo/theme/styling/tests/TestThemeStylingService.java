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
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.html.ui.ThemeStyles;
import org.nuxeo.theme.themes.ThemeManager;

/**
 * @since 5.4.3
 */
public class TestThemeStylingService extends NXRuntimeTestCase {

    public static final String THEME_NAME = "testStyling";

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
    }

    protected String getRenderedCssFileContent(String collection) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("themeName", THEME_NAME);
        params.put("path", "myPath");
        params.put("basePath", "myBasePath");
        params.put("collection", collection);
        params.put("includeDate", "false");
        return ThemeStyles.render(params, false, true, false);
    }

    protected static String getTestFileContent(String filePath)
            throws Exception {
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext(filePath));
        String content = FileUtils.read(expected);
        // replacements needed for generated ids
        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(THEME_NAME, "style",
                THEME_NAME + "/default Page Styles");
        assertNotNull(style);
        content = content.replace("${default_suid}",
                CSSUtils.computeCssClassName(style));
        style = (Style) themeManager.getNamedObject(THEME_NAME, "style",
                THEME_NAME + "/print Page Styles");
        assertNotNull(style);
        content = content.replace("${print_suid}",
                CSSUtils.computeCssClassName(style));
        return content;
    }

    public void testStylesRegistration() throws Exception {
        String res = getRenderedCssFileContent("default");
        String expected = getTestFileContent("css_default_rendering.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering.txt");
        assertEquals(expected, res);

        // override conf, by adding additional nuxeo_dm_default2 css to the
        // page
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config2.xml");

        res = getRenderedCssFileContent("default");
        expected = getTestFileContent("css_default_rendering2.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("css_dark_rendering2.txt");
        assertEquals(expected, res);
    }

    // TODO: test themePage merge + (styles + flavors merge)
    // test Flavors (presets) merge

}
