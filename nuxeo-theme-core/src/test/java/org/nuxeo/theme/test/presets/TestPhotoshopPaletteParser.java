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

import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.presets.PhotoshopPaletteParser;

public class TestPhotoshopPaletteParser extends TestCase {

    public void testAcoV1() {
        URL url = getClass().getClassLoader().getResource(
                "photoshop-v1-palette.aco");
        Map<String, String> entries = PaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();
        assertEquals(336, keys.length);
        assertEquals("Color 1", keys[0]);
        assertEquals("Color 2", keys[1]);
        assertEquals("Color 3", keys[2]);
        assertEquals("Color 4", keys[3]);
        assertEquals("Color 5", keys[4]);
        assertEquals("Color 6", keys[5]);
        assertEquals("Color 7", keys[6]);
        assertEquals("#fff", entries.get(keys[0]));
        assertEquals("#ccc", entries.get(keys[1]));
        assertEquals("#999", entries.get(keys[2]));
        assertEquals("#666", entries.get(keys[3]));
        assertEquals("#333", entries.get(keys[4]));
        assertEquals("#000", entries.get(keys[5]));
        assertEquals("#fc0", entries.get(keys[6]));
        assertEquals("#f90", entries.get(keys[7]));
    }

    public void testAcoV2() {
        URL url = getClass().getClassLoader().getResource(
                "photoshop-v2-palette.aco");
        Map<String, String> entries = PaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();
        assertEquals(72, keys.length);
        assertEquals("Technorati Green", keys[0]);
        assertEquals("Technorati Green 2", keys[1]);
        assertEquals("Technorati Red", keys[2]);
        assertEquals("Technorati Blue", keys[3]);
        assertEquals("RSS Button Orange", keys[4]);
        assertEquals("#85c329", entries.get(keys[0]));
        assertEquals("#3bb000", entries.get(keys[1]));
        assertEquals("#c90404", entries.get(keys[2]));
        assertEquals("#000097", entries.get(keys[3]));
        assertEquals("#f60", entries.get(keys[4]));
    }

}
