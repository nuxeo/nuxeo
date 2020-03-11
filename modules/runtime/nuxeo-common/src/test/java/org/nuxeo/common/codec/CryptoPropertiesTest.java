/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     jcarsique
 */
package org.nuxeo.common.codec;

import static org.junit.Assert.*;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import org.nuxeo.common.Environment;

/**
 * @since 7.4
 */
public class CryptoPropertiesTest {

    CryptoProperties properties = new CryptoProperties();

    final String key1 = "some.key1";

    final String key2 = "some.key2";

    final String aValue = "some passphrase";

    final String aCryptedValue = "{$$xoWODkBtRXnSuqIBdosUFA==}";

    final String aCustomCryptedValue = "{$REVTL0VDQi9QS0NTNVBhZGRpbmc=$w/wCDLXoG5VFkNaEpMt0wg==}";

    @Before
    public void setUp() throws Exception {
        properties.setProperty(Environment.CRYPT_KEY, Base64.encodeBase64String("secret".getBytes()));
    }

    @Test
    public void testClearValues() {
        assertNull(properties.setProperty(key1, aValue));
        assertEquals(aValue, properties.getProperty(key1));
        assertEquals(aValue, properties.getRawProperty(key1));
    }

    @Test
    public void testEncryptedValues() {
        assertNull(properties.setProperty(key1, aCryptedValue));
        assertEquals(aValue, properties.getProperty(key1));
        assertEquals(aCryptedValue, properties.getRawProperty(key1));

        assertEquals(aValue, properties.setProperty(key1, aCustomCryptedValue));
        assertEquals(aValue, properties.getProperty(key1));
        assertNotEquals(aCryptedValue, properties.getRawProperty(key1));
        assertEquals(aCustomCryptedValue, properties.getRawProperty(key1));
    }

    @Test
    public void testChangedCrypto() {
        assertNull(properties.setProperty(key1, aCryptedValue));
        assertEquals(aCryptedValue, properties.getRawProperty(key1));
        assertEquals(aValue, new String(properties.getCrypto().decrypt(aCryptedValue)));

        assertEquals(Base64.encodeBase64String("secret".getBytes()),
                properties.setProperty(Environment.CRYPT_KEY, Base64.encodeBase64String("anothersecret".getBytes())));
        assertEquals(aValue, properties.getProperty(key1));
        assertEquals(aCryptedValue, properties.getRawProperty(key1));
        assertEquals("Crypto should have changed", aCryptedValue,
                new String(properties.getCrypto().decrypt(aCryptedValue)));
    }

}
