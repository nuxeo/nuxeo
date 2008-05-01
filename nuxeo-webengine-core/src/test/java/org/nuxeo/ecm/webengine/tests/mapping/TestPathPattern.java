package org.nuxeo.ecm.webengine.tests.mapping;

import junit.framework.TestCase;
import org.nuxeo.ecm.webengine.mapping.PathPattern;
import org.nuxeo.ecm.webengine.mapping.Mapping;

public class TestPathPattern extends TestCase {

    public void testSimple() {
        PathPattern pat = new PathPattern("/a/b/c");
        Mapping m1 = pat.match("/a/b/c");
        assertNotNull(m1);
        assertEquals("/a/b/c", m1.getValue("path"));

        Mapping m2 = pat.match("/a/b/c/d/e");
        assertNull(m2);
    }

    public void testWildcard() {
        PathPattern pat = new PathPattern("/a/b/.*");
        Mapping m1 = pat.match("/a/b/c");
        assertNotNull(m1);
        assertEquals("/a/b/c", m1.getValue("path"));

        Mapping m2 = pat.match("/a/b/c/d/e");
        assertNotNull(m2);
        assertEquals("/a/b/c/d/e", m2.getValue("path"));
    }

    public void testNamedPattern() {
        PathPattern pat = new PathPattern("/(?first:.*)/demos/?");
        Mapping m1 = pat.match("/a/b/demos/");
        assertNotNull(m1);
        assertEquals("/a/b/demos/", m1.getValue("path"));
        assertEquals("a/b", m1.getValue("first"));

        Mapping m2 = pat.match("/foo/demos/bar");
        assertNull(m2);
    }

}
