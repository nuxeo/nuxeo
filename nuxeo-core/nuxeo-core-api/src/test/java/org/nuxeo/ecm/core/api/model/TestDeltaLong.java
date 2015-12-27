/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDeltaLong {

    @Test
    public void testDeltaLong() {
        DeltaLong dl = new DeltaLong(100, 123);
        assertEquals(100, dl.getBase());
        assertEquals(123, dl.getDelta());
        assertEquals(223, dl.longValue());
        assertEquals(223, dl.intValue());
        assertEquals(223f, dl.floatValue(), 0.0f);
        assertEquals(223d, dl.doubleValue(), 0.0d);
        assertEquals(Long.valueOf(223), dl.getFullValue());
        assertEquals(Long.valueOf(123), dl.getDeltaValue());
        assertEquals("223", dl.toString());
    }

    @Test
    public void testDeltaLong_addDelta() {
        DeltaLong dl1 = new DeltaLong(100, 123);
        DeltaLong dl2 = new DeltaLong(1000, 555);
        DeltaLong added = (DeltaLong) dl1.add(dl2);
        assertEquals(100, added.getBase());
        assertEquals(123 + 555, added.getDelta());
    }

    @Test
    public void testDeltaLong_addNumber() {
        DeltaLong dl = new DeltaLong(100, 123);
        Long n = Long.valueOf(1000);
        Long added = (Long) dl.add(n);
        assertEquals(1123, added.longValue());
    }

    @Test
    public void testDeltaLong_deltaOrLong() {
        Number base;
        Number n;
        DeltaLong dl;

        base = null;
        n = DeltaLong.deltaOrLong(base, 123);
        assertTrue(n instanceof Long);
        assertEquals(Long.valueOf(123), (Long) n);

        base = Long.valueOf(100);
        n = DeltaLong.deltaOrLong(base, 111);
        assertTrue(n instanceof DeltaLong);
        dl = (DeltaLong) n;
        assertEquals(100, dl.getBase());
        assertEquals(111, dl.getDelta());

        base = new DeltaLong(100, 123);
        n = DeltaLong.deltaOrLong(base, 111);
        assertTrue(n instanceof DeltaLong);
        dl = (DeltaLong) n;
        assertEquals(100, dl.getBase());
        assertEquals(234, dl.getDelta());

        n = DeltaLong.deltaOrLong(base, 0);
        assertSame(base, n);
    }

    @Test
    public void testEquals() {
        DeltaLong dl1 = new DeltaLong(100, 123);
        DeltaLong dl2 = new DeltaLong(0, 223);
        assertEquals(dl1, dl1);

        assertEquals(dl1.longValue(), dl2.longValue());
        assertFalse(dl1.equals(dl2));
    }

}
