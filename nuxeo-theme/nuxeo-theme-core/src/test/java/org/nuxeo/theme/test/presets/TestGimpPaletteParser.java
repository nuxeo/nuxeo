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

import org.nuxeo.theme.presets.GimpPaletteParser;
import org.nuxeo.theme.presets.PaletteParser;

public class TestGimpPaletteParser extends TestCase {

    public void testParser() {
        URL url = getClass().getClassLoader().getResource("gimp-palette.gpl");
        Map<String, String> entries = PaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();
        assertEquals(4, keys.length);
        assertEquals("Plum", keys[0]);
        assertEquals("Chocolate", keys[1]);
        assertEquals("Color 3", keys[2]);
        assertEquals("Double", keys[3]);
        assertEquals("#ad7fa8", entries.get(keys[0]));
        assertEquals("#e9b96e", entries.get(keys[1]));
        assertEquals("#010203", entries.get(keys[2]));
        assertEquals("#010203", entries.get(keys[3]));
    }

}
