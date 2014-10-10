/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.storage.binary.AESBinaryManager.PARAM_KEY_ALIAS;
import static org.nuxeo.ecm.core.storage.binary.AESBinaryManager.PARAM_KEY_PASSWORD;
import static org.nuxeo.ecm.core.storage.binary.AESBinaryManager.PARAM_KEY_STORE_FILE;
import static org.nuxeo.ecm.core.storage.binary.AESBinaryManager.PARAM_KEY_STORE_PASSWORD;
import static org.nuxeo.ecm.core.storage.binary.AESBinaryManager.PARAM_KEY_STORE_TYPE;
import static org.nuxeo.ecm.core.storage.binary.AESBinaryManager.PARAM_PASSWORD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAESBinaryManager extends NXRuntimeTestCase {

    private static final String KEY_STORE_TYPE = "JCEKS";

    private static final String KEY_STORE_PASSWORD = "keystoresecret";

    private static final String KEY_ALIAS = "myaeskey";

    private static final String KEY_PASSWORD = "keysecret";

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    private static final String UTF8 = "UTF-8";

    @Test
    public void testEncryptDecryptWithPassword() throws Exception {
        AESBinaryManager binaryManager = new AESBinaryManager();
        binaryManager.digestAlgorithm = binaryManager.getDigest(); // MD5
        String options = String.format("%s=%s", PARAM_PASSWORD, "mypassword");
        binaryManager.initializeOptions(options);

        // encrypt
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String digest = binaryManager.storeAndDigest(new ByteArrayInputStream(
                CONTENT.getBytes(UTF8)), out);
        assertEquals(CONTENT_MD5, digest);
        byte[] encrypted = out.toByteArray();

        // decrypt
        out = new ByteArrayOutputStream();
        binaryManager.decrypt(new ByteArrayInputStream(encrypted), out);

        assertEquals(CONTENT, new String(out.toByteArray(), UTF8));

        // cannot decrypt with wrong password

        options = String.format("%s=%s", PARAM_PASSWORD, "badpassword");
        binaryManager.initializeOptions(options);

        out = new ByteArrayOutputStream();
        binaryManager.decrypt(new ByteArrayInputStream(encrypted), out);

        assertFalse(CONTENT.equals(new String(out.toByteArray(), UTF8)));

        binaryManager.close();
    }

    @Test
    public void testEncryptDecryptWithKeyStore() throws Exception {
        File keyStoreFile = File.createTempFile("nuxeoKeyStore_", "");
        keyStoreFile.delete();
        createKeyStore(keyStoreFile);

        String options = String.format("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s",
                PARAM_KEY_STORE_TYPE, KEY_STORE_TYPE, //
                PARAM_KEY_STORE_FILE, keyStoreFile.getPath(), //
                PARAM_KEY_STORE_PASSWORD, KEY_STORE_PASSWORD, //
                PARAM_KEY_ALIAS, KEY_ALIAS, //
                PARAM_KEY_PASSWORD, KEY_PASSWORD);

        AESBinaryManager binaryManager = new AESBinaryManager();
        binaryManager.digestAlgorithm = binaryManager.getDigest(); // MD5
        binaryManager.initializeOptions(options);

        // encrypt
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String digest = binaryManager.storeAndDigest(new ByteArrayInputStream(
                CONTENT.getBytes(UTF8)), out);
        assertEquals(CONTENT_MD5, digest);
        byte[] encrypted = out.toByteArray();

        // decrypt
        out = new ByteArrayOutputStream();
        binaryManager.decrypt(new ByteArrayInputStream(encrypted), out);

        assertEquals(CONTENT, new String(out.toByteArray(), UTF8));

        binaryManager.close();
    }

    protected void createKeyStore(File file) throws GeneralSecurityException,
            IOException {
        AESBinaryManager.setUnlimitedJCEPolicy();

        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(256);
        Key skey = kgen.generateKey();
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        // keyStore.load(null, KEY_STORE_PASSWORD.toCharArray());
        keyStore.load(null, null);
        keyStore.setKeyEntry(KEY_ALIAS, skey, KEY_PASSWORD.toCharArray(), null);
        OutputStream out = new FileOutputStream(file);
        keyStore.store(out, KEY_STORE_PASSWORD.toCharArray());
        out.close();
    }

    @Test
    public void testAESBinaryManager() throws Exception {
        AESBinaryManager binaryManager = new AESBinaryManager();
        BinaryManagerDescriptor descriptor = new BinaryManagerDescriptor();
        String options = String.format("%s=%s", PARAM_PASSWORD, "mypassword");
        descriptor.key = options;
        binaryManager.initialize(descriptor);

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertNull(binary);

        // store binary
        byte[] bytes = CONTENT.getBytes(UTF8);
        binary = binaryManager.getBinary(new ByteArrayInputStream(bytes));
        assertNotNull(binary);
        assertEquals(CONTENT_MD5, binary.getDigest());

        // get binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        InputStream stream = binary.getStream();
        assertEquals(CONTENT, IOUtils.toString(stream, UTF8));

        binaryManager.close();
    }

}
