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
import static org.junit.Assert.assertNotNull;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, TransactionalFeature.class })
@Deploy("org.nuxeo.runtime.kv")
public abstract class AbstractKeyValueStoreTest {

    protected static final String BAR = "bar";

    protected static final String GEE = "gee";

    protected static final String MOO = "moo";

    protected static final String ZAP = "zap";

    protected static final byte[] BAR_B = BAR.getBytes();

    protected static final byte[] GEE_B = GEE.getBytes();

    protected static final byte[] MOO_B = MOO.getBytes();

    protected static final byte[] NOT_UTF_8 = new byte[] { (byte) 128, (byte) 0 };

    @Inject
    protected KeyValueService keyValueService;

    protected KeyValueStoreProvider store;

    @Before
    public void setUp() {
        store = (KeyValueStoreProvider) keyValueService.getKeyValueStore("default");
        store.clear();
    }

    protected boolean hasSlowTTLExpiration() {
        return false;
    }

    protected void sleepForTTLExpiration() {
    }

    protected Set<String> storeKeys() {
        return store.keyStream().collect(Collectors.toSet());
    }

    @Test
    public void testCopyDoesNotShareData() {
        KeyValueStore otherStore = keyValueService.getKeyValueStore("notregistered");
        String key = "mykey";
        store.put(key, "foo");
        assertNotNull(store.getString(key));
        assertNull(otherStore.getString(key)); // other key/value store does not see the same data
    }

    @Test
    public void testPutGet() {
        String key = "foo";

        assertEquals(Collections.emptySet(), storeKeys());
        assertNull(store.get(key));
        assertNull(store.getString(key));
        assertNull(store.getLong(key));
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

        try {
            store.getLong(key);
            fail("shouldn't allow to get long");
        } catch (NumberFormatException e) {
            // ok
        }

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

        store.put(key, Long.valueOf(-123));
        assertEquals(Long.valueOf(-123), store.getLong(key));
    }

    /**
     * Checks that things are done outside the main transaction.
     */
    @Test
    public void testNotTransactional() {
        String key = "foo";
        String value = "bar";
        // in a transaction
        TransactionHelper.runInTransaction(() -> {
            // set a value
            store.put(key, value);
            assertEquals(value, store.getString(key));
            // then rollback
            TransactionHelper.setTransactionRollbackOnly();
        });
        // check that after rollback value is still set
        assertEquals(value, store.getString(key));
        // including if we start a new transaction
        TransactionHelper.runInTransaction(() -> assertEquals(value, store.getString(key)));
    }

    @Test
    public void testReadFromBytes() {
        String key = "foo";

        // null
        store.put(key, (byte[]) null);
        checkReadNull(key);

        // long
        store.put(key, "123456789".getBytes(UTF_8));
        checkReadLong(key, 123456789);

        // UTF-8 string
        store.put(key, "ABC".getBytes(UTF_8));
        checkReadString(key, "ABC");

        // bytes not UTF-8
        store.put(key, NOT_UTF_8);
        checkReadBytes(key, NOT_UTF_8);
    }

    @Test
    public void testReadFromString() {
        String key = "foo";

        // null
        store.put(key, (String) null);
        checkReadNull(key);

        // long
        store.put(key, "123456789");
        checkReadLong(key, 123456789);

        // UTF-8 string
        store.put(key, "ABC");
        checkReadString(key, "ABC");
    }

    @Test
    public void testReadFromLong() {
        String key = "foo";

        // null
        store.put(key, (Long) null);
        checkReadNull(key);

        // long
        store.put(key, Long.valueOf(123456789));
        checkReadLong(key, 123456789);
    }

    protected void checkReadNull(String key) {
        assertNull(store.get(key));
        assertNull(store.getString(key));
        assertNull(store.getLong(key));

        Set<String> keys = Collections.singleton(key);
        assertTrue(store.get(keys).isEmpty());
        assertTrue(store.getStrings(keys).isEmpty());
        assertTrue(store.getLongs(keys).isEmpty());
    }

    protected void checkReadLong(String key, long value) {
        Long longLong = Long.valueOf(value);
        String longString = longLong.toString();

        assertArrayEquals(longString.getBytes(UTF_8), store.get(key));
        assertEquals(longString, store.getString(key));
        assertEquals(longLong, store.getLong(key));

        Set<String> keys = Collections.singleton(key);
        Map<String, byte[]> bytesMap = store.get(keys);
        assertEquals(1, bytesMap.size());
        assertEquals(key, bytesMap.keySet().iterator().next());
        assertArrayEquals(longString.getBytes(UTF_8), bytesMap.values().iterator().next());
        assertEquals(Collections.singletonMap(key, longString), store.getStrings(keys));
        assertEquals(Collections.singletonMap(key, longLong), store.getLongs(keys));
    }

    protected void checkReadString(String key, String string) {
        assertArrayEquals(string.getBytes(UTF_8), store.get(key));
        assertEquals(string, store.getString(key));
        try {
            store.getLong(key);
            fail("should fail reading a non-numeric value");
        } catch (NumberFormatException e) {
            // ok
        }

        Set<String> keys = Collections.singleton(key);
        Map<String, byte[]> bytesMap = store.get(keys);
        assertEquals(1, bytesMap.size());
        assertEquals(key, bytesMap.keySet().iterator().next());
        assertArrayEquals(string.getBytes(UTF_8), bytesMap.values().iterator().next());
        assertEquals(Collections.singletonMap(key, string), store.getStrings(keys));
        try {
            store.getLongs(keys);
            fail("should fail reading a non-numeric value");
        } catch (NumberFormatException e) {
            // ok
        }
    }

    // bytes that are not utf-8
    protected void checkReadBytes(String key, byte[] bytes) {
        assertArrayEquals(bytes, store.get(key));
        try {
            store.getString(key);
            fail("should fail reading a non-UTF-8 value");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            store.getLong(key);
            fail("should fail reading a non-numeric value");
        } catch (NumberFormatException e) {
            // ok
        }

        Set<String> keys = Collections.singleton(key);
        Map<String, byte[]> bytesMap = store.get(keys);
        assertEquals(1, bytesMap.size());
        assertEquals(key, bytesMap.keySet().iterator().next());
        assertArrayEquals(bytes, bytesMap.values().iterator().next());
        try {
            store.getStrings(keys);
            fail("should fail reading a non-UTF-8 value");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            store.getLongs(keys);
            fail("should fail reading a non-numeric value");
        } catch (NumberFormatException e) {
            // ok
        }
    }

    @Test
    public void testGetMany() {
        assertTrue(store.get(Collections.emptyList()).isEmpty());
        assertTrue(store.getStrings(Collections.emptyList()).isEmpty());

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

    @SuppressWarnings("boxing")
    @Test
    public void testGetManyLong() {
        assertTrue(store.getLongs(Collections.emptyList()).isEmpty());

        String key1 = "foo1";
        String key2 = "foo2";
        String key3 = "foo3";
        String key4 = "foo4";
        String key5 = "foo5";
        Set<String> keys = new HashSet<>(Arrays.asList(key1, key2, key3, key4, key5));

        assertTrue(store.get(keys).isEmpty());

        Map<String, Long> map = new HashMap<>();
        map.put(key1, 1L);
        map.put(key2, 2L);
        map.put(key3, 3L);
        store.put(key1, 1L);
        store.put(key2, 2L);
        store.put(key3, 3L);
        store.put(key4, (Long) null);
        assertEquals(map, store.getLongs(keys));

        // store a value that cannot be interpreted as a Long
        store.put(key1, "notalong");
        try {
            store.getLongs(keys);
            fail("shouldn't allow to get longs");
        } catch (NumberFormatException e) {
            // ok
        }
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

        // non-canonical form for Long in some storages
        store.put(key, "0");
        assertTrue(store.compareAndSet(key, "0", "1"));
        assertTrue(store.compareAndSet(key, "1".getBytes(), "2".getBytes()));
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

    @Test
    public void testAddAndGet() throws Exception {
        String key = "foo";

        assertNull(store.get(key));
        assertEquals(123, store.addAndGet(key, 123));
        assertEquals(579, store.addAndGet(key, 456));
        assertEquals(-421, store.addAndGet(key, -1000));
        assertEquals(0, store.addAndGet(key, 421));

        // numeric string works too
        store.put(key, "123");
        assertEquals(456, store.addAndGet(key, 333));
        assertEquals("456", store.getString(key));
        assertEquals(Long.valueOf(456), store.getLong(key));

        // invalid empty string
        store.put(key, "");
        try {
            store.addAndGet(key, 1);
            // TODO uncomment when Redis is upgraded to a more recent version than 2.8, which allowed this
            // fail("shouldn't allow incrementing an empty string");
        } catch (NumberFormatException e) {
            // ok
        }

        // invalid non-numeric string
        store.put(key, "ABC");
        try {
            store.addAndGet(key, 1);
            fail("shouldn't allow incrementing a non-numeric string");
        } catch (NumberFormatException e) {
            // ok
        }
    }

    @Test
    public void testKeyStream() throws Exception {
        // keyStream() already tested by all other test methods indirectly

        store.put("foo", "test");
        store.put("foox", "test");
        store.put("foo?", "test"); // ? should not be matched by Redis or MongoDB as a wildcard
        store.put("foo?a", "test");
        store.put("foo*", "test"); // * should not be matched by Redis or MongoDB as a wildcard
        store.put("foo*b", "test");

        store.put("bar", "test");
        store.put("barx", "test");
        store.put("bar.", "test"); // . should not be matched by MongoDB as a wildcard
        store.put("bar.1", (String) null);
        store.put("bar.2", (byte[]) null);
        store.put("bar.3", (Long) null);
        store.put("bar.4", "test");
        store.put("bar.5", "test".getBytes(UTF_8));
        store.put("bar.6", Long.valueOf(123));

        store.put("baz", "test");
        store.put("bazx", "test");
        store.put("baz%", "test"); // % should not be matched by SQL as a wildcard
        store.put("baz%7", "test");

        // ? should not be matched by Redis or MongoDB as a wildcard
        String prefix = "foo?";
        List<String> expected = Arrays.asList("foo?", "foo?a");
        assertEquals(new HashSet<>(expected), store.keyStream(prefix).collect(Collectors.toSet()));

        // * should not be matched by Redis or MongoDB as a wildcard
        prefix = "foo*";
        expected = Arrays.asList("foo*", "foo*b");
        assertEquals(new HashSet<>(expected), store.keyStream(prefix).collect(Collectors.toSet()));

        // . should not be matched by MongoDB as a wildcard
        prefix = "bar.";
        expected = Arrays.asList("bar.", "bar.4", "bar.5", "bar.6");
        assertEquals(new HashSet<>(expected), store.keyStream(prefix).collect(Collectors.toSet()));

        // % should not be matched by SQL as a wildcard
        prefix = "baz%";
        expected = Arrays.asList("baz%", "baz%7");
        assertEquals(new HashSet<>(expected), store.keyStream(prefix).collect(Collectors.toSet()));
    }

}
