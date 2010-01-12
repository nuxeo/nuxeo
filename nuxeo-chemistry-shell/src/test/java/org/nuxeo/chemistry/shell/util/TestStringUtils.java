package org.nuxeo.chemistry.shell.util;

import org.junit.Assert;
import org.junit.Test;

public class TestStringUtils extends Assert {

    @Test
    public void testSplit() {
        String[] res = StringUtils.split("abc", '|', true);
        assertArrayEquals(new String[] {"abc"}, res);

        res = StringUtils.split(" abc ", '|', true);
        assertArrayEquals(new String[] {"abc"}, res);

        res = StringUtils.split(" abc ", '|', false);
        assertArrayEquals(new String[] {" abc "}, res);

        res = StringUtils.split("a|b|c", '|', true);
        assertArrayEquals(new String[] {"a", "b", "c"}, res);

        res = StringUtils.split(" a | b |c ", '|', true);
        assertArrayEquals(new String[] {"a", "b", "c"}, res);

        res = StringUtils.split(" a | b |c ", '|', false);
        assertArrayEquals(new String[] {" a ", " b ", "c "}, res);
    }

    @Test
    public void testTokenizeSimple() {
        String[] res = StringUtils.tokenize("a bc def");
        assertArrayEquals(new String[] {"a", "bc", "def"}, res);
    }

    @Test
    public void testTokenizeEscape() {
        String[] res = StringUtils.tokenize("a\\ bc def");
        assertArrayEquals(new String[] {"a bc", "def"}, res);

        res = StringUtils.tokenize("a\\\\\\n\\t def");
        assertArrayEquals(new String[] {"a\\\n\t", "def"}, res);

        res = StringUtils.tokenize("a\\bc def");
        assertArrayEquals(new String[] {"abc", "def"}, res);
    }

    @Test
    public void testTokenizeString() {
        String[] res = StringUtils.tokenize("a \"bc def\"");
        assertArrayEquals(new String[] {"a", "bc def"}, res);
    }

    @Test
    // "" is stronger than \
    public void testTokenizeBoth() {
        String[] res = StringUtils.tokenize("a \"bc\\ \\ndef\"");
        assertArrayEquals(new String[] {"a", "bc\\ \\ndef"}, res);
    }

}
