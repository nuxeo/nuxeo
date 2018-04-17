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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Key/Value Store common methods.
 *
 * @since 9.3
 */
public abstract class AbstractKeyValueStoreProvider implements KeyValueStoreProvider {

    protected static final ThreadLocal<CharsetDecoder> UTF_8_DECODERS = ThreadLocal.withInitial(
            () -> UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(
                    CodingErrorAction.REPORT));

    /**
     * Converts UTF-8 bytes to a String, or throws if malformed.
     *
     * @throws CharacterCodingException
     */
    protected static String bytesToString(byte[] bytes) throws CharacterCodingException {
        return bytes == null ? null : UTF_8_DECODERS.get().decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * Converts a String to UTF-8 bytes.
     */
    protected static byte[] stringToBytes(String string) {
        return string == null ? null : string.getBytes(UTF_8);
    }

    /**
     * Converts UTF-8 bytes to a Long, or throws if malformed.
     *
     * @throws NumberFormatException
     */
    protected static Long bytesToLong(byte[] bytes) throws NumberFormatException { // NOSONAR
        if (bytes == null) {
            return null;
        }
        if (bytes.length > 20) { // Long.MIN_VALUE has 20 characters including the sign
            throw new NumberFormatException("For input string of length " + bytes.length);
        }
        return Long.valueOf(new String(bytes, UTF_8));
    }

    /**
     * Converts a long to UTF-8 bytes.
     */
    protected static byte[] longToBytes(Long value) {
        return value == null ? null : value.toString().getBytes(UTF_8);
    }

    @Override
    public void put(String key, byte[] value) {
        put(key, value, 0);
    }

    @Override
    public void put(String key, String value) {
        put(key, stringToBytes(value), 0);
    }

    @Override
    public void put(String key, String value, long ttl) {
        put(key, stringToBytes(value), ttl);
    }

    @Override
    public void put(String key, Long value) {
        put(key, longToBytes(value), 0);
    }

    @Override
    public void put(String key, Long value, long ttl) {
        put(key, longToBytes(value), ttl);
    }

    @Override
    public String getString(String key) {
        byte[] bytes = get(key);
        try {
            return bytesToString(bytes);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Value is not a String for key: " + key);
        }
    }

    @Override
    public Long getLong(String key) throws NumberFormatException { // NOSONAR
        byte[] bytes = get(key);
        return bytesToLong(bytes);
    }

    /*
     * This default implementation is uninteresting. It is expected that underlying storage implementations
     * will leverage bulk fetching to deliver significant optimizations over this simple loop.
     */
    @Override
    public Map<String, byte[]> get(Collection<String> keys) {
        Map<String, byte[]> map = new HashMap<>(keys.size());
        for (String key : keys) {
            byte[] value = get(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    /*
     * This default implementation is uninteresting. It is expected that underlying storage implementations
     * will leverage bulk fetching to deliver significant optimizations over this simple loop.
     */
    @Override
    public Map<String, String> getStrings(Collection<String> keys) {
        Map<String, String> map = new HashMap<>(keys.size());
        for (String key : keys) {
            String value = getString(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    /*
     * This default implementation is uninteresting. It is expected that underlying storage implementations
     * will leverage bulk fetching to deliver significant optimizations over this simple loop.
     */
    @Override
    public Map<String, Long> getLongs(Collection<String> keys) throws NumberFormatException { // NOSONAR
        Map<String, Long> map = new HashMap<>(keys.size());
        for (String key : keys) {
            Long value = getLong(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    @Override
    public boolean compareAndSet(String key, byte[] expected, byte[] value) {
        return compareAndSet(key, expected, value, 0);
    }

    @Override
    public boolean compareAndSet(String key, String expected, String value) {
        return compareAndSet(key, expected, value, 0);
    }

    @Override
    public boolean compareAndSet(String key, String expected, String value, long ttl) {
        return compareAndSet(key, stringToBytes(expected), stringToBytes(value), ttl);
    }

    @Override
    public long addAndGet(String key, long delta) throws NumberFormatException { // NOSONAR
        for (;;) {
            String value = getString(key);
            long base = value == null ? 0 : Long.parseLong(value);
            long result = base + delta;
            String newValue = Long.toString(result);
            if (compareAndSet(key, value, newValue)) {
                return result;
            }
        }
    }

}
