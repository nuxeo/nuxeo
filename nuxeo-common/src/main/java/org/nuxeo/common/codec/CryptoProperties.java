/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.codec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;

/**
 * {@link Properties} with crypto capabilities.<br>
 * The cryptographic algorithms depend on:
 * <ul>
 * <li>Environment.SERVER_STATUS_KEY</li>
 * <li>Environment.CRYPT_KEYALIAS && Environment.CRYPT_KEYSTORE_PATH || getProperty(Environment.JAVA_DEFAULT_KEYSTORE)
 * </li>
 * <li>Environment.CRYPT_KEY</li>
 * </ul>
 * Changing one of those parameters will affect the ability to read encrypted values.
 *
 * @see Crypto
 * @since 7.4
 */
public class CryptoProperties extends Properties {
    private static final Log log = LogFactory.getLog(CryptoProperties.class);

    private static final Crypto Crypto_NO_OP = Crypto.NoOp.NO_OP;

    private Crypto crypto = Crypto_NO_OP;

    private static final List<String> CRYPTO_PROPS = Arrays.asList(Environment.SERVER_STATUS_KEY,
            Environment.CRYPT_KEYALIAS, Environment.CRYPT_KEYSTORE_PATH, Environment.JAVA_DEFAULT_KEYSTORE,
            Environment.CRYPT_KEYSTORE_PASS, Environment.JAVA_DEFAULT_KEYSTORE_PASS, Environment.CRYPT_KEY);

    private byte[] cryptoID;

    private static final int SALT_LEN = 8;

    private final byte[] salt = new byte[SALT_LEN];

    private static final Random random = new SecureRandom();

    private Map<String, String> encrypted = new ConcurrentHashMap<>();

    /**
     * {@link Properties#Properties(Properties)}
     */
    public CryptoProperties(Properties defaults) {
        super(defaults);
        synchronized (random) {
            random.nextBytes(salt);
        }
        cryptoID = evalCryptoID();
    }

    private byte[] evalCryptoID() {
        byte[] ID = null;
        for (String prop : CRYPTO_PROPS) {
            ID = ArrayUtils.addAll(ID, salt);
            ID = ArrayUtils.addAll(ID, getProperty(prop, "").getBytes());
        }
        return crypto.getSHA1DigestOrEmpty(ID);
    }

    public CryptoProperties() {
        this(null);
    }

    private static final long serialVersionUID = 1L;

    public Crypto getCrypto() {
        String statusKey = getProperty(Environment.SERVER_STATUS_KEY);
        String keyAlias = getProperty(Environment.CRYPT_KEYALIAS);
        String keystorePath = getProperty(Environment.CRYPT_KEYSTORE_PATH,
                getProperty(Environment.JAVA_DEFAULT_KEYSTORE));
        if (keyAlias != null && keystorePath != null) {
            String keystorePass = getProperty(Environment.CRYPT_KEYSTORE_PASS);
            if (StringUtils.isNotEmpty(keystorePass)) {
                keystorePass = new String(Base64.decodeBase64(keystorePass));
            } else {
                keystorePass = getProperty(Environment.JAVA_DEFAULT_KEYSTORE_PASS, "changeit");
            }
            try {
                return new Crypto(keystorePath, keystorePass.toCharArray(), keyAlias, statusKey.toCharArray());
            } catch (GeneralSecurityException | IOException e) {
                log.warn(e);
                return Crypto_NO_OP;
            }
        }

        String secretKey = new String(Base64.decodeBase64(getProperty(Environment.CRYPT_KEY, "")));
        if (StringUtils.isNotEmpty(secretKey)) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(secretKey).openStream()))) {
                secretKey = in.readLine();
            } catch (MalformedURLException e) {
                // It's a raw value, not an URL => fall through
            } catch (IOException e) {
                log.warn(e);
                return Crypto_NO_OP;
            }
        } else {
            secretKey = statusKey;
        }
        if (secretKey == null) {
            log.warn("Missing " + Environment.SERVER_STATUS_KEY);
            return Crypto_NO_OP;
        }
        return new Crypto(secretKey.getBytes());
    }

    private boolean isNewCryptoProperty(String key, String value) {
        return CRYPTO_PROPS.contains(key) && !StringUtils.equals(value, getProperty(key));
    }

    private void resetCrypto() {
        byte[] id = evalCryptoID();
        if (!Arrays.equals(id, cryptoID)) {
            cryptoID = id;
            crypto = getCrypto();
        }
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        Properties props = new Properties();
        props.load(reader);
        putAll(props);
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        Properties props = new Properties();
        props.load(inStream);
        putAll(props);
    }

    protected class PropertiesGetDefaults extends Properties {
        private static final long serialVersionUID = 1L;

        public Properties getDefaults() {
            return defaults;
        }

        public Hashtable<String, Object> getDefaultProperties() {
            Hashtable<String, Object> h = new Hashtable<>();
            if (defaults != null) {
                Enumeration<?> allDefaultProperties = defaults.propertyNames();
                while (allDefaultProperties.hasMoreElements()) {
                    String key = (String) allDefaultProperties.nextElement();
                    String value = defaults.getProperty(key);
                    h.put(key, value);
                }
            }
            return h;
        }
    }

    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        PropertiesGetDefaults props = new PropertiesGetDefaults();
        props.loadFromXML(in);
        if (defaults == null) {
            defaults = props.getDefaults();
        } else {
            defaults.putAll(props.getDefaultProperties());
        }
        putAll(props);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        Objects.requireNonNull(value);
        String sKey = (String) key;
        String sValue = (String) value;
        if (isNewCryptoProperty(sKey, sValue)) { // Crypto properties are not themselves encrypted
            Object old = super.put(sKey, sValue);
            resetCrypto();
            return old;
        }
        if (Crypto.isEncrypted(sValue)) {
            encrypted.put(sKey, sValue);
            sValue = new String(crypto.decrypt(sValue));
        }
        return super.put(sKey, sValue);
    }

    @Override
    public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
        for (String key : CRYPTO_PROPS) {
            if (t.containsKey(key)) {
                super.put(key, t.get(key));
            }
        }
        resetCrypto();
        for (Map.Entry<? extends Object, ? extends Object> e : t.entrySet()) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (Crypto.isEncrypted(value)) {
                encrypted.put(key, value);
                value = new String(crypto.decrypt(value));
            }
            super.put(key, value);
        }
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        Objects.requireNonNull(value);
        String sKey = (String) key;
        String sValue = (String) value;
        if (get(key) != null) { // Not absent: do nothing, return current value
            return get(key);
        }
        if (isNewCryptoProperty(sKey, sValue)) { // Crypto properties are not themselves encrypted
            Object old = super.putIfAbsent(sKey, sValue);
            resetCrypto();
            return old;
        }
        if (Crypto.isEncrypted(sValue)) {
            encrypted.putIfAbsent(sKey, sValue);
            sValue = new String(crypto.decrypt(sValue));
        }
        return super.putIfAbsent(sKey, sValue);
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        String sKey = (String) key;
        String sOldValue = (String) oldValue;
        String sNewValue = (String) newValue;

        if (isNewCryptoProperty(sKey, sNewValue)) { // Crypto properties are not themselves encrypted
            if (super.replace(key, sOldValue, sNewValue)) {
                resetCrypto();
                return true;
            } else {
                return false;
            }
        }
        if (super.replace(sKey, new String(crypto.decrypt(sOldValue)), new String(crypto.decrypt(sNewValue)))) {
            if (Crypto.isEncrypted(sNewValue)) {
                encrypted.put(sKey, sNewValue);
            } else {
                encrypted.remove(sKey);
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        Objects.requireNonNull(value);
        if (!super.containsKey(key)) {
            return null;
        }
        return put(key, value);
    }

    @Override
    public synchronized Object merge(Object key, Object value,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        // If the specified key is not already associated with a value or is associated with null, associates it with
        // the given non-null value.
        if (get(key) == null) {
            putIfAbsent(key, value);
            return value;
        }
        if (CRYPTO_PROPS.contains(key)) { // Crypto properties are not themselves encrypted
            Object newValue = super.merge(key, value, remappingFunction);
            resetCrypto();
            return newValue;
        }
        String sKey = (String) key;
        String sValue = (String) value;
        if (Crypto.isEncrypted(sValue)) {
            encrypted.put(sKey, sValue);
            sValue = new String(crypto.decrypt(sValue));
        }
        return super.merge(sKey, sValue, remappingFunction);
    }

    /**
     * @return the "raw" property: not decrypted if it was provided encrypted
     */
    public String getRawProperty(String key) {
        return getProperty(key, true);
    }

    /**
     * Searches for the property with the specified key in this property list. If the key is not found in this property
     * list, the default property list, and its defaults, recursively, are then checked. The method returns the default
     * value argument if the property is not found.
     *
     * @return the "raw" property (not decrypted if it was provided encrypted) or the {@code defaultValue} if not found
     * @see #setProperty
     */
    public String getRawProperty(String key, String defaultValue) {
        String val = getRawProperty(key);
        return (val == null) ? defaultValue : val;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, false);
    }

    /**
     * @param raw if the encrypted values must be returned encrypted ({@code raw==true}) or decrypted (
     *            {@code raw==false} )
     * @return the property value or null
     */
    public String getProperty(String key, boolean raw) {
        Object oval = super.get(key);
        String value = (oval instanceof String) ? (String) oval : null;
        if (value == null) {
            if (defaults == null) {
                encrypted.remove(key); // cleanup
            } else if (defaults instanceof CryptoProperties) {
                value = ((CryptoProperties) defaults).getProperty(key, raw);
            } else {
                value = defaults.getProperty(key);
                if (Crypto.isEncrypted(value)) {
                    encrypted.put(key, value);
                    if (!raw) {
                        value = new String(crypto.decrypt(value));
                    }
                }
            }
        } else if (raw && encrypted.containsKey(key)) {
            value = encrypted.get(key);
        }
        return value;
    }

    @Override
    public synchronized Object remove(Object key) {
        encrypted.remove(key);
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        if (super.remove(key, value)) {
            encrypted.remove(key);
            return true;
        }
        return false;
    }

}
