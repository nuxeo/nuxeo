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

package org.nuxeo.common.xmap;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.nuxeo.common.collections.PrimitiveArrays;

public class TestPrimitiveArrays {
    @SuppressWarnings("rawtypes")
    private Collection myColl;

    private Object[] myColl2;

    @Test
    public void testIntegerCase() {
        myColl = Arrays.asList(0, 1);

        @SuppressWarnings("unchecked")
        int[] result = (int[]) PrimitiveArrays.toPrimitiveArray(myColl,
                Integer.TYPE);
        assertEquals(2, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);

        myColl2 = new Integer[] { 0, 1 };
        Integer[] result1 = (Integer[]) PrimitiveArrays.toObjectArray(myColl2);
        assertEquals(2, result1.length);
        assertEquals(Integer.valueOf(0), result1[0]);
        assertEquals(Integer.valueOf(1), result1[1]);
    }

    @Test
    public void testLongCase() {
        myColl = Arrays.asList(0L, 1L);

        @SuppressWarnings("unchecked")
        long[] result = (long[]) PrimitiveArrays.toPrimitiveArray(myColl,
                Long.TYPE);
        assertEquals(2, result.length, 2);
        assertEquals(0L, result[0]);
        assertEquals(1L, result[1]);

        myColl2 = new Long[] { 0L, 1L };
        Long[] result1 = (Long[]) PrimitiveArrays.toObjectArray(myColl2);
        assertEquals(2, result1.length);
        assertEquals(Long.valueOf(0), result1[0]);
        assertEquals(Long.valueOf(1), result1[1]);
    }

    @Test
    public void testDoubleCase() {
        myColl = Arrays.asList(0.0, 1.0);

        @SuppressWarnings("unchecked")
        double[] result = (double[]) PrimitiveArrays.toPrimitiveArray(myColl,
                Double.TYPE);
        assertEquals(2, result.length);
        assertEquals(0.0, result[0], 1e-8);
        assertEquals(1.0, result[1], 1e-8);

        myColl2 = new Double[] { 0.0, 1.0 };
        Double[] result1 = (Double[]) PrimitiveArrays.toObjectArray(myColl2);
        assertEquals(2, result1.length);
        assertEquals(0.0, result1[0], 1e-8);
        assertEquals(1.0, result1[1], 1e-8);
    }

}
