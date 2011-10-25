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
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.themes.ThemeManager;

/**
 * @since 5.4.3
 */
public class TestThemeStylingService extends NXRuntimeTestCase {

    public static final String THEME_NAME = "testStyling";

    protected ThemeService themeService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.theme.core");
        deployBundle("org.nuxeo.theme.fragments");
        deployBundle("org.nuxeo.theme.styling");
        deployContrib("org.nuxeo.theme.styling.tests", "theme-test-config.xml");
        deployContrib("org.nuxeo.theme.styling.tests",
                "theme-styling-test-config.xml");
        themeService = Manager.getThemeService();
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
        Style dmStyle = (Style) themeManager.getNamedObject(THEME_NAME,
                "style", "nuxeo_dm_default");
        assertNotNull(dmStyle);
        content = content.replace("${nuxeo_dm_default_suid}",
                CSSUtils.computeCssClassName(dmStyle));
        Style dmStyle2 = (Style) themeManager.getNamedObject(THEME_NAME,
                "style", "nuxeo_dm_default2");
        assertNotNull(dmStyle2);
        content = content.replace("${nuxeo_dm_default2_suid}",
                CSSUtils.computeCssClassName(dmStyle2));
        Style printStyle = (Style) themeManager.getNamedObject(THEME_NAME,
                "style", "print_default");
        assertNotNull(printStyle);
        content = content.replace("${print_default_suid}",
                CSSUtils.computeCssClassName(printStyle));
        return content;
    }

    public void testStylesRegistration() throws Exception {
        String res = getRenderedCssFileContent("default");
        String expected = getTestFileContent("expected_default_rendering.txt");
        assertEquals(expected, res);

        res = getRenderedCssFileContent("dark");
        expected = getTestFileContent("expected_dark_rendering.txt");
        assertEquals(expected, res);
    }

    // TODO: test themePage merge + (styles + flavors merge)
    // test Flavors (presets) merge

}
