/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

public class TestState {

    @Test
    public void testState() {
        State s = new State();
        check(s, 0);

        // putting a null for a key doesn't add it (base State semantics, StateDiff would be different)
        s.put("555", null);
        check(s, 0);

        s.put("1", "1");
        check(s, 1);

        s.remove("1");
        check(s, 0);

        s.put("1", "1");
        check(s, 1);
        // put different value
        s.put("1", "111");
        assertEquals("111", s.get("1"));
        // put again different value to keep testing easily
        s.put("1", "1");
        check(s, 1);
        // putting a null for a key doesn't add it
        s.put("555", null);
        check(s, 1);

        // remove by putting null
        s.put("1", null);
        check(s, 0);

        s.put("1", "1");
        check(s, 1);
        s.put("2", "2");
        check(s, 2);
        s.put("3", "3");
        check(s, 3);
        s.put("4", "4");
        check(s, 4);
        s.put("5", "5");
        check(s, 5);
        // this will switch to a map internally (ARRAY_MAX = 5)
        s.put("6", "6");
        check(s, 6);
        s.put("7", "7");
        check(s, 7);

        // remove by putting a null
        s.put("7", null);
        check(s, 6);
    }

    protected static void check(State s, int n) {
        Set<String> set = s.keySet();
        Set<Entry<String, Serializable>> es = s.entrySet();
        assertEquals(n, s.size());
        if (n == 0) {
            assertTrue(s.isEmpty());
            assertTrue(set.isEmpty());
            assertTrue(es.isEmpty());
        } else {
            assertFalse(s.isEmpty());
            assertFalse(set.isEmpty());
            assertFalse(es.isEmpty());
        }
        assertNull(s.get("nosuchkey"));
        assertNull(s.remove("nosuchkey"));
        assertFalse(s.containsKey("nosuchkey"));
        assertEquals(n, set.size());
        for (int i = 1; i <= n; i++) {
            String ii = String.valueOf(i);
            assertEquals(ii, s.get(ii));
            assertTrue(set.contains(ii));
        }
        assertFalse(set.contains("nosuchkey"));
        assertEquals(n, es.size());
        String[] ar = s.keyArray();
        assertEquals(n, ar.length);
        int i = 1;
        for (Entry<String, Serializable> e : es) {
            String ii = String.valueOf(i++);
            assertEquals(ii, e.getKey());
            assertEquals(ii, e.getValue());
        }
    }

}
