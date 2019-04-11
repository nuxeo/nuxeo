/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestStringUtils.java 27204 2007-11-14 19:14:10Z gracinet $
 */

package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestStringUtils {

    @Test
    public void testToAscii() throws UnsupportedEncodingException {
        String s = "h\u00E9h\u00E9";
        assertEquals("hehe", StringUtils.toAscii(s));
    }

    @Test
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

        str = " , , a , b, c, d  ,  ,  ";
        ar = StringUtils.split(str, ',', false);
        assertTrue(Arrays.equals(new String[] { " ", " ", " a ", " b", " c", " d  ", "  ", "  " }, ar));
        ar = StringUtils.split(str, ',', true);
        assertTrue(Arrays.equals(new String[] { "", "", "a", "b", "c", "d", "", "" }, ar));
    }

    /**
     * @since 9.1
     */
    @Test
    public void testSplitWithEscape() {

        String str = " , , a , b, c, d  ,  ,  ";
        List<String> li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { " ", " ", " a ", " b", " c", " d  ", "  ", "  " },
                li.toArray(new String[li.size()])));
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(
                Arrays.equals(new String[] { "", "", "a", "b", "c", "d", "", "" }, li.toArray(new String[li.size()])));

        str = " , , a , b, c, d  ,  ,";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { " ", " ", " a ", " b", " c", " d  ", "  ", "" },
                li.toArray(new String[li.size()])));
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(
                Arrays.equals(new String[] { "", "", "a", "b", "c", "d", "", "" }, li.toArray(new String[li.size()])));

        str = "a , b\\,aobad, c,\n d";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "a ", " b,aobad", " c", "\n d" }, li.toArray(new String[li.size()])));
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(Arrays.equals(new String[] { "a", "b,aobad", "c", "d" }, li.toArray(new String[li.size()])));

        str = "a , b\\\\,aobad, c, d\\";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(
                Arrays.equals(new String[] { "a ", " b\\", "aobad", " c", " d\\" }, li.toArray(new String[li.size()])));
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(Arrays.equals(new String[] { "a", "b\\", "aobad", "c", "d\\" }, li.toArray(new String[li.size()])));

        str = "a | b\\|aobad| c|\n d";
        li = StringUtils.split(str, '|', '\\', false);
        assertTrue(Arrays.equals(new String[] { "a ", " b|aobad", " c", "\n d" }, li.toArray(new String[li.size()])));
        li = StringUtils.split(str, '|', '\\', true);
        assertTrue(Arrays.equals(new String[] { "a", "b|aobad", "c", "d" }, li.toArray(new String[li.size()])));

        str = "a \\ b\\\\aobad\\ c\\\n d";
        try {
            li = StringUtils.split(str, '\\', '\\', false);
            fail("Using the same character for escape and delimiter should not be possible");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            li = StringUtils.split(str, '\\', '\\', true);
            fail("Using the same character for escape and delimiter should not be possible");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        str = "foo\\\\bar";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "foo\\bar" }, li.toArray(new String[li.size()])));

        str = "toto,foo\\\\bar";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "toto", "foo\\bar" }, li.toArray(new String[li.size()])));

        str = "foo\\,bar";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "foo,bar" }, li.toArray(new String[li.size()])));

        str = "foo\\,bar,foo\\\\bar";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "foo,bar", "foo\\bar" }, li.toArray(new String[li.size()])));

        str = "foo\\\\\\,bar";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "foo\\,bar" }, li.toArray(new String[li.size()])));

        str = "foo\\\\,bar";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "foo\\", "bar" }, li.toArray(new String[li.size()])));

        str = "foo\\zbar";
        li = StringUtils.split("foo\\zbar", ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "foo\\zbar" }, li.toArray(new String[li.size()])));

        str = "";
        li = StringUtils.split(str, ',', '\\', false);
        assertTrue(Arrays.equals(new String[] { "" }, li.toArray(new String[li.size()])));

        str = "\\,";
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(Arrays.equals(new String[] { "," }, li.toArray(new String[li.size()])));

        str = "\\,a";
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(Arrays.equals(new String[] { ",a" }, li.toArray(new String[li.size()])));

        str = "a\\,";
        li = StringUtils.split(str, ',', '\\', true);
        assertTrue(Arrays.equals(new String[] { "a," }, li.toArray(new String[li.size()])));
    }

}
