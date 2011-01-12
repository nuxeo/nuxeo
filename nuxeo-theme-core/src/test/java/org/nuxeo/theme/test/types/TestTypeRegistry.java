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

package org.nuxeo.theme.test.types;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestTypeRegistry extends NXRuntimeTestCase {

    private PresetType preset1;

    private PresetType preset2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");

        preset1 = new PresetType("preset1", "value", "group", "category", "",
                "");
        preset2 = new PresetType("preset2", "value", "", "category", "", "");
    }

    public void testPresetType() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        assertTrue(typeRegistry.getTypes(TypeFamily.PRESET).isEmpty());
        assertTrue(typeRegistry.getTypeNames(TypeFamily.PRESET).isEmpty());
        assertNull(typeRegistry.lookup(TypeFamily.PRESET, "preset1 (group)"));
        assertNull(typeRegistry.lookup(TypeFamily.PRESET, "preset2"));

        // register presets
        typeRegistry.register(preset1);
        typeRegistry.register(preset2);

        assertTrue(typeRegistry.getTypes(TypeFamily.PRESET).contains(preset1));
        assertTrue(typeRegistry.getTypes(TypeFamily.PRESET).contains(preset2));

        assertTrue(typeRegistry.getTypeNames(TypeFamily.PRESET).contains(
                "preset1 (group)"));
        assertTrue(typeRegistry.getTypeNames(TypeFamily.PRESET).contains(
                "preset2"));

        assertSame(preset1, typeRegistry.lookup(TypeFamily.PRESET,
                "preset1 (group)"));
        assertSame(preset2, typeRegistry.lookup(TypeFamily.PRESET, "preset2"));

        // unregister presets
        typeRegistry.unregister(preset1);
        typeRegistry.unregister(preset2);

        assertTrue(typeRegistry.getTypes(TypeFamily.PRESET).isEmpty());
        assertTrue(typeRegistry.getTypeNames(TypeFamily.PRESET).isEmpty());

        assertNull(typeRegistry.lookup(TypeFamily.PRESET, "preset1 (group)"));
        assertNull(typeRegistry.lookup(TypeFamily.PRESET, "preset1"));

        // clear registry
        typeRegistry.register(preset1);
        typeRegistry.clear();

        assertTrue(typeRegistry.getTypes(TypeFamily.PRESET).isEmpty());
        assertTrue(typeRegistry.getTypeNames(TypeFamily.PRESET).isEmpty());
        assertNull(typeRegistry.lookup(TypeFamily.PRESET, "preset1"));
    }

}
