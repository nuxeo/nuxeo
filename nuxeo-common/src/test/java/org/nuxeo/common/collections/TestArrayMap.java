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
 * $Id$
 */

package org.nuxeo.common.collections;

import junit.framework.TestCase;

/**
 * @author sfermigier
 */
public class TestArrayMap extends TestCase {

    public void test() {
        ArrayMap<String, String> am = new ArrayMap<String, String>();

        assertTrue(am.isEmpty());
        assertEquals(0, am.size());

        am.put("0", "0");
        am.put("1", "1");
        assertFalse(am.isEmpty());
        assertEquals(2, am.size());

        assertEquals("0", am.get("0"));
        assertEquals("1", am.get("1"));
        assertNull(am.get("2"));

        assertEquals("0", am.get(0));
        assertEquals("1", am.get(1));
        assertEquals("0", am.getKey(0));
        assertEquals("1", am.getKey(1));

        ArrayMap<String, String> am2 = new ArrayMap<String, String>(am);

        assertEquals("0", am2.get("0"));
        assertEquals("1", am2.get("1"));
        assertEquals("0", am2.get(0));
        assertEquals("1", am2.get(1));
        assertEquals("0", am2.getKey(0));
        assertEquals("1", am2.getKey(1));

        assertEquals(am, am2);
        assertEquals(am.hashCode(), am2.hashCode());

        am2.remove(0);
        assertEquals(1, am2.size());
        assertEquals("1", am2.get("1"));
        assertEquals("1", am2.get(0));
        assertEquals("1", am2.getKey(0));
        assertFalse(am.equals(am2));
        assertFalse(am2.equals(am));

        ArrayMap<String, String> am3 = new ArrayMap<String, String>(am2);
        am2.trimToSize();
        assertEquals(am2, am3);

        am2.clear();
        assertEquals(0, am2.size());

        am2.put("0", "1");
        am2.put("0", "2");
        assertEquals(1, am2.size());
        assertEquals("2", am2.get("0"));
        assertEquals("2", am2.get(0));
        am2.add("0", "3");
        assertEquals(2, am2.size());
        assertEquals("2", am2.get("0"));
    }

    public void testAdd() {
        ArrayMap<String, String> am = new ArrayMap<String, String>(1);

        assertTrue(am.isEmpty());
        assertEquals(0, am.size());

        am.add("0", "0");
        am.add("1", "1");
        assertFalse(am.isEmpty());
        assertEquals(2, am.size());

        assertEquals("0", am.get("0"));
        assertEquals("1", am.get("1"));
        assertEquals("0", am.get(0));
        assertEquals("1", am.get(1));
        assertEquals("0", am.getKey(0));
        assertEquals("1", am.getKey(1));
    }

}
