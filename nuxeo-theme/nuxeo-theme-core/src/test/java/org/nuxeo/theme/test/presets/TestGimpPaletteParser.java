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

public class TestGimpPaletteParser extends TestCase {

    public void testParser() {
        URL url = getClass().getClassLoader().getResource("gimp-palette.gpl");
        Map<String, String> entries = GimpPaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();
        assertEquals(4, keys.length);
        assertEquals("Plum", keys[0]);
        assertEquals("Chocolate", keys[1]);
        assertEquals("Color 3", keys[2]);
        assertEquals("Double", keys[3]);
        assertEquals("rgb(173,127,168)", entries.get(keys[0]));
        assertEquals("rgb(233,185,110)", entries.get(keys[1]));
        assertEquals("rgb(1,2,3)", entries.get(keys[2]));
        assertEquals("rgb(1,2,3)", entries.get(keys[3]));
    }

}
