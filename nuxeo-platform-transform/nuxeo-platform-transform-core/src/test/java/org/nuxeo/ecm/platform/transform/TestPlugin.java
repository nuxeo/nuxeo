/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestPlugin.java 21872 2007-07-03 16:45:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.transform.interfaces.Plugin;


public class TestPlugin extends TestCase {

    public void testOptionsNoDefault() {
        Plugin plugin = new FakePlugin("FakePlugin");
        assertTrue(plugin.getDefaultOptions().keySet().isEmpty());

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put("host", "localhost");
        options.put("port", "8080");

        plugin.setSpecificOptions(options);

        Map<String, Serializable> pluginDefaultOptions = plugin.getDefaultOptions();
        for (String key : options.keySet()) {
            assertTrue(pluginDefaultOptions.containsKey(key));
            assertEquals(pluginDefaultOptions.get(key), options.get(key));
        }

        // Test override here.
        options = new HashMap<String, Serializable>();
        options.put("host", "anotherhost");
        plugin.setSpecificOptions(options);

        pluginDefaultOptions = plugin.getDefaultOptions();
        assertEquals(pluginDefaultOptions.get("host"), options.get("host"));
        assertEquals("anotherhost", pluginDefaultOptions.get("host"));

        // Check old value
        assertEquals("8080", pluginDefaultOptions.get("port"));
    }

    public void testOptions() {
        Map<String, Serializable> defaultOptions = new HashMap<String, Serializable>();
        defaultOptions.put("host", "localhost");
        defaultOptions.put("port", "8080");

        List<String> sourceMimeTypes = new ArrayList<String>();
        sourceMimeTypes.add("application/mwsword");
        FakePlugin plugin = new FakePlugin("FakePlugin", sourceMimeTypes,
                "application/pdf", defaultOptions);

        Map<String, Serializable> pluginDefaultOptions = plugin.getDefaultOptions();
        for (String key : defaultOptions.keySet()) {
            assertTrue(pluginDefaultOptions.containsKey(key));
            assertEquals(pluginDefaultOptions.get(key), defaultOptions.get(key));
        }
    }

}
