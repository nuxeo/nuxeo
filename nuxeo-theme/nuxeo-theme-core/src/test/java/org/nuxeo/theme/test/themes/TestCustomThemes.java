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

import java.io.FilenameFilter;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.CustomThemeNameFilter;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

public class TestCustomThemes extends NXRuntimeTestCase {

    private ThemeManager themeManager;

    private TypeRegistry typeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");

        themeManager = Manager.getThemeManager();
        typeRegistry = Manager.getTypeRegistry();
    }

    @Override
    public void tearDown() throws Exception {
        Manager.getRelationStorage().clear();
        Manager.getPerspectiveManager().clear();
        Manager.getTypeRegistry().clear();
        Manager.getUidManager().clear();
        themeManager.clear();
        themeManager = null;
        typeRegistry.clear();
        typeRegistry = null;
        super.tearDown();
    }

    public void testFileNameFilter() {
        FilenameFilter filter = new CustomThemeNameFilter();

        assertTrue(filter.accept(null, "theme-default.xml"));
        assertTrue(filter.accept(null, "theme-default-1.xml"));
        assertTrue(filter.accept(null, "theme-default_1.xml"));
        assertTrue(filter.accept(null, "theme-default-1-2.xml"));
        assertTrue(filter.accept(null, "theme-default-a.xml"));
        assertTrue(filter.accept(null, "theme-a.xml"));
        assertTrue(filter.accept(null, "theme-a1.xml"));

        assertFalse(filter.accept(null, "theme-1.xml"));
        assertFalse(filter.accept(null, "theme-default-1.2.xml"));
        assertFalse(filter.accept(null, "theme-default-.xml"));
        assertFalse(filter.accept(null, "theme-default_.xml"));
        assertFalse(filter.accept(null, "theme-Default.xml"));
        assertFalse(filter.accept(null, "theme-_.xml"));
        assertFalse(filter.accept(null, "theme-.xml"));
        assertFalse(filter.accept(null, "theme-..xml"));
        assertFalse(filter.accept(null, "theme-1xml"));
        assertFalse(filter.accept(null, "dummy-1.xml"));
        assertFalse(filter.accept(null, "dummy-1.ext"));
        assertFalse(filter.accept(null, "dummy.ext"));
    }

    public void testUpdateDescriptors() {
        ThemeManager.updateThemeDescriptors();

        ThemeDescriptor theme1 = new ThemeDescriptor();
        theme1.setName("default");
        theme1.setSrc("src1");
        typeRegistry.register(theme1);
        ThemeManager.updateThemeDescriptors();
        assertFalse(theme1.isCustomized());

        ThemeDescriptor theme2 = new ThemeDescriptor();
        theme2.setName("alt");
        theme2.setSrc("src2");
        typeRegistry.register(theme2);
        ThemeManager.updateThemeDescriptors();
        assertFalse(theme1.isCustomized());
        assertFalse(theme2.isCustomized());

        ThemeDescriptor theme3 = new ThemeDescriptor();
        // override "default" theme
        theme3.setName("default");
        theme3.setSrc("src3");
        typeRegistry.register(theme3);
        ThemeManager.updateThemeDescriptors();
        assertTrue(theme1.isCustomized());
        assertFalse(theme2.isCustomized());
        assertFalse(theme3.isCustomized());

        theme3.setName("default-new");
        ThemeManager.updateThemeDescriptors();
        assertFalse(theme1.isCustomized());
        assertFalse(theme2.isCustomized());
        assertFalse(theme3.isCustomized());
    }

}
