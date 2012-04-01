/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.common.utils;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class TestArrayUtils {

    @Test
    public void testArraysJoinSimple1() {
        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "b", "d" };

        String[] result = ArrayUtils.intersect(a1, a2);

        String[] expected = { "a", "b" };

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysJoinSimple2() {
        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "b", "d" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = { "b" };

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysJoinSimple3() {
        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "x", "y", "z" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysJoinSimple4() {
        String[] a1 = {};
        String[] a2 = { "b", "y", "z" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysJoinSimple5() {
        String[] a1 = { "b", "y", "z" };
        String[] a2 = {};
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.intersect(a1, a2, a3);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysMerge() {
        String[] a1 = { "b", "y", "z" };
        String[] a2 = {};
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.arrayMerge(a1, a2, a3);

        String[] expected = { "b", "y", "z", "b", "d", "e", "f" };

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysMerge2() {
        String[] a1 = {};
        String[] a2 = { "b", "y", "z" };
        String[] a3 = { "b", "d", "e", "f" };

        String[] result = ArrayUtils.arrayMerge(a1, a2, a3);

        String[] expected = { "b", "y", "z", "b", "d", "e", "f" };

        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArraysMerge3() {
        String[] a1 = {};
        String[] a2 = {};

        String[] result = ArrayUtils.arrayMerge(a1, a2);

        String[] expected = {};

        assertTrue(Arrays.equals(expected, result));
    }

}
