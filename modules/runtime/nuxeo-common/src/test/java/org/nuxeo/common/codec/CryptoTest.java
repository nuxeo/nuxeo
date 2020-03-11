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
 *     Julien Carsique
 *
 */
package org.nuxeo.common.codec;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @since 7.4
 */
public class CryptoTest {

    protected final byte[] secretKey = "secret".getBytes();

    protected Crypto crypto;

    final String strToEncrypt = "some passphrase \n ${\\my.strange/value}";

    @Before
    public void setUp() throws Exception {
        crypto = new Crypto(secretKey);
    }

    @After
    public void tearDown() {
        crypto.clear();
    }

    @Test
    public void testConversions() throws Exception {
        char[] chars = Crypto.getChars(strToEncrypt.getBytes());
        assertArrayEquals(strToEncrypt.toCharArray(), chars);
        byte[] bytes = Crypto.getBytes(chars);
        assertArrayEquals(strToEncrypt.getBytes(), bytes);
        assertEquals(strToEncrypt, new String(bytes));
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        String encryptedString = crypto.encrypt(strToEncrypt.getBytes());
        assertEquals("Encryption failed", "{$$4pqzMagRsvTI61VjRyGd10JPEAmNXVjIvL13eosHVNnGKraCh0+trE/LdQEz3B2u}",
                encryptedString);
        String decryptedString = new String(Crypto.getChars(crypto.decrypt(encryptedString)));
        assertEquals("Decryption failed", decryptedString, strToEncrypt);
    }

    @Test
    public void testCustomAlgo() throws Exception {
        String encryptedString = crypto.encrypt(Crypto.DES_ECB_PKCS5PADDING, strToEncrypt.getBytes());
        assertEquals("Encryption failed",
                "{$REVTL0VDQi9QS0NTNVBhZGRpbmc=$w/wCDLXoG5XaXjHkADRcwOs4n1wsvuvUMOwrJcpvtiiyWpBsIxtj2g==}",
                encryptedString);
        String decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption failed", decryptedString, strToEncrypt);
    }

    @Test(expected = GeneralSecurityException.class)
    public void testUnsupportedAlgoOnEncrypt() throws Exception {
        crypto.encrypt("AEZ/ECB/PKCS5Padding", "something".getBytes());
    }

    @Test
    public void testUnsupportedAlgoOnDecrypt() throws Exception {
        String encryptedString = "{$xxx$xoWODkBtRXnSuqIBdosUFA==}";
        String decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption must fail silently", decryptedString, encryptedString);

        encryptedString = "{$REVTL0VDQS0NTNsdVBhZGRpbmc=$w/wCDLXoG5VFkNaEpMt0wg==}";
        decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption must fail silently", decryptedString, encryptedString);
    }

    @Test
    public void testDecryptionFailSilently() throws Exception {
        assertFalse(Crypto.isEncrypted(null));
        assertFalse(Crypto.isEncrypted(""));
        assertFalse(Crypto.isEncrypted("{$$}"));

        // bad padding
        String encryptedString = "{$$xoWODkBtRXnsdzIBdosUFA==}";
        String decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption must fail silently", decryptedString, encryptedString);

        // last block FB== is not valid (B is not a possible value)
        encryptedString = "{$$xoWODkCtRXnSuqIBdosUFB==}";
        decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption must fail silently", decryptedString, encryptedString);

        // illegal block size
        encryptedString = "{$REVTL0VDQi9QS0NTNVBhZGRpbmc=$w/wCDLXoGsasskNaEpMt0wg==}";
        decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption must fail silently", decryptedString, encryptedString);

        // bad padding
        encryptedString = "{$REVTL0VDQi9QS0NTNVBhZGRpbmc=$w/wCDLXoG5VFkNbEpMt0wg==}";
        decryptedString = new String(crypto.decrypt(encryptedString));
        assertEquals("Decryption must fail silently", decryptedString, encryptedString);
    }

    @Test(expected = RuntimeException.class)
    public void testClear() throws Exception {
        crypto.clear();
        crypto.encrypt("something".getBytes());
    }

}
