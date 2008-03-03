/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestStringUtils.java 15072 2007-03-31 19:17:08Z sfermigier $
 */

package org.nuxeo.common.utils;

import junit.framework.TestCase;

/**
 * @author sfermigier
 */
public class TestPath extends TestCase {

    public void test() {
        Path path = new Path("/a/b/c/d");

        assertFalse(path.isRoot());
        assertFalse(path.isEmpty());

        assertTrue(path.isAbsolute());

        assertEquals("/a/b/c/d", path.toString());
        assertEquals(4, path.segmentCount());
        assertEquals(4, path.segments().length);
        assertEquals("a", path.segment(0));
        assertEquals("b", path.segment(1));
        assertEquals("c", path.segment(2));
        assertEquals("d", path.segment(3));
        assertNull(path.segment(4));
        assertEquals("d", path.lastSegment());

        assertTrue(path.isPrefixOf(new Path("/a/b/c/d/e")));
        assertFalse(path.isPrefixOf(new Path("/a/b/c")));
        assertFalse(path.isPrefixOf(new Path("/a/b/c/e")));
        assertFalse(path.isPrefixOf(new Path("/e/b/c/d")));

        assertEquals(new Path("b/c/d"), path.removeFirstSegments(1));
        assertEquals(new Path("/a/b/c"), path.removeLastSegments(1));
    }

    @SuppressWarnings({"ObjectEqualsNull"})
    public void testEquals() {
        Path path = new Path("/a/b/c/d");
        Path path2 = new Path("/a/b/c/d/");
        Path path3 = new Path("/a/b/c////d/");
        Path path4 = new Path("/a/b/././c/./e/../d/");
        //Path path5 = new Path("../a/b/c/d");

        assertEquals(path, path);
        assertEquals(path, path2);
        assertEquals(path, path3);
        assertEquals(path, path4);
        //assertEquals(path, path5);

        assertEquals(path.hashCode(), path2.hashCode());
        assertEquals(path.hashCode(), path3.hashCode());
        assertEquals(path.hashCode(), path4.hashCode());
        //assertEquals(path.hashCode(), path5.hashCode());

        assertFalse(path.equals(null));

        assertFalse(path.equals(new Path("a/b/c/d")));
        assertFalse(path.equals(new Path("/a/b/c/e")));
        assertFalse(path.equals(new Path("/a/b/c/d/e")));
    }

    public void testGetFileExtension() {
        assertNull(new Path("/a/b/c/").getFileExtension());
        assertNull(new Path("/a/b/c").getFileExtension());
        assertEquals("doc", new Path("/a/b/c.doc").getFileExtension());
    }

    public void testBasic() {
        final Path path = new Path("/a/b/c");

        assertEquals(3, path.segmentCount());
        assertTrue(path.isAbsolute());
        assertFalse(path.isEmpty());
        assertFalse(path.isRoot());
        assertEquals(3, path.segments().length);
        assertEquals("a", path.segment(0));
        assertEquals("b", path.segment(1));
        assertEquals("c", path.segment(2));

        assertEquals(path, Path.createFromAbsolutePath("/a/b/c"));
    }

    public void testFileExtension() {
        Path path = new Path("/a/b/c");
        assertNull(path.getFileExtension());

        path = new Path("/a/b/c.txt");
        assertEquals("txt", path.getFileExtension());
        assertEquals("/a/b/c", path.removeFileExtension().toString());

        path = new Path("/a/b/c/");
        assertNull(path.getFileExtension());
    }

    public void testPathNormalisation() {
        Path path = new Path("////a/./b/../c");
        assertEquals("/a/c", path.toString());
    }

    public void testEquality() {
        assertEquals(new Path("/a/b/c"), new Path("/a/b/c"));
        assert !new Path("/a/b/c").equals(new Path("/a/b"));
    }

    public void testAppend() {
        Path path1 = new Path("/a/b/c");
        Path path2 = new Path("/d/e/f");
        Path path3 = new Path("/a/b/c/d/e/f");
        // XXX: I don't think this is natural, see my comment in Path.java
        assertEquals(path1.append(path2), path3);
    }

}
