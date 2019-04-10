/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.tests;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;

public class TestMetadataCollector extends TestCase {

    Map<String, String> props1 = new HashMap<String, String>();

    Map<String, String> props2 = new HashMap<String, String>();

    Map<String, String> props3 = new HashMap<String, String>();

    MetadataCollector mc = new MetadataCollector();

    protected void setUp() throws Exception {
        super.setUp();
        props1.put("str", "value1");
        props1.put("str0", "value0");
        props1.put("date", "12/06/2007");
        props1.put("num", "1577");
        props1.put("lst", "1|2|3");

        props2.put("str", "value2");
        props2.put("str2", "value22");
        props2.put("date", "01/07/2008");
        props2.put("num", "2008");

        props3.put("str", "value3");
        props3.put("str3", "value33");
        props3.put("date", "09/28/2009");
        props3.put("num", "7786");
        props3.put("lst", "AA|BB|CC");

        mc = new MetadataCollector();
        mc.addPropertiesFromStrings("/", props1);
        mc.addPropertiesFromStrings("/node21/", props2);
        mc.addPropertiesFromStrings("/node22/", props3);
        mc.addPropertiesFromStrings("/node21/node31", props3);
    }

    public void testRead() {

        assertEquals("value0", mc.getProperty("/", "str0"));
        assertEquals("value1", mc.getProperty("/", "str"));
        assertEquals("1577", mc.getProperty("/", "num"));
        assertEquals(
                2007,
                ((GregorianCalendar) mc.getProperty("/", "date")).get(Calendar.YEAR));
        assertNull(mc.getProperty("/", "doesnotexist"));
        List<Long> list = (List<Long>) mc.getProperty("/", "lst");
        assertNotNull(list);
        assertEquals(3, list.size());

        assertEquals("value0", mc.getProperty("/node21/", "str0"));
        assertEquals("value2", mc.getProperty("/node21/", "str"));
        assertEquals("value22", mc.getProperty("/node21/", "str2"));
        assertEquals("2008", mc.getProperty("/node21/", "num"));
        assertEquals(2008, ((GregorianCalendar) mc.getProperty("/node21/",
                "date")).get(Calendar.YEAR));
        assertNull(mc.getProperty("/node21/", "doesnotexist"));
        list = (List<Long>) mc.getProperty("/node21", "lst");
        assertNotNull(list);
        assertEquals(3, list.size());

        assertEquals("value0", mc.getProperty("/node21", "str0"));
        assertEquals("value2", mc.getProperty("/node21", "str"));
        assertEquals("value22", mc.getProperty("/node21", "str2"));
        assertEquals("2008", mc.getProperty("/node21", "num"));
        assertEquals(2008, ((GregorianCalendar) mc.getProperty("/node21",
                "date")).get(Calendar.YEAR));
        assertNull(mc.getProperty("/node21", "doesnotexist"));

        assertEquals("value0", mc.getProperty("/node21/node31", "str0"));
        assertEquals("value3", mc.getProperty("/node21/node31", "str"));
        assertEquals("value33", mc.getProperty("/node21/node31", "str3"));
        assertEquals("value22", mc.getProperty("/node21/node31", "str2"));
        assertNull(mc.getProperty("/node21/node31", "doesnotexist"));
        List<String> list2 = (List<String>) mc.getProperty("/node21/node31",
                "lst");
        assertNotNull(list2);
        assertEquals(3, list2.size());

    }
}
