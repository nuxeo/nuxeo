package org.nuxeo.runtime.binding;

import junit.framework.TestCase;

import javax.naming.InvalidNameException;
import javax.naming.Name;

public class TestJndiName extends TestCase {

    public void testAccessors() {
        JndiName name = new JndiName("/a/b/c");

        assertEquals(3, name.size());
        assertEquals("a", name.get(0));
        assertEquals("b", name.get(1));
        assertEquals("c", name.get(2));
    }

    public void testComparators() {
        Name n1 = new JndiName("/a/b/c");
        Name n2 = new JndiName("a", "b", "c");
        Name n3 = new JndiName("//a///b/c///");
        Name n4 = new JndiName("/./a/b/../b/c");

        Name n5 = new JndiName("/a/b");
        Name n6 = new JndiName("/c/d");

        Name n7 = new JndiName();
        Name n8 = new JndiName("");

        assertEquals(n1, n1);
        assertEquals(n1, n2);
        assertEquals(n1, n3);
        assertEquals(n1, n4);

        assertEquals(n7, n8);

        assertEquals(n1.hashCode(), n2.hashCode());
        assertEquals(n1.hashCode(), n3.hashCode());
        assertEquals(n1.hashCode(), n4.hashCode());
        assertEquals(0, n1.compareTo(n2));
        assertEquals(0, n1.compareTo(n3));
        assertEquals(0, n1.compareTo(n4));

        assertFalse(n1.equals(null));
        assertFalse(n1.equals(n5));
        assertFalse(n1.equals(n6));
        assertFalse(n2.equals(n5));
        assertFalse(n2.equals(n6));
        assertFalse(n5.equals(n6));

        assertTrue(n1.startsWith(new JndiName("")));
        assertTrue(n1.startsWith(new JndiName("/a")));
        assertTrue(n1.startsWith(new JndiName("/a/b")));
        assertTrue(n1.startsWith(new JndiName("/a/b/c")));

        assertFalse(n1.startsWith(new JndiName("/d")));

        assertTrue(n1.endsWith(new JndiName("")));
        assertTrue(n1.endsWith(new JndiName("/c")));
        assertTrue(n1.endsWith(new JndiName("/b/c")));
        assertTrue(n1.endsWith(new JndiName("/a/b/c")));

        assertFalse(n1.endsWith(new JndiName("/d")));

        assertEquals(new JndiName("/a/b"), n1.getPrefix(2));
        assertEquals(new JndiName("/c"), n1.getSuffix(2));

        assertEquals(n1, n1.clone());
    }

    public void testEmpty() {
        Name n1 = new JndiName();
        Name n2 = new JndiName("");
        Name n3 = new JndiName("/");
        Name n4 = new JndiName(".");

        assertTrue(n1.isEmpty());
        assertEquals(0, n1.size());
        assertEquals(n1, n2);
        assertEquals(n1, n3);
        assertEquals(n1, n4);
    }

    public void testOperations() throws InvalidNameException {
        JndiName n1 = new JndiName("/a");

        Name n2 = n1.add("b");
        assertEquals(n1, n2);
        assertEquals(new JndiName("/a/b"), n1);

        Name n3 = n1.add(0, "c");
        assertEquals(n1, n3);
        assertEquals(new JndiName("/c/a/b"), n1);

        Name n4 = n1.addAll(new JndiName("/d/e"));
        assertEquals(n1, n4);
        assertEquals(new JndiName("/c/a/b/d/e"), n1);

        Name n5 = n1.addAll(0, new JndiName("/f"));
        assertEquals(n1, n5);
        assertEquals(new JndiName("/f/c/a/b/d/e"), n1);

        Name n6 = n1.addAll(1, new JndiName("/g"));
        assertEquals(n1, n6);
        assertEquals(new JndiName("/f/g/c/a/b/d/e"), n1);

        Name n7 = n1.addAll(n1.size(), new JndiName("/h"));
        assertEquals(n1, n7);
        assertEquals(new JndiName("/f/g/c/a/b/d/e/h"), n1);
    }

}
