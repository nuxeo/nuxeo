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

package org.nuxeo.theme.test.presets;

import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.presets.CustomPresetType;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeRegistry;

public class TestPresetManager extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    public void testExtractPresetName() {
        // Global preset
        assertEquals("green (colors)", PresetManager.extractPresetName(null,
                " \"green (colors)\"  "));
        // Custom preset
        assertEquals("theme1/green", PresetManager.extractPresetName("theme1",
                "\"green\""));
        assertNull(PresetManager.extractPresetName(null, "\"green\""));
    }

    public void testGetPresetByName() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        PresetType preset1 = new PresetType("orange", "#fc0", "colors",
                "color", "", "");
        PresetType preset2 = new PresetType("green", "#0f0", "colors", "color",
                "", "");
        typeRegistry.register(preset1);
        typeRegistry.register(preset2);
        assertSame(preset1, PresetManager.getPresetByName("orange (colors)"));
        assertSame(preset2, PresetManager.getPresetByName("green (colors)"));
    }

    public void testGetAllPresets() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        assert PresetManager.getAllPresets().isEmpty();
        PresetType preset1 = new PresetType("orange", "#fc0", "colors",
                "color", "", "");
        PresetType preset2 = new PresetType("green", "#0f0", "colors", "color",
                "", "");
        typeRegistry.register(preset1);
        typeRegistry.register(preset2);
        List<Type> allPresets = PresetManager.getAllPresets();
        assertTrue(allPresets.contains(preset1));
        assertTrue(allPresets.contains(preset2));
        assertEquals(2, allPresets.size());
    }

    public void testResolvePresets() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        PresetType preset1 = new PresetType("orange", "#fc0", "colors",
                "color", "", "");
        PresetType preset2 = new PresetType("green", "#0f0", "colors", "color",
                "", "");
        typeRegistry.register(preset1);
        typeRegistry.register(preset2);
        assertEquals("#000 #fc0 #0f0 #fff", PresetManager.resolvePresets(null,
                "#000 \"orange (colors)\" \"green (colors)\" #fff"));
        assertEquals("1px solid \"red\"", PresetManager.resolvePresets(null,
                "1px solid \"red\""));
        assertEquals("1px solid #fc0", PresetManager.resolvePresets(null,
                "1px solid \"orange (colors)\""));
        assertEquals("#fc0", PresetManager.resolvePresets(null,
                "\"orange (colors)\""));
    }

    public void testResolveCustomPresets() {
        ThemeManager themeManager = Manager.getThemeManager();
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ThemeElement theme = new ThemeElement();
        theme.setName("theme1");
        themeManager.registerTheme(theme);
        PresetType preset1 = new CustomPresetType("orange", "#fc0", "theme1",
                "color", "", "");
        PresetType preset2 = new CustomPresetType("green", "#0f0", "theme1",
                "color", "", "");
        typeRegistry.register(preset1);
        typeRegistry.register(preset2);
        assertEquals("#000 #fc0 #0f0 #fff", PresetManager.resolvePresets(
                "theme1", "#000 \"orange\" \"green\" #fff"));
        assertEquals("1px solid \"red\"", PresetManager.resolvePresets(
                "theme1", "1px solid \"red\""));
        assertEquals("1px solid #fc0", PresetManager.resolvePresets("theme1",
                "1px solid \"orange\""));
        assertEquals("#fc0", PresetManager.resolvePresets("theme1",
                "\"orange\""));
    }

}
