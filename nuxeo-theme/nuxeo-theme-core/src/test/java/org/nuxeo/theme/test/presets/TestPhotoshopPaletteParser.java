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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.theme.presets.PhotoshopPaletteParser;

public class TestPhotoshopPaletteParser extends TestCase {

    public void testAcoV1() throws MalformedURLException {
        URL url = getClass().getClassLoader().getResource("photoshop-v1-palette.aco");
        Map<String, String> entries = PhotoshopPaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();
        assertEquals(336, keys.length);
        assertEquals("Color 1", keys[0]);
        assertEquals("Color 2", keys[1]);
        assertEquals("Color 3", keys[2]);
        assertEquals("Color 4", keys[3]);
        assertEquals("Color 5", keys[4]);
        assertEquals("Color 6", keys[5]);
        assertEquals("Color 7", keys[6]);
        assertEquals("rgb(255,255,255)", entries.get(keys[0]));
        assertEquals("rgb(204,204,204)", entries.get(keys[1]));
        assertEquals("rgb(153,153,153)", entries.get(keys[2]));
        assertEquals("rgb(102,102,102)", entries.get(keys[3]));
        assertEquals("rgb(51,51,51)", entries.get(keys[4]));
        assertEquals("rgb(0,0,0)", entries.get(keys[5]));
        assertEquals("rgb(255,204,0)", entries.get(keys[6]));
        assertEquals("rgb(255,153,0)", entries.get(keys[7]));
    }

    public void testAcoV2() throws MalformedURLException {
        URL url = getClass().getClassLoader().getResource("photoshop-v2-palette.aco");
        Map<String, String> entries = PhotoshopPaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();
        assertEquals(72, keys.length);
        assertEquals("Technorati Green", keys[0]);
        assertEquals("Technorati Green 2", keys[1]);
        assertEquals("Technorati Red", keys[2]);
        assertEquals("Technorati Blue", keys[3]);
        assertEquals("RSS Button Orange", keys[4]);
        assertEquals("rgb(133,195,41)", entries.get(keys[0]));
        assertEquals("rgb(59,176,0)", entries.get(keys[1]));
        assertEquals("rgb(201,4,4)", entries.get(keys[2]));
        assertEquals("rgb(0,0,151)", entries.get(keys[3]));
        assertEquals("rgb(255,102,0)", entries.get(keys[4]));
    }

}
