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
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestDependencyTree extends TestCase {

    private DependencyTree<String, String> dt;

    @Override
    protected void setUp() throws Exception {
        dt = new DependencyTree<String, String>();
    }

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
