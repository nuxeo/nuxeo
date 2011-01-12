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

public class TestPropertiesPaletteParser extends TestCase {

    public void testParser() {
        URL url = getClass().getClassLoader().getResource(
                "properties-palette.properties");
        Map<String, String> entries = PaletteParser.parse(url);
        Object[] keys = entries.keySet().toArray();

        assertEquals("Plum", keys[0]);
        assertEquals("Chocolate", keys[1]);
        assertEquals("rgb(173,127,168)", entries.get("Plum"));
        assertEquals("rgb(233,185,110)", entries.get("Chocolate"));
    }

}
