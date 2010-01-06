package org.nuxeo.chemistry.shell;

import org.junit.Assert;
import org.junit.Test;

public class TestPath extends Assert {

    @Test
    public void test1() {
        Path p = new Path("abc/asdf/file.ext");
        assertEquals("abc/asdf/file.ext", p.toString());
    }

    @Test
    public void test2() {
        Path p = new Path("/abc/asdf/file.ext");
        assertEquals("/abc/asdf/file.ext", p.toString());
    }

    @Test
    public void test3() {
        Path p = new Path("/./abc//asdf/../file.ext");
        assertEquals("/abc/file.ext", p.toString());
    }

    @Test
    public void test4() {
        Path p = new Path("/commands");

        p = p.append("test");
        assertEquals("/commands/test", p.toString());

        p = p.append("../../../test2");
        assertEquals("../test2", p.toString());

        p = p.makeAbsolute();
        assertEquals("/test2", p.toString());

        p = p.append("..");
        assertEquals("/", p.toString());

        p = p.append("..");
        assertEquals("/", p.toString());
    }

}
