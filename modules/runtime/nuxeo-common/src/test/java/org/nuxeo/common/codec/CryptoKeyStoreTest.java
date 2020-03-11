/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.commons.collections.CollectionUtils;

/**
 * @since 7.4
 */
public class CryptoKeyStoreTest extends CryptoTest {
    protected final char[] keystorePass = "changeit".toCharArray();

    protected final char[] keyPass = "serverKey".toCharArray();

    protected final String keyAlias = "NuxeoSecretKey";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Map<String, SecretKey> secretKeys = new HashMap<>();
        secretKeys.put(Crypto.AES, crypto.getSecretKey(Crypto.AES, secretKey));
        secretKeys.put(Crypto.DES, crypto.getSecretKey(Crypto.DES, secretKey));
        crypto.clear();

        // Generate a keystore where to store the key
        File keystoreFile = File.createTempFile("keystore", ".jceks", new File(System.getProperty("java.io.tmpdir")));
        keystoreFile.delete(); // ensure new keystore creation
        for (SecretKey key : secretKeys.values()) {
            Crypto.setKeyInKeyStore(keystoreFile.getPath(), keystorePass, keyAlias + key.getAlgorithm(), keyPass, key);
        }
        assertTrue(CollectionUtils.isEqualCollection(secretKeys.values(),
                Crypto.getKeysFromKeyStore(keystoreFile.getPath(), keystorePass, keyAlias, keyPass).values()));
        crypto = new Crypto(keystoreFile.getPath(), keystorePass, keyAlias, keyPass);
    }

}
