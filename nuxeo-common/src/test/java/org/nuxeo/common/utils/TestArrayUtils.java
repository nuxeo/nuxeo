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

import java.util.Arrays;

import junit.framework.TestCase;

public class TestArrayUtils extends TestCase {

    public void testArraysJoinSimple1() {

        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "b", "d" };

        String[] result = ArrayUtils.intersect(a1, a2);

        String[] expected = { "a", "b" };

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysJoinSimple2() {

        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "b", "d" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = { "b" };

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysJoinSimple3() {

        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "x", "y", "z" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysJoinSimple4() {

        String[] a1 = {};
        String[] a2 = { "b", "y", "z" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysJoinSimple5() {

        String[] a1 = { "b", "y", "z" };
        String[] a2 = {};
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysMerge() {

        String[] a1 = { "b", "y", "z" };
        String[] a2 = {};
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.arrayMerge(a1, a2, a3);

        String[] expected = { "b", "y", "z", "b", "d", "e", "f" };

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysMerge2() {

        String[] a1 = {};
        String[] a2 = { "b", "y", "z" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.arrayMerge(a1, a2, a3);

        String[] expected = { "b", "y", "z", "b", "d", "e", "f" };

        assertTrue(Arrays.equals(expected, result));
    }

    public void testArraysMerge3() {

        String[] a1 = {};
        String[] a2 = {};

        String[] result = ArrayUtils.arrayMerge(a1, a2);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

}
