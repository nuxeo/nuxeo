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
package org.nuxeo.runtime.kv;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
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

    protected KeyValueStoreProvider store;

    @Before
    public void setUp() {
        store = newKeyValueStore();
    }

    protected abstract KeyValueStoreProvider newKeyValueStore();

    protected boolean hasSlowTTLExpiration() {
        return false;
    }

    protected void sleepForTTLExpiration() {
    }

    protected Set<String> storeKeys() {
        return store.keyStream().collect(Collectors.toSet());
    }

    @Test
    public void testPutGet() {
        String key = "foo";

        assertEquals(Collections.emptySet(), storeKeys());
        assertNull(store.get(key));
        assertNull(store.getString(key));
        store.put(key, (byte[]) null);
        assertNull(store.get(key));
        assertEquals(Collections.emptySet(), storeKeys());

        store.put(key, BAR_B);
        assertEquals(BAR, new String(store.get(key)));
        assertEquals(BAR, store.getString(key));
        assertEquals(Collections.singleton(key), storeKeys());
        store.put(key, GEE_B);
        assertEquals(GEE, new String(store.get(key)));
        assertEquals(GEE, store.getString(key));
        assertEquals(Collections.singleton(key), storeKeys());
        store.put(key, MOO);
        assertEquals(MOO, new String(store.get(key)));
        assertEquals(MOO, store.getString(key));
        assertEquals(Collections.singleton(key), storeKeys());

        // check value is copied on put and get
        byte[] bytes = ZAP.getBytes();
        store.put(key, bytes);
        bytes[0] = 'c';
        byte[] bytes2 = store.get(key);
        assertEquals(ZAP, new String(bytes2));
        bytes2[0] = 'c';
        assertEquals(ZAP, new String(store.get(key)));

        store.put(key, (String) null);
        assertNull(store.get(key));
        assertEquals(Collections.emptySet(), storeKeys());
    }

    @Test
    public void testGetMany() {
        String key1 = "foo1";
        String key2 = "foo2";
        String key3 = "foo3";
        String key4 = "foo4";
        String key5 = "foo5";
        Set<String> keys = new HashSet<>(Arrays.asList(key1, key2, key3, key4, key5));

        assertTrue(store.get(keys).isEmpty());

        Map<String, String> map = new HashMap<>();
        map.put(key1, BAR);
        map.put(key2, GEE);
        map.put(key3, MOO);
        store.put(key1, BAR);
        store.put(key2, GEE);
        store.put(key3, MOO);
        store.put(key4, (String) null);
        assertEquals(map, store.getStrings(keys));

        store.put(key1, BAR_B);
        store.put(key2, GEE_B);
        store.put(key3, MOO_B);
        Map<String, byte[]> storeBMap = store.get(keys);
        assertArrayEquals(BAR_B, storeBMap.get(key1));
        assertArrayEquals(GEE_B, storeBMap.get(key2));
        assertArrayEquals(MOO_B, storeBMap.get(key3));
        assertEquals(3, storeBMap.entrySet().size());
    }

    @Test
    public void testCompareAndSet() {
        String key = "foo";

        assertFalse(store.compareAndSet(key, BAR_B, GEE_B));
        assertNull(store.get(key));
        assertFalse(store.compareAndSet(key, BAR_B, null));
        assertNull(store.get(key));
        assertTrue(store.compareAndSet(key, (byte[]) null, (byte[]) null));
        assertNull(store.get(key));

        assertTrue(store.compareAndSet(key, null, BAR_B));
        assertEquals(BAR, new String(store.get(key)));
        assertFalse(store.compareAndSet(key, GEE_B, MOO_B));
        assertEquals(BAR, new String(store.get(key)));
        assertFalse(store.compareAndSet(key, null, GEE));
        assertEquals(BAR, new String(store.get(key)));
        assertFalse(store.compareAndSet(key, (String) null, (String) null));
        assertEquals(BAR, new String(store.get(key)));

        assertTrue(store.compareAndSet(key, BAR, GEE));
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
        String key = "foo";

        byte[] value = new byte[256];
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte) i;
        }

        // make sure the bytes are not valid UTF-8
        try {
            CharsetDecoder cd = UTF_8.newDecoder()
                                     .onMalformedInput(CodingErrorAction.REPORT) //
                                     .onUnmappableCharacter(CodingErrorAction.REPORT);
            cd.decode(ByteBuffer.wrap(value));
            fail("bytes should be invalid UTF-8");
        } catch (CharacterCodingException e) {
            // ok
        }

        store.put(key, value);
        assertArrayEquals(value, store.get(key));
        try {
            store.getString(key);
            fail("Shoudl fail to get value as a String");
        } catch (IllegalArgumentException e) {
            assertEquals("Value is not a String for key: foo", e.getMessage());
        }
    }

    @Test
    public void testClear() {
        String key = "foo";
        store.put(key, BAR_B);
        assertEquals(BAR, new String(store.get(key)));
        assertEquals(Collections.singleton(key), storeKeys());

        ((KeyValueStoreProvider) store).clear();
        assertNull(store.get(key));
        assertEquals(Collections.emptySet(), storeKeys());
    }

    @Test
    public void testTTL() throws Exception {
        assumeFalse("Ignored because of slow TTL expiration", hasSlowTTLExpiration());
        int longTTL = 30; // 30s
        int shortTTL = 3; // 3s

        String key = "foo";
        assertFalse(store.setTTL(key, 0)); // no such key
        assertFalse(store.setTTL(key, longTTL)); // no such key
        store.put(key, BAR_B, longTTL);
        assertEquals(BAR, new String(store.get(key)));

        assertTrue(store.setTTL(key, shortTTL)); // set shorter TTL
        Thread.sleep((shortTTL + 2) * 1000); // sleep a bit more in case expiration is late
        sleepForTTLExpiration();
        assertNull(store.get(key));

        store.put(key, BAR, shortTTL);
        store.setTTL(key, 0); // unset TTL
        Thread.sleep((shortTTL + 2) * 1000); // sleep a bit more in case expiration is late
        sleepForTTLExpiration();
        assertEquals(BAR, new String(store.get(key)));

        // compareAndSet with TTL

        assertTrue(store.compareAndSet(key, BAR, GEE, shortTTL));
        assertEquals(GEE, store.getString(key));
        Thread.sleep((shortTTL + 2) * 1000); // sleep a bit more in case expiration is late
        sleepForTTLExpiration();
        assertNull(store.get(key));

        assertTrue(store.compareAndSet(key, null, MOO, shortTTL));
        assertEquals(MOO, store.getString(key));
        Thread.sleep((shortTTL + 2) * 1000); // sleep a bit more in case expiration is late
        sleepForTTLExpiration();
        assertNull(store.get(key));

    }

}
