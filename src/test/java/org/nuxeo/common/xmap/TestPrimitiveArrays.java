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

package org.nuxeo.common.xmap;

import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

import org.nuxeo.common.collections.PrimitiveArrays;

public class TestPrimitiveArrays extends TestCase {
    private Collection myColl;
    private Object[] myColl2;

    public void testIntegerCase() {
        myColl = Arrays.asList(0, 1);

        int[] result = (int[]) PrimitiveArrays.toPrimitiveArray(myColl, Integer.TYPE);
        assertEquals(2, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);

        myColl2 = new Integer[] { 0, 1 };
        Integer[] result1 = (Integer[]) PrimitiveArrays.toObjectArray(myColl2);
        assertEquals(2, result1.length);
        assertEquals((Integer) 0, result1[0]);
        assertEquals((Integer) 1, result1[1]);
    }

    public void testLongCase() {
        myColl = Arrays.asList(0L, 1L);

        long[] result = (long[]) PrimitiveArrays.toPrimitiveArray(myColl, Long.TYPE);
        assertEquals(2, result.length, 2);
        assertEquals(0L, result[0]);
        assertEquals(1L, result[1]);

        myColl2 = new Long[] { 0L, 1L };
        Long[] result1 = (Long[]) PrimitiveArrays.toObjectArray(myColl2);
        assertEquals(2, result1.length);
        assertEquals((Long) 0L, result1[0]);
        assertEquals((Long) 1L, result1[1]);
    }

    public void testDoubleCase() {
        myColl = Arrays.asList(0.0, 1.0);

        double[] result = (double[]) PrimitiveArrays.toPrimitiveArray(myColl, Double.TYPE);
        assertEquals(2, result.length);
        assertEquals(0.0, result[0]);
        assertEquals(1.0, result[1]);

        myColl2 = new Double[] { 0.0, 1.0 };
        Double[] result1 = (Double[]) PrimitiveArrays.toObjectArray(myColl2);
        assertEquals(2, result1.length);
        assertEquals(0.0, result1[0]);
        assertEquals(1.0, result1[1]);
    }

}
