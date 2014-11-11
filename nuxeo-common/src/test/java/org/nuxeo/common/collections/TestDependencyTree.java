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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestDependencyTree {

    private DependencyTree<String, String> dt;

    @Before
    public void setUp() {
        dt = new DependencyTree<String, String>();
    }

    @Test
    public void testRegistration() {
        dt.add("a", "a");
        assertEquals("a", dt.get("a"));
        assertTrue(dt.isRegistered("a"));
        assertTrue(dt.isResolved("a"));
        assertNotNull(dt.get("a"));

        dt.add("ab", "ab", "a", "b");
        assertTrue(dt.isRegistered("ab"));
        assertFalse(dt.isResolved("ab"));
        assertTrue(dt.isRegistered("ab"));
        assertFalse(dt.isRegistered("b"));
        assertFalse(dt.isResolved("b"));
        assertNotNull(dt.get("ab"));
        assertNull(dt.get("b"));

        dt.add("ac", "ac", "a", "c");
        assertTrue(dt.isRegistered("ac"));
        assertFalse(dt.isResolved("ac"));
        assertTrue(dt.isRegistered("ac"));
        assertFalse(dt.isRegistered("c"));
        assertFalse(dt.isResolved("c"));
        assertNotNull(dt.get("ac"));
        assertNull(dt.get("c"));

        dt.add("b", "b", "c");
        assertTrue(dt.isRegistered("ab"));
        assertFalse(dt.isResolved("ab"));
        assertTrue(dt.isRegistered("ab"));
        assertTrue(dt.isRegistered("b"));
        assertFalse(dt.isResolved("b"));
        assertNotNull(dt.get("ab"));
        assertEquals("b", dt.get("b"));

        assertFalse(dt.isRegistered("c"));
        assertFalse(dt.isResolved("c"));
        assertNull(dt.get("c"));


        dt.add("c", "c", "a");

        assertTrue(dt.isRegistered("ab"));
        assertTrue(dt.isResolved("ab"));
        assertTrue(dt.isRegistered("b"));
        assertTrue(dt.isResolved("b"));
        assertEquals("ab", dt.get("ab"));
        assertEquals("b", dt.get("b"));

        assertTrue(dt.isRegistered("ac"));
        assertTrue(dt.isResolved("ac"));
        assertEquals("ac", dt.get("ac"));

        assertTrue(dt.isRegistered("c"));
        assertTrue(dt.isResolved("c"));
        assertEquals("c", dt.get("c"));
    }

    @Test
    public void testUnregister() {
        testRegistration();

        dt.remove("a");

        assertFalse(dt.isRegistered("a"));
        assertFalse(dt.isResolved("a"));
        assertNull(dt.get("a"));

        assertTrue(dt.isRegistered("ab"));
        assertFalse(dt.isResolved("ab"));
        assertEquals("ab", dt.get("ab"));

        assertTrue(dt.isRegistered("b"));
        assertFalse(dt.isResolved("b"));
        assertEquals("b", dt.get("b"));

        assertTrue(dt.isRegistered("ac"));
        assertFalse(dt.isResolved("ac"));
        assertEquals("ac", dt.get("ac"));

        assertTrue(dt.isRegistered("c"));
        assertFalse(dt.isResolved("c"));
        assertEquals("c", dt.get("c"));
    }

}
