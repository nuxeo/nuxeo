/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

public class TestDBSInvalidations {

    @Test
    public void testSerialization() throws IOException {
        DBSInvalidations invals;
        @SuppressWarnings("resource")
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        String ser;

        invals = new DBSInvalidations();
        invals.serialize(baout);
        ser = new String(baout.toByteArray());
        assertEquals("", ser);

        invals = new DBSInvalidations();
        invals.add("foo");
        baout.reset();
        invals.serialize(baout);
        ser = new String(baout.toByteArray());
        assertEquals(",foo", ser);

        invals.add("bar");
        baout.reset();
        invals.serialize(baout);
        ser = new String(baout.toByteArray());
        assertTrue(ser, Arrays.asList(",foo,bar", ",bar,foo").contains(ser)); // non-deterministic order

        invals = new DBSInvalidations();
        invals.setAll();
        baout.reset();
        invals.serialize(baout);
        ser = new String(baout.toByteArray());
        assertEquals("A", ser);
    }

    @Test
    public void testDeserialization() throws IOException {
        DBSInvalidations invals;
        ByteArrayInputStream bain;

        bain = new ByteArrayInputStream("".getBytes());
        invals = DBSInvalidations.deserialize(bain);
        assertNull(invals);

        bain = new ByteArrayInputStream("x".getBytes());
        invals = DBSInvalidations.deserialize(bain);
        assertNull(invals);

        bain = new ByteArrayInputStream("A".getBytes());
        invals = DBSInvalidations.deserialize(bain);
        assertTrue(invals.all);

        bain = new ByteArrayInputStream(",foo".getBytes());
        invals = DBSInvalidations.deserialize(bain);
        assertEquals(Collections.singleton("foo"), invals.ids);

        bain = new ByteArrayInputStream(",foo,bar".getBytes());
        invals = DBSInvalidations.deserialize(bain);
        assertEquals(new HashSet<>(Arrays.asList("foo", "bar")), invals.ids);
    }

}
