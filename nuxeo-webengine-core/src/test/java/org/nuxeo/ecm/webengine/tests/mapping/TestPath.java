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
        Path p = new Path("/a/./b/../c/");
        assertEquals("/a/c/", p.toString());
    }

}
