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
 * $Id: TestStringUtils.java 27204 2007-11-14 19:14:10Z gracinet $
 */

package org.nuxeo.common.utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestStringUtils extends TestCase {

    public void testToAscii() throws UnsupportedEncodingException {
        String s = "h\u00E9h\u00E9";
        assertEquals("hehe", StringUtils.toAscii(s));
    }

    public void testSplit() {
        String str;
        String[] ar;

        str = ",a ,,, b";
        ar = StringUtils.split(str, ',', false);
        assertTrue(Arrays.equals(new String[] { "", "a ", "", "", " b" }, ar));
        ar = StringUtils.split(str, ',', true);
        assertTrue(Arrays.equals(new String[] { "", "a", "", "", "b" }, ar));

        str = "a , b, c,\n d";
        ar = StringUtils.split(str, ',', false);
        assertTrue(Arrays.equals(new String[] { "a ", " b", " c", "\n d" }, ar));
        ar = StringUtils.split(str, ',', true);
        assertTrue(Arrays.equals(new String[] { "a", "b", "c", "d" }, ar));

        str = "a , b, c, d,";
        ar = StringUtils.split(str, ',', false);
        assertTrue(Arrays.equals(new String[] { "a ", " b", " c", " d", "" }, ar));
        ar = StringUtils.split(str, ',', true);
        assertTrue(Arrays.equals(new String[] { "a", "b", "c", "d", "" }, ar));

        str  = " , , a , b, c, d  ,  ,  ";
        ar = StringUtils.split(str, ',', false);
        assertTrue(Arrays.equals(
                new String[] { " ", " ", " a ", " b", " c", " d  ", "  ", "  " }, ar));
        ar = StringUtils.split(str, ',', true);
        assertTrue(Arrays.equals(
                new String[] { "", "", "a", "b", "c", "d", "", "" }, ar));
    }

    public void testJoin() {
        String[] ar;

        // String[]
        assertNull(StringUtils.join((String[]) null, "()"));
        assertNull(StringUtils.join((String[]) null, ','));
        assertNull(StringUtils.join((String[]) null, null));
        assertNull(StringUtils.join((String[]) null));

        assertEquals("", StringUtils.join(new String[0]));

        ar = new String[] { "a", "b", "", "c", null, "d"};
        assertEquals("a()b()()c()()d", StringUtils.join(ar, "()"));
        assertEquals("abcd", StringUtils.join(ar, null));
        assertEquals("abcd", StringUtils.join(ar));

        // List<String>
        assertNull(StringUtils.join((List<String>) null, null));
        assertNull(StringUtils.join((List<String>) null, "()"));
        assertNull(StringUtils.join((List<String>) null, ','));
        assertNull(StringUtils.join((List<String>) null));

        List<String> li = new LinkedList<String>();
        assertEquals("", StringUtils.join(li, "()"));
        assertEquals("", StringUtils.join(li, ','));
        assertEquals("", StringUtils.join(li));

        li.add("a");
        li.add("b");
        li.add("");
        li.add("c");
        li.add(null);
        li.add("d");
        assertEquals("a()b()()c()()d", StringUtils.join(li, "()"));
        assertEquals("a,b,,c,,d", StringUtils.join(li, ','));
        assertEquals("abcd", StringUtils.join(li, null));
        assertEquals("abcd", StringUtils.join(li));
    }

    public void testTodHex() {
        assertEquals("", StringUtils.toHex(""));
        assertEquals("746F746F", StringUtils.toHex("toto"));
    }
}
