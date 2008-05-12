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

package org.nuxeo.ecm.webengine.tests.mapping;

import junit.framework.TestCase;

import org.nuxeo.ecm.webengine.mapping.Path;

public class TestPath extends TestCase {

    public void testAbsolute() {
        Path p = new Path("/a/b/c");
        assertEquals("/a/b/c", p.toString());

        assertFalse(p.isRoot());
        assertFalse(p.isEmpty());
        assertFalse(p.isRelative());
        assertTrue(p.isAbsolute());
        assertFalse(p.hasTrailingSeparator());

        assertEquals(p, p.makeAbsolute());
        assertEquals(new Path("a/b/c"), p.makeRelative());

        assertEquals(p, p.removeTrailingSeparator());
        assertEquals(new Path("/a/b/c/"), p.addTrailingSeparator());

        assertEquals(3, p.segmentCount());
        assertEquals("a", p.segment(0));
        assertEquals("b", p.segment(1));
        assertEquals("c", p.segment(2));
        assertEquals("c", p.lastSegment());
        assertEquals("a", p.segments()[0]);
        assertEquals("b", p.segments()[1]);
        assertEquals("c", p.segments()[2]);
    }

    public void testRelative() {
        Path p = new Path("a/b/c/");
        assertEquals("a/b/c/", p.toString());

        assertEquals(p, p.makeRelative());
        assertEquals(new Path("/a/b/c/"), p.makeAbsolute());

        assertEquals(p, p.addTrailingSeparator());
        assertEquals(new Path("a/b/c"), p.removeTrailingSeparator());

        assertFalse(p.isRoot());
        assertFalse(p.isEmpty());
        assertTrue(p.isRelative());
        assertFalse(p.isAbsolute());
        assertTrue(p.hasTrailingSeparator());
    }

    public void testEquals() {
        Path p1 = new Path("/a/b/c");
        Path p2 = new Path("/a/b/c");
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        Path p3 = new Path("/a/b/c/");
        assertFalse(p1.equals(p3));
        assertFalse(p3.equals(p1));

        Path p4 = new Path("a/b/c");
        assertFalse(p1.equals(p4));
        assertFalse(p4.equals(p1));
        assertFalse(p3.equals(p4));
        assertFalse(p4.equals(p3));
    }

    public void test3() {
        Path p = new Path("///a/b/c");
        assertEquals("/a/b/c", p.toString());
    }

    public void test4() {
        Path p = new Path("/a/./b/../c///");
        assertEquals("/a/c/", p.toString());
    }

    public void testFile() {
        Path p = new Path("/a/b/c/test.txt");
        assertEquals("txt", p.getFileExtension());
        assertEquals("test", p.getFileName());
    }

    public void testAppend() {
        // XXX: check that this are the intended results
        assertEquals(new Path("a/b/c"), new Path("").append(new Path("/a/b/c")));
        assertEquals(new Path("/a/b/c"), new Path("/a/b/c").append(new Path("")));

        assertEquals(new Path("/a/b/c/"), new Path("/").append(new Path("a/b/c/")));
        assertEquals(new Path("/a/b/c"), new Path("/a/b/c").append(new Path("/")));
    }

    public void testParent() {
        assertEquals(new Path("/a/b/"), new Path("/a/b/c/").getParent());
        assertEquals(new Path("/a/b"), new Path("/a/b/c").getParent());
    }

}
