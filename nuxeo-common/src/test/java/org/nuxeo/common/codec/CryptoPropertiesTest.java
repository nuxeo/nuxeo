/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
