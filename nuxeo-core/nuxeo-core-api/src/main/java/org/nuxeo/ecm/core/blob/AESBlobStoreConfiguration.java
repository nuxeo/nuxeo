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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * Configuration for the AES-encrypted storage of files.
 *
 * @since 11.1
 */
public class AESBlobStoreConfiguration extends PropertyBasedConfiguration {

    private static final Log log = LogFactory.getLog(AESBlobStoreConfiguration.class);

    protected static final String AES = "AES";

    protected static final String PBKDF2_WITH_HMAC_SHA1 = "PBKDF2WithHmacSHA1";

    protected static final int PBKDF2_ITERATIONS = 10000;

    // AES-256
    protected static final int PBKDF2_KEY_LENGTH = 256;

    // insecure, see https://find-sec-bugs.github.io/bugs.htm#PADDING_ORACLE
    protected static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";

    protected static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    public static final String PROP_COMPAT_KEY = "key";

    public static final String PROP_PASSWORD = "password";

    public static final String PROP_KEY_STORE_TYPE = "keyStoreType";

    public static final String PROP_KEY_STORE_FILE = "keyStoreFile";

    public static final String PROP_KEY_STORE_PASSWORD = "keyStorePassword";

    public static final String PROP_KEY_ALIAS = "keyAlias";

    public static final String PROP_KEY_PASSWORD = "keyPassword";

    /**
     * If {@code true}, use the insecure AES/CBC/PKCS5Padding for encryption. The default is {@code false}, to use
     * AES/GCM/NoPadding.
     */
    public static final String PROP_KEY_USE_INSECURE_CIPHER = "useInsecureCipher";

    public final boolean usePBKDF2;

    public final String password;

    public final String keyStoreType;

    public final String keyStoreFile;

    public final String keyStorePassword;

    public final String keyAlias;

    public final String keyPassword;

    public final boolean useInsecureCipher;

    public AESBlobStoreConfiguration(Map<String, String> properties) throws IOException {
        super(null, properties);
        parseCompat();
        password = getProperty(PROP_PASSWORD);
        keyStoreType = getProperty(PROP_KEY_STORE_TYPE);
        keyStoreFile = getProperty(PROP_KEY_STORE_FILE);
        keyStorePassword = getProperty(PROP_KEY_STORE_PASSWORD);
        keyAlias = getProperty(PROP_KEY_ALIAS);
        String keyPassword = getProperty(PROP_KEY_PASSWORD); // NOSONAR
        useInsecureCipher = Boolean.parseBoolean(getProperty(PROP_KEY_USE_INSECURE_CIPHER));

        usePBKDF2 = password != null;
        if (usePBKDF2) {
            if (keyStoreType != null) {
                throw new NuxeoException("Cannot use " + PROP_KEY_STORE_TYPE + " with " + PROP_PASSWORD);
            }
            if (keyStoreFile != null) {
                throw new NuxeoException("Cannot use " + PROP_KEY_STORE_FILE + " with " + PROP_PASSWORD);
            }
            if (keyStorePassword != null) {
                throw new NuxeoException("Cannot use " + PROP_KEY_STORE_PASSWORD + " with " + PROP_PASSWORD);
            }
            if (keyAlias != null) {
                throw new NuxeoException("Cannot use " + PROP_KEY_ALIAS + " with " + PROP_PASSWORD);
            }
            if (keyPassword != null) {
                throw new NuxeoException("Cannot use " + PROP_KEY_PASSWORD + " with " + PROP_PASSWORD);
            }
        } else {
            if (keyStoreType == null) {
                throw new NuxeoException("Missing " + PROP_KEY_STORE_TYPE);
            }
            // keystore file is optional
            if (keyStoreFile == null && keyStorePassword != null) {
                throw new NuxeoException("Missing " + PROP_KEY_STORE_PASSWORD);
            }
            if (keyAlias == null) {
                throw new NuxeoException("Missing " + PROP_KEY_ALIAS);
            }
            if (keyPassword == null) {
                keyPassword = keyStorePassword;
            }
        }
        this.keyPassword = keyPassword;
    }

    protected void parseCompat() {
        String compatKey = getProperty(PROP_COMPAT_KEY);
        if (isBlank(compatKey)) {
            return;
        }
        for (String option : compatKey.split(",")) {
            String[] split = option.split("=", 2);
            if (split.length != 2) {
                log.error("Unrecognized option '" + option + "' in compatibility property '" + PROP_COMPAT_KEY + "'");
                continue;
            }
            String prop = split[0];
            String value = defaultIfBlank(split[1], null);
            if (!Arrays.asList(PROP_PASSWORD, //
                    PROP_KEY_STORE_TYPE, //
                    PROP_KEY_STORE_FILE, //
                    PROP_KEY_STORE_PASSWORD, //
                    PROP_KEY_ALIAS, //
                    PROP_KEY_PASSWORD, //
                    PROP_KEY_USE_INSECURE_CIPHER).contains(prop)) {
                log.error("Unrecognized property '" + prop + "' in compatibility property '" + PROP_COMPAT_KEY + "'");
                continue;
            }
            if (properties.containsKey(prop)) {
                log.error("Ignoring property " + option + " in compatibility property '" + PROP_COMPAT_KEY
                        + "' because it is already present as a standard property");
                continue;
            }
            properties.put(prop, value);
        }
    }

    /**
     * Generates an AES key from the password using PBKDF2.
     *
     * @param salt the salt
     */
    protected Key generateSecretKey(byte[] salt) throws GeneralSecurityException {
        char[] pw = password.toCharArray();
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA1);
        PBEKeySpec spec = new PBEKeySpec(pw, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        Key derived = factory.generateSecret(spec);
        spec.clearPassword();
        return new SecretKeySpec(derived.getEncoded(), AES);
    }

    /**
     * Gets the AES key from the keystore.
     */
    protected Key getSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] kspw = keyStorePassword == null ? null : keyStorePassword.toCharArray();
        String keyStoreFile = this.keyStoreFile; // NOSONAR
        if (Framework.isTestModeSet() && keyStoreFile != null) {
            keyStoreFile = Framework.expandVars(keyStoreFile);
        }
        if (keyStoreFile == null) {
            // some keystores are not backed by a file
            keyStore.load(null, kspw);
        } else {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(keyStoreFile)))) {
                keyStore.load(in, kspw);
            }
        }
        char[] kpw = keyPassword == null ? null : keyPassword.toCharArray();
        return keyStore.getKey(keyAlias, kpw);
    }

    protected Cipher getCipher() throws GeneralSecurityException {
        if (useInsecureCipher) {
            return Cipher.getInstance(AES_CBC_PKCS5_PADDING); // NOSONAR
        } else {
            return Cipher.getInstance(AES_GCM_NOPADDING);
        }
    }

    protected AlgorithmParameterSpec getParameterSpec(byte[] iv) {
        if (useInsecureCipher) {
            return new IvParameterSpec(iv);
        } else {
            return new GCMParameterSpec(128, iv);
        }
    }

}
