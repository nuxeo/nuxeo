/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.utils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestPattern extends TestCase {

    public void testFileNamePattern() {

        FileNamePattern pattern;

        pattern = new FileNamePattern("");
        Assert.assertTrue(pattern.match(""));
        Assert.assertFalse(pattern.match("a"));

        pattern = new FileNamePattern("abc.txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertFalse(pattern.match("ac.txt"));


        pattern = new FileNamePattern("?");
        Assert.assertTrue(pattern.match("a"));
        Assert.assertFalse(pattern.match("ab"));

        pattern = new FileNamePattern("*");
        Assert.assertTrue(pattern.match("abababa"));
        Assert.assertTrue(pattern.match(""));

        pattern = new FileNamePattern("*.txt");
        Assert.assertTrue(pattern.match("a.txt"));
        Assert.assertTrue(pattern.match("a.b.txt"));
        Assert.assertFalse(pattern.match("a:txt"));
        Assert.assertFalse(pattern.match("a.txtz"));

        pattern = new FileNamePattern("*abc.txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertTrue(pattern.match("xabc.txt"));
        Assert.assertTrue(pattern.match("xyzabc.txt"));
        Assert.assertFalse(pattern.match("ac.txt"));

        pattern = new FileNamePattern("a*bc.txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertTrue(pattern.match("axbc.txt"));
        Assert.assertTrue(pattern.match("axyzbc.txt"));
        Assert.assertFalse(pattern.match("ac.txt"));

        pattern = new FileNamePattern("abc*txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertTrue(pattern.match("abctxt"));
        Assert.assertTrue(pattern.match("abcxyztxt"));
        Assert.assertFalse(pattern.match("ac.txt"));

        pattern = new FileNamePattern("abc.t*t");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertTrue(pattern.match("abc.tt"));
        Assert.assertTrue(pattern.match("abc.txyzt"));
        Assert.assertFalse(pattern.match("abc.t"));

        pattern = new FileNamePattern("abc.*");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertTrue(pattern.match("abc."));
        Assert.assertTrue(pattern.match("abc.t"));
        Assert.assertFalse(pattern.match("ab.txt"));

        pattern = new FileNamePattern("?bc.txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertFalse(pattern.match("zabc.txt"));
        Assert.assertFalse(pattern.match("abc.txy"));
        Assert.assertFalse(pattern.match("abc.t"));

        pattern = new FileNamePattern("abc.???");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertFalse(pattern.match("zabc.txt"));
        Assert.assertTrue(pattern.match("abc.txy"));
        Assert.assertFalse(pattern.match("abc.t"));

        pattern = new FileNamePattern("*?txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertFalse(pattern.match("abc.tt"));
        Assert.assertTrue(pattern.match("abctxt"));
        Assert.assertTrue(pattern.match(".txt"));

        pattern = new FileNamePattern("abc*?txt");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertFalse(pattern.match("abc.tt"));
        Assert.assertFalse(pattern.match("abctxt"));
        Assert.assertTrue(pattern.match("abcdef.txt"));

        pattern = new FileNamePattern("abc.t*?");
        Assert.assertTrue(pattern.match("abc.txt"));
        Assert.assertTrue(pattern.match("abc.tt"));
        Assert.assertTrue(pattern.match("abc.txyzt"));
        Assert.assertFalse(pattern.match("abc.t"));

        pattern = new FileNamePattern("*.jar");
        Assert.assertTrue(pattern.match("abc.jar"));
        Assert.assertTrue(pattern.match("a.b.c.jar"));
        Assert.assertFalse(pattern.match("jar.j.ja."));
        Assert.assertFalse(pattern.match("jar.j.jar."));
        Assert.assertTrue(pattern.match("jar.j.ja.jar"));
        Assert.assertTrue(pattern.match(".jar"));
        Assert.assertFalse(pattern.match("abc-jar"));

        pattern = new FileNamePattern("nuxeo?*common*.jar");
        Assert.assertTrue(pattern.match("nuxeo-common.jar"));
        Assert.assertTrue(pattern.match("nuxeo--common.jar"));
        Assert.assertTrue(pattern.match("nuxeo-common-SNAPSHOT-1.0.jar"));
        Assert.assertFalse(pattern.match("nuxeocommon.jar"));

        pattern = new FileNamePattern("???*");
        Assert.assertTrue(pattern.match("abcdef"));
        Assert.assertTrue(pattern.match("xyz"));
        Assert.assertFalse(pattern.match("ab"));
        Assert.assertFalse(pattern.match("x"));

        pattern = new FileNamePattern("??*?**");
        Assert.assertTrue(pattern.match("abcdef"));
        Assert.assertTrue(pattern.match("xyz"));
        Assert.assertFalse(pattern.match("ab"));
        Assert.assertFalse(pattern.match("x"));

        pattern = new FileNamePattern("*???");
        Assert.assertTrue(pattern.match("abcdef"));
        Assert.assertTrue(pattern.match("xyz"));
        Assert.assertFalse(pattern.match("ab"));
        Assert.assertFalse(pattern.match("x"));

    }

    public void testFilePathPattern() {
        FilePathPattern pattern = new FilePathPattern("a/b/c/d");
        Assert.assertTrue(pattern.match("a/b/c/d"));
        Assert.assertTrue(pattern.match("a/b//c/d"));
        Assert.assertFalse(pattern.match("a/b/c"));

        pattern = new FilePathPattern("**/b/c");
        Assert.assertTrue(pattern.match("b/c"));
        Assert.assertTrue(pattern.match("a/b/c"));
        Assert.assertTrue(pattern.match("x/a/b/c"));
        Assert.assertFalse(pattern.match("b/c/d"));

        pattern = new FilePathPattern("b/c/**");
        Assert.assertTrue(pattern.match("b/c"));
        Assert.assertTrue(pattern.match("b/c/d"));
        Assert.assertTrue(pattern.match("b/c/d/e"));
        Assert.assertFalse(pattern.match("a/b/c"));

        pattern = new FilePathPattern("a/**/c");
        Assert.assertTrue(pattern.match("a/c"));
        Assert.assertTrue(pattern.match("a/b/c"));
        Assert.assertTrue(pattern.match("a/b/x/c"));
        Assert.assertFalse(pattern.match("b/c/d"));
        Assert.assertFalse(pattern.match("a/b/c/d"));
    }

    public void testIndexOf() {
        Assert.assertEquals(0, FileNamePattern.indexOf("abc".toCharArray(), "a".toCharArray(), 0));
        Assert.assertEquals(1, FileNamePattern.indexOf("abc".toCharArray(), "b".toCharArray(), 0));
        Assert.assertEquals(2, FileNamePattern.indexOf("abc".toCharArray(), "c".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("abc".toCharArray(), "ab".toCharArray(), 0));
        Assert.assertEquals(1, FileNamePattern.indexOf("abc".toCharArray(), "bc".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("abc".toCharArray(), "abc".toCharArray(), 0));
        Assert.assertEquals(-1, FileNamePattern.indexOf("abc".toCharArray(), "ac".toCharArray(), 0));

        Assert.assertEquals(2, FileNamePattern.indexOf("abcdef".toCharArray(), "cde".toCharArray(), 0));
        Assert.assertEquals(2, FileNamePattern.indexOf("abcdef".toCharArray(), "cdef".toCharArray(), 0));
        Assert.assertEquals(-1, FileNamePattern.indexOf("abcdef".toCharArray(), "cdefg".toCharArray(), 0));
        Assert.assertEquals(-1, FileNamePattern.indexOf("abcdef".toCharArray(), "cdf".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("abcdef".toCharArray(), "abc".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("abcdef".toCharArray(), "abcdef".toCharArray(), 0));
        Assert.assertEquals(-1, FileNamePattern.indexOf("abcdef".toCharArray(), "abd".toCharArray(), 0));
        Assert.assertEquals(2, FileNamePattern.indexOf("abcdef".toCharArray(), "c".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("abcdef".toCharArray(), "a".toCharArray(), 0));
        Assert.assertEquals(5, FileNamePattern.indexOf("abcdef".toCharArray(), "f".toCharArray(), 0));
        Assert.assertEquals(3, FileNamePattern.indexOf("abcdef".toCharArray(), "de".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("abcdef".toCharArray(), "".toCharArray(), 0));
        Assert.assertEquals(0, FileNamePattern.indexOf("".toCharArray(), "".toCharArray(), 0));
        Assert.assertEquals(-1, FileNamePattern.indexOf("".toCharArray(), "a".toCharArray(), 0));
        Assert.assertEquals(-1, FileNamePattern.indexOf("a".toCharArray(), "ab".toCharArray(), 0));

        Assert.assertEquals(2, FileNamePattern.indexOf("abc.txt".toCharArray(), "?.txt".toCharArray(), 0));
    }


    public void testContainsAt() {
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "a".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 1, "b".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 2, "c".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "ab".toCharArray()));
        Assert.assertFalse(FileNamePattern.containsAt("abc".toCharArray(), 0, "ac".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "abc".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 1, "bc".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "".toCharArray()));

        Assert.assertFalse(FileNamePattern.containsAt("abc".toCharArray(), 1, "c".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 1, "b".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 1, "bc".toCharArray()));
        Assert.assertFalse(FileNamePattern.containsAt("abc".toCharArray(), 1, "c".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 2, "c".toCharArray()));

        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "a?c".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "ab?".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 0, "?bc".toCharArray()));
        Assert.assertTrue(FileNamePattern.containsAt("abc".toCharArray(), 1, "??".toCharArray()));

    }


}
