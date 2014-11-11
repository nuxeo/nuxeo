/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.themes;

import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeSet;

public class TestThemeSet extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "themeset-config.xml");

    }

    public void testGetThemeSets() {
        ThemeManager themeManager = Manager.getThemeManager();
        List<ThemeSet> themeSets = themeManager.getThemeSets();
        assertEquals(1, themeSets.size());
        assertEquals("galaxy", themeSets.get(0).getName());
    }

    public void testGetThemeSetByName() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeSet themeSet = themeManager.getThemeSetByName("galaxy");
        assertEquals("galaxy", themeSet.getName());
        assertEquals("galaxy-blogs", themeSet.getThemeForFeature("blogs"));
        assertEquals("galaxy-dm", themeSet.getThemeForFeature("dm"));
        assertEquals("galaxy-sites", themeSet.getThemeForFeature("sites"));
        assertEquals("galaxy-default", themeSet.getThemeForFeature("default"));
        assertEquals("galaxy-default", themeSet.getThemeForFeature("dashboard"));
        assertEquals("galaxy-search", themeSet.getThemeForFeature("search"));
        assertNull(themeSet.getThemeForFeature("unknown"));
    }
}
