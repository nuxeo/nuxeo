package org.nuxeo.chemistry.shell;

import org.junit.Assert;
import org.junit.Test;

public class TestPath extends Assert {

    @Test
    public void testRelative() {
        Path p = new Path("abc/asdf/file.ext");
        assertEquals("abc/asdf/file.ext", p.toString());
        assertEquals("ext", p.getFileExtension());
        assertEquals("file", p.getFileName());
        assertEquals("abc", p.segment(0));
        assertEquals("asdf", p.segment(1));
        assertEquals("file.ext", p.getLastSegment());

        p = p.makeAbsolute();
        assertEquals("/abc/asdf/file.ext", p.toString());

        p = p.makeAbsolute();
        assertEquals("/abc/asdf/file.ext", p.toString());

        Path p1 = p.getParent();
        assertEquals("/abc/asdf", p1.toString());

        p1 = p.up();
        assertEquals("/abc/asdf", p1.toString());
    }

    @Test
    public void testAbsolute() {
        Path p = new Path("/abc/asdf/file.ext");
        assertEquals("/abc/asdf/file.ext", p.toString());

        p = p.makeRelative();
        assertEquals("abc/asdf/file.ext", p.toString());

        p = p.makeRelative();
        assertEquals("abc/asdf/file.ext", p.toString());
    }

    @Test
    public void testDots() {
        Path p = new Path("/./abc//asdf/../file.ext");
        assertEquals("/abc/file.ext", p.toString());
    }

    @Test
    public void testAppend() {
        Path p = new Path("/commands");

        p = p.append((Path) null);
        assertEquals("/commands", p.toString());

        p = p.append("");
        assertEquals("/commands", p.toString());

        p = p.append(Path.EMPTY);
        assertEquals("/commands", p.toString());

        p = Path.EMPTY.append(p);
        assertEquals("commands", p.toString());

        p = new Path("/commands");
        p = p.append("test");
        assertEquals("/commands/test", p.toString());

        p = p.append("../../../test2");
        assertEquals("../test2", p.toString());

        p = p.makeAbsolute();
        assertEquals("/test2", p.toString());

        p = p.append(".");
        assertEquals("/test2", p.toString());

        p = p.append("..");
        assertEquals("/", p.toString());

        p = p.append("..");
        assertEquals("/", p.toString());
    }

    @Test
    public void testRoot() {
        assertTrue(Path.ROOT.isRoot());
        assertFalse(Path.ROOT.isEmpty());
        assertEquals("/", Path.ROOT.toString());
        Path p1 = new Path("/");
        assertEquals(Path.ROOT, p1);
        Path p2 = new Path(Path.ROOT);
        assertEquals(Path.ROOT, p2);
    }

    @Test
    public void testEmpty() {
        assertTrue(Path.EMPTY.isEmpty());
        assertFalse(Path.EMPTY.isRoot());
        assertEquals("", Path.EMPTY.toString());
        Path p1 = new Path("");
        assertEquals(Path.EMPTY, p1);
        Path p2 = new Path(Path.EMPTY);
        assertEquals(Path.EMPTY, p2);
    }

    // Tests copied (mutatis mutandis) from the org.nuxeo.commons Path

    @Test
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
        assertEquals("d", path.getLastSegment());

        assertEquals(new Path("b/c/d"), path.removeFirstSegments(1));
        assertEquals(new Path("/a/b/c"), path.removeLastSegments(1));
    }

    @Test
    public void testEquals() {
        Path path = new Path("/a/b/c/d");
        Path path2 = new Path("/a/b/c/d/");
        Path path3 = new Path("/a/b/c////d/");
        Path path4 = new Path("/a/b/././c/./e/../d/");

        assertEquals(path, path);

        assertFalse(path.equals(path2));

        assertEquals(path2, path3);
        assertEquals(path2, path4);

        // SF: I think this shouldn't pass...
        assertEquals(path.hashCode(), path2.hashCode());
        assertEquals(path.hashCode(), path3.hashCode());
        assertEquals(path.hashCode(), path4.hashCode());

        assertFalse(path.equals(null));

        assertFalse(path.equals(new Path("a/b/c/d")));
        assertFalse(path.equals(new Path("/a/b/c/e")));
        assertFalse(path.equals(new Path("/a/b/c/d/e")));
    }

    @Test
    public void testGetFileExtension() {
        assertNull(new Path("/a/b/c/").getFileExtension());
        assertNull(new Path("/a/b/c").getFileExtension());
        assertEquals("doc", new Path("/a/b/c.doc").getFileExtension());
    }

    @Test
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

        assertEquals(path, new Path("/a/b/c"));
    }

    @Test
    public void testFileExtension() {
        Path path = new Path("/a/b/c");
        assertNull(path.getFileExtension());

        path = new Path("/a/b/c.txt");
        assertEquals("txt", path.getFileExtension());
        assertEquals("/a/b/c", path.removeFileExtension().toString());

        path = new Path("/a/b/c/");
        assertNull(path.getFileExtension());
    }

    @Test
    public void testPathNormalisation() {
        Path path = new Path("////a/./b/../c");
        assertEquals("/a/c", path.toString());
    }

    @Test
    public void testEquality() {
        assertEquals(new Path("/a/b/c"), new Path("/a/b/c"));
        assertEquals(new Path("/a/b/c/"), new Path("/a/b/c/"));

        assert !new Path("/a/b/c").equals(new Path("/a/b"));
        assert !new Path("/a/b").equals(new Path("/a/b/c"));
        assert !new Path("/a/b/c/").equals(new Path("/a/b/c"));
        assert !new Path("/a/b/c").equals(new Path("/a/b/c/"));

        assert !new Path("/a/b/d").equals(new Path("/a/b/c"));
        assert !new Path("/a/d/c").equals(new Path("/a/b/c"));
        assert !new Path("/d/b/c").equals(new Path("/a/b/c"));
        assert !new Path("/a/b/c").equals(new Path("/a/b/d"));
        assert !new Path("/a/b/c").equals(new Path("/a/d/c"));
        assert !new Path("/a/b/c").equals(new Path("/d/b/c"));
    }

    @Test
    public void testAppend1() {
        Path path1 = new Path("/a/b/c");
        Path path2 = new Path("/d/e/f");
        Path path3 = new Path("/a/b/c/d/e/f");
        assertEquals(path3, path1.append(path2));

        assertEquals("/a/b/c", path1.append(".").toString());
        assertEquals("/a/b", path1.append("..").toString());

        assertEquals("/a/b/c", path1.append(new Path(".")).toString());
        assertEquals("/a/b", path1.append(new Path("..")).toString());
    }

}
