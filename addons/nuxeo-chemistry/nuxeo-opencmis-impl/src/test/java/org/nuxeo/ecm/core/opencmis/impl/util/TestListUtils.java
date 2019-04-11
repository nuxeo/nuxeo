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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.util;

import static org.nuxeo.ecm.core.opencmis.impl.util.ListUtils.getBatchedList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.opencmis.impl.util.ListUtils.BatchedList;

public class TestListUtils {

    @Test
    public void testBatchList() throws Exception {
        List<String> list = Arrays.asList("1", "2", "3", "4", "5", "6");
        BatchedList<String> l;
        l = getBatchedList(list, null, null, 10);
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), null, 10);
        assertEquals(Arrays.asList("1", "2", "3", "4"), l.getList());
        assertEquals(6, l.numItems);
        assertTrue(l.hasMoreItems);
        l = getBatchedList(list, null, null, 4);
        assertEquals(Arrays.asList("1", "2", "3", "4"), l.getList());
        assertEquals(6, l.numItems);
        assertTrue(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(6), null, 10);
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(42), null, 10);
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(1), 10);
        assertEquals(Arrays.asList("2", "3", "4", "5"), l.getList());
        assertEquals(6, l.numItems);
        assertTrue(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(2), 10);
        assertEquals(Arrays.asList("3", "4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(3), 10);
        assertEquals(Arrays.asList("4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(5), 10);
        assertEquals(Arrays.asList("6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(6), 10);
        assertEquals(Arrays.asList(), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(1), BigInteger.valueOf(42), 10);
        assertEquals(Arrays.asList(), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
    }

}
