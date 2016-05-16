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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDeltaLong {

    @Test
    public void testDeltaLong() {
        DeltaLong dl = DeltaLong.valueOf(Long.valueOf(100), 123);
        assertEquals(Long.valueOf(100), dl.getBase());
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
    public void testDeltaLongNull() {
        DeltaLong dl = DeltaLong.valueOf(null, 123);
        assertNull(dl.getBase());
        assertEquals(123, dl.getDelta());
        assertEquals(123, dl.longValue());
        assertEquals(123, dl.intValue());
        assertEquals(123f, dl.floatValue(), 0.0f);
        assertEquals(123d, dl.doubleValue(), 0.0d);
        assertEquals(Long.valueOf(123), dl.getFullValue());
        assertEquals(Long.valueOf(123), dl.getDeltaValue());
        assertEquals("123", dl.toString());
    }

    @Test
    public void testDeltaLong_addDelta() {
        DeltaLong dl1 = DeltaLong.valueOf(Long.valueOf(100), 123);
        DeltaLong dl2 = DeltaLong.valueOf(Long.valueOf(1000), 555);
        DeltaLong added = (DeltaLong) dl1.add(dl2);
        assertEquals(Long.valueOf(100), added.getBase());
        assertEquals(123 + 555, added.getDelta());
    }

    @Deprecated
    @Test
    public void testDeltaLong_addNumber() {
        DeltaLong dl = DeltaLong.valueOf(Long.valueOf(100), 123);
        Long n = Long.valueOf(1000);
        Long added = (Long) dl.add(n);
        assertEquals(1123, added.longValue());
    }

    @Test
    public void testDeltaLong_of() {
        Number base;
        Number n;
        DeltaLong dl;

        base = null;
        n = DeltaLong.valueOf(base, 123);
        assertTrue(n instanceof DeltaLong);
        dl = (DeltaLong) n;
        assertNull(dl.getBase());
        assertEquals(123, dl.getDelta());

        base = Long.valueOf(100);
        n = DeltaLong.valueOf(base, 111);
        assertTrue(n instanceof DeltaLong);
        dl = (DeltaLong) n;
        assertEquals(Long.valueOf(100), dl.getBase());
        assertEquals(111, dl.getDelta());

        base = DeltaLong.valueOf(Long.valueOf(100), 123);
        n = DeltaLong.valueOf(base, 111);
        assertTrue(n instanceof DeltaLong);
        dl = (DeltaLong) n;
        assertEquals(Long.valueOf(100), dl.getBase());
        assertEquals(234, dl.getDelta());

        n = DeltaLong.valueOf(base, 0);
        assertSame(base, n);
    }

    @Test
    public void testEquals() {
        DeltaLong dl0 = DeltaLong.valueOf(null, 123);
        DeltaLong dl0b = DeltaLong.valueOf(null, 123);
        DeltaLong dl1 = DeltaLong.valueOf(Long.valueOf(100), 123);
        DeltaLong dl1b = DeltaLong.valueOf(Long.valueOf(100), 123);
        DeltaLong dl2 = DeltaLong.valueOf(Long.valueOf(0), 223);
        DeltaLong dl2b = DeltaLong.valueOf(Long.valueOf(0), 223);
        assertEquals(dl1.longValue(), dl2.longValue());

        assertEquals(dl0, dl0);
        assertEquals(dl0, dl0b);
        assertEquals(dl1, dl1);
        assertEquals(dl1, dl1b);
        assertEquals(dl2, dl2);
        assertEquals(dl2, dl2b);

        assertFalse(dl0.equals(dl1));
        assertFalse(dl0.equals(dl2));
        assertFalse(dl1.equals(dl0));
        assertFalse(dl1.equals(dl2));
        assertFalse(dl2.equals(dl0));
        assertFalse(dl2.equals(dl1));
    }

}
