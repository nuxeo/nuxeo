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
package org.nuxeo.ecm.core.storage.kv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @since 9.1
 */
public abstract class AbstractKeyValueStoreTest {

    protected static final String BAR = "bar";

    protected static final String GEE = "gee";

    protected static final String MOO = "moo";

    protected static final String ZAP = "zap";

    protected static final byte[] BAR_B = BAR.getBytes();

    protected static final byte[] GEE_B = GEE.getBytes();

    protected static final byte[] MOO_B = MOO.getBytes();

    protected abstract KeyValueStore newKeyValueStore();

    @Test
    public void testPutGet() {
        KeyValueStore store = newKeyValueStore();
        String key = "foo";

        assertNull(store.get(key));
        store.put(key, null);
        assertNull(store.get(key));
        store.put(key, BAR_B);
        assertEquals(BAR, new String(store.get(key)));
        store.put(key, GEE_B);
        assertEquals(GEE, new String(store.get(key)));

        // check value is copied on put and get
        byte[] bytes = ZAP.getBytes();
        store.put(key, bytes);
        bytes[0] = 'c';
        byte[] bytes2 = store.get(key);
        assertEquals(ZAP, new String(bytes2));
        bytes2[0] = 'c';
        assertEquals(ZAP, new String(store.get(key)));

        store.put(key, null);
        assertNull(store.get(key));
    }

    @Test
    public void testCompareAndSet() {
        KeyValueStore store = newKeyValueStore();
        String key = "foo";

        assertFalse(store.compareAndSet(key, BAR_B, GEE_B));
        assertNull(store.get(key));
        assertFalse(store.compareAndSet(key, BAR_B, null));
        assertNull(store.get(key));
        assertTrue(store.compareAndSet(key, null, null));
        assertNull(store.get(key));

        assertTrue(store.compareAndSet(key, null, BAR_B));
        assertEquals(BAR, new String(store.get(key)));
        assertFalse(store.compareAndSet(key, GEE_B, MOO_B));
        assertEquals(BAR, new String(store.get(key)));
        assertFalse(store.compareAndSet(key, null, GEE_B));
        assertEquals(BAR, new String(store.get(key)));
        assertFalse(store.compareAndSet(key, null, null));
        assertEquals(BAR, new String(store.get(key)));

        assertTrue(store.compareAndSet(key, BAR_B, GEE_B));
        assertEquals(GEE, new String(store.get(key)));

        // check value is copied
        byte[] bytes = ZAP.getBytes();
        assertTrue(store.compareAndSet(key, GEE_B, bytes));
        bytes[0] = 'c';
        assertEquals(ZAP, new String(store.get(key)));

        assertTrue(store.compareAndSet(key, ZAP.getBytes(), null));
        assertNull(store.get(key));

        // check value is copied
        bytes = ZAP.getBytes();
        assertTrue(store.compareAndSet(key, null, bytes));
        bytes[0] = 'c';
        assertEquals(ZAP, new String(store.get(key)));
    }

    @Test
    public void testBinaryValuesAreAccepted() {
        KeyValueStore store = newKeyValueStore();
        String key = "foo";

        byte[] value = new byte[256];
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte) i;
        }
        store.put(key, value);
        assertArrayEquals(value, store.get(key));
    }

    @Test
    public void testClear() {
        KeyValueStore store = newKeyValueStore();
        String key = "foo";
        store.put(key, BAR_B);
        assertEquals(BAR, new String(store.get(key)));
        ((KeyValueStoreProvider) store).clear();
        assertNull(store.get(key));
    }

}
