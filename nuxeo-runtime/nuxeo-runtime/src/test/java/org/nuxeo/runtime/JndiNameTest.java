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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime;

import junit.framework.TestCase;

import org.nuxeo.runtime.binding.JndiName;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JndiNameTest extends TestCase {

    public void testName() throws Exception {
        JndiName a = new JndiName("a/b/c");
        JndiName b = new JndiName("a/b/c");
        assertEquals(a, b);

        b = new JndiName("a/b/d");
        assertFalse(a.equals(b));
        b = new JndiName("a", "b", "c");
        assertEquals(a, b);

        // endsWith
        assertTrue(a.endsWith(b));
        b = new JndiName("a/b/c/d");
        assertFalse(a.endsWith(b));
        b = new JndiName("a/b");
        assertFalse(a.endsWith(b));
        b = new JndiName("b/c");
        assertTrue(a.endsWith(b));
        b = new JndiName("c");
        assertTrue(a.endsWith(b));

        // startsWith
        a = new JndiName("a/b/c");
        b = new JndiName("a/b/c");
        assertTrue(a.startsWith(b));
        b = new JndiName("a/b/c/d");
        assertFalse(a.startsWith(b));
        b = new JndiName("a/b");
        assertTrue(a.startsWith(b));
        b = new JndiName("a");
        assertTrue(a.startsWith(b));

        // add
        a = new JndiName("a/b/c");
        assertEquals(a.add("d"), new JndiName("a/b/c/d"));
        assertEquals(a.add(a.size(), "TAIL"), new JndiName("a/b/c/d/TAIL"));
        assertEquals(a.add(0, "HEAD"), new JndiName("HEAD/a/b/c/d/TAIL"));
        assertEquals(a.add(1, "x"), new JndiName("HEAD/x/a/b/c/d/TAIL"));

        assertEquals("x", a.remove(1));
        assertEquals(a, new JndiName("HEAD/a/b/c/d/TAIL"));
        assertEquals("TAIL", a.remove(a.size() - 1));
        assertEquals(a, new JndiName("HEAD/a/b/c/d"));
        assertEquals("HEAD", a.remove(0));
        assertEquals(a, new JndiName("a/b/c/d"));
    }

}
