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
package org.nuxeo.ecm.core.kv;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

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
    public String getString(String key) {
        byte[] bytes = get(key);
        try {
            return bytesToString(bytes);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Value is not a String for key: " + key);
        }
    }

    @Override
    public boolean compareAndSet(String key, String expected, String value) {
        return compareAndSet(key, stringToBytes(expected), stringToBytes(value));
    }

}
