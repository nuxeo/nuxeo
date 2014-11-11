/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.collections;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author sfermigier
 */
public class TestArrayMap {

    @Test
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

    @Test
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
