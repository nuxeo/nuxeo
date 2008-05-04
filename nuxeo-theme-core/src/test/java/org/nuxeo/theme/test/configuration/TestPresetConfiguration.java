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

package org.nuxeo.theme.test.configuration;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestPresetConfiguration extends NXRuntimeTestCase {

    private TypeRegistry typeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "preset-config.xml");
        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        typeRegistry = (TypeRegistry) themeService.getRegistry("types");
    }

    public void testRegisterPropertiesPalette1() throws Exception {
        PresetType preset = (PresetType) typeRegistry.lookup(TypeFamily.PRESET, "Plum (default colors)");
        assertNotNull(preset);
        assertEquals("rgb(173,127,168)", preset.getValue());
        assertEquals("color", preset.getCategory());
    }

    public void testRegisterPropertiesPalette2() throws Exception {
        PresetType preset = (PresetType) typeRegistry.lookup(TypeFamily.PRESET, "Chocolate (default colors)");
        assertNotNull(preset);
        assertEquals("rgb(233,185,110)", preset.getValue());
        assertEquals("color", preset.getCategory());
    }

    public void testRegisterPropertiesPalette3() throws Exception {
        PresetType preset = (PresetType) typeRegistry.lookup(TypeFamily.PRESET, "ChocolateBorder (default borders)");
        assertNotNull(preset);
        assertEquals("1px solid rgb(233,185,110)", preset.getValue());
        assertEquals("border", preset.getCategory());
    }

    public void testRegisterPropertiesPalette4() throws Exception {
        PresetType preset = (PresetType) typeRegistry.lookup(TypeFamily.PRESET, "PlumBorder (default borders)");
        assertNotNull(preset);
        assertEquals("1px solid rgb(173,127,168)", preset.getValue());
        assertEquals("border", preset.getCategory());
    }

}
