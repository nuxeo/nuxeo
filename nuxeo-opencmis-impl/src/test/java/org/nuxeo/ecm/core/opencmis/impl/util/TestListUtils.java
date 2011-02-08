/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.util;

import static org.nuxeo.ecm.core.opencmis.impl.util.ListUtils.getBatchedList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.opencmis.impl.util.ListUtils.BatchedList;

import junit.framework.TestCase;

public class TestListUtils extends TestCase {

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
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(1),
                10);
        assertEquals(Arrays.asList("2", "3", "4", "5"), l.getList());
        assertEquals(6, l.numItems);
        assertTrue(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(2),
                10);
        assertEquals(Arrays.asList("3", "4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(3),
                10);
        assertEquals(Arrays.asList("4", "5", "6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(5),
                10);
        assertEquals(Arrays.asList("6"), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(4), BigInteger.valueOf(6),
                10);
        assertEquals(Arrays.asList(), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
        l = getBatchedList(list, BigInteger.valueOf(1), BigInteger.valueOf(42),
                10);
        assertEquals(Arrays.asList(), l.getList());
        assertEquals(6, l.numItems);
        assertFalse(l.hasMoreItems);
    }

}
