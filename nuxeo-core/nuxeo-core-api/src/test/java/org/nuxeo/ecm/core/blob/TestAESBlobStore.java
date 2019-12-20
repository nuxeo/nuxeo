/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.nuxeo.ecm.core.blob.AESBlobStoreConfiguration.PROP_KEY_ALIAS;
import static org.nuxeo.ecm.core.blob.AESBlobStoreConfiguration.PROP_KEY_PASSWORD;
import static org.nuxeo.ecm.core.blob.AESBlobStoreConfiguration.PROP_KEY_STORE_FILE;
import static org.nuxeo.ecm.core.blob.AESBlobStoreConfiguration.PROP_KEY_STORE_PASSWORD;
import static org.nuxeo.ecm.core.blob.AESBlobStoreConfiguration.PROP_KEY_STORE_TYPE;
import static org.nuxeo.ecm.core.blob.AESBlobStoreConfiguration.PROP_PASSWORD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.KeyGenerator;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.AESBlobStore.DecryptingInputStream;
import org.nuxeo.ecm.core.blob.AESBlobStore.EncryptingOutputStream;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-blob-provider-aes-digest.xml")
public class TestAESBlobStore extends TestLocalBlobStoreAbstract {

    // also in XML
    protected static final String KEY_STORE_TYPE = "JCEKS";

    // also in XML
    protected static final String KEY_STORE_PASSWORD = "keystoresecret";

    // also in XML
    protected static final String KEY_ALIAS = "myaeskey";

    // also in XML
    protected static final String KEY_PASSWORD = "keysecret";

    // property used in XML
    protected static final String PATH_PROP = "test.keystore.file";

    // password for PBKDF2
    protected static final String PASSWORD = "secretpassword";

    protected Path keyStoreFile;

    @Override
    public boolean checkSizeOfGCedFiles() {
        return false;
    }

    @Test
    public void testFlags() {
        assertFalse(bp.isTransactional());
        assertFalse(bp.isRecordMode());
        assertTrue(bs.getKeyStrategy().useDeDuplication());
    }

    @Override
    protected void testCopyOrMove(boolean atomicMove) throws IOException {
        // we don't test the unimplemented copyBlob API, as it's only called from commit or during caching
        assumeFalse("low-level copy/move not tested in aes blob store", true);
    }

    @Before
    public void initKeyStore() throws IOException, GeneralSecurityException {
        keyStoreFile = Framework.createTempFilePath("nuxeoKeyStore_", "");
        Files.delete(keyStoreFile);
        createKeyStore(keyStoreFile);
        Properties properties = Framework.getProperties();
        properties.put(PATH_PROP, keyStoreFile.toString());
    }

    @After
    public void clearKeyStore() throws IOException {
        Files.delete(keyStoreFile);
    }

    protected void createKeyStore(Path file) throws IOException, GeneralSecurityException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(256);
        Key skey = kgen.generateKey();
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        // keyStore.load(null, KEY_STORE_PASSWORD.toCharArray())
        keyStore.load(null, null);
        keyStore.setKeyEntry(KEY_ALIAS, skey, KEY_PASSWORD.toCharArray(), null);
        try (OutputStream out = Files.newOutputStream(file)) {
            keyStore.store(out, KEY_STORE_PASSWORD.toCharArray());
        }
    }

    @Test
    public void testEncryptDecryptWithKeystore() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROP_KEY_STORE_TYPE, KEY_STORE_TYPE);
        properties.put(PROP_KEY_STORE_FILE, keyStoreFile.toString());
        properties.put(PROP_KEY_STORE_PASSWORD, KEY_STORE_PASSWORD);
        properties.put(PROP_KEY_ALIAS, KEY_ALIAS);
        properties.put(PROP_KEY_PASSWORD, KEY_PASSWORD);
        doTestEncryptDecrypt(properties);
    }

    @Test
    public void testEncryptDecryptWithPassword() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROP_PASSWORD, PASSWORD);
        doTestEncryptDecrypt(properties);
    }

    protected void doTestEncryptDecrypt(Map<String, String> properties) throws IOException {
        AESBlobStoreConfiguration aesConfig = new AESBlobStoreConfiguration(properties);
        String string = "hello world";
        // encrypt
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream enc = new EncryptingOutputStream(baos, aesConfig)) {
            IOUtils.write(string, enc, UTF_8);
        }
        // decrypt
        ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray());
        try (InputStream dec = new DecryptingInputStream(bain, aesConfig)) {
            String result = IOUtils.toString(dec, UTF_8);
            assertEquals(string, result);
        }
        // decrypt corrupted data
        byte[] bytes = baos.toByteArray();
        bytes[bytes.length - 5] += 123;
        bain = new ByteArrayInputStream(bytes);
        try (InputStream dec = new DecryptingInputStream(bain, aesConfig)) {
            String result = IOUtils.toString(dec, UTF_8);
            fail("Should fail to decrypt, but read: " + result);
        } catch (IOException e) {
            String message = e.getMessage();
            assertTrue(message,
                    message.contains("Given final block not properly padded") || message.contains("Tag mismatch"));
        }
        if (aesConfig.usePBKDF2) {
            // decrypt with bad password
            properties.put(PROP_PASSWORD, "badpassword");
            aesConfig = new AESBlobStoreConfiguration(properties);
            bain = new ByteArrayInputStream(baos.toByteArray());
            try (InputStream dec = new DecryptingInputStream(bain, aesConfig)) {
                String result = IOUtils.toString(dec, UTF_8);
                fail("Should fail to decrypt, but read: " + result);
            } catch (IOException e) {
                String message = e.getMessage();
                assertTrue(message, message.contains("Tag mismatch"));
            }
        } else {
            // decrypt with bad key password
            properties.put(PROP_KEY_PASSWORD, "badpassword");
            aesConfig = new AESBlobStoreConfiguration(properties);
            bain = new ByteArrayInputStream(baos.toByteArray());
            try (InputStream dec = new DecryptingInputStream(bain, aesConfig)) {
                String result = IOUtils.toString(dec, UTF_8);
                fail("Should fail to decrypt, but read: " + result);
            } catch (IOException e) {
                String message = e.getMessage();
                assertTrue(message, message.contains("Given final block not properly padded"));
            }
        }
    }

}
