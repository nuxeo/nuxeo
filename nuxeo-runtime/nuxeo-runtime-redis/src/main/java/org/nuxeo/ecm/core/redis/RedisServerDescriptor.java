/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.redis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * Descriptor for a Redis configuration.
 *
 * @since 5.8
 */
@XObject("server")
public class RedisServerDescriptor extends RedisPoolDescriptor {

    @XNode("host")
    public String host;

    @XNode("port")
    public int port = Protocol.DEFAULT_PORT;

    /** @since 10.3 */
    @XNode("ssl")
    public boolean ssl;

    /** @since 10.3 */
    @XNode("trustStorePath")
    public String trustStorePath;

    /** @since 10.3 */
    @XNode("trustStorePassword")
    public String trustStorePassword;

    /** @since 10.3 */
    @XNode("trustStoreType")
    public String trustStoreType;

    /** @since 10.3 */
    @XNode("keyStorePath")
    public String keyStorePath;

    /** @since 10.3 */
    @XNode("keyStorePassword")
    public String keyStorePassword;

    /** @since 10.3 */
    @XNode("keyStoreType")
    public String keyStoreType;

    @XNode("failoverTimeout")
    public int failoverTimeout = 300;

    @Override
    public RedisExecutor newExecutor() {
        SSLContext sslContext = getSSLContext();
        boolean useSSL;
        SSLSocketFactory sslSocketFactory;
        if (sslContext == null) {
            useSSL = ssl;
            sslSocketFactory = null;
        } else {
            useSSL = true;
            sslSocketFactory = sslContext.getSocketFactory();
        }
        try (Jedis jedis = new Jedis(host, port, useSSL, sslSocketFactory, null, null)) {
            if (StringUtils.isNotBlank(password)) {
                jedis.auth(password);
            }
            String pong = jedis.ping();
            if (!"PONG".equals(pong)) {
                throw new RuntimeException("Cannot connect to Redis host: " + host + ":" + port);
            }
        }

        JedisPoolConfig conf = new JedisPoolConfig();
        conf.setMaxTotal(maxTotal);
        conf.setMaxIdle(maxIdle);
        RedisExecutor base = new RedisPoolExecutor(new JedisPool(conf, host, port, timeout,
                StringUtils.defaultIfBlank(password, null), database));
        return new RedisFailoverExecutor(failoverTimeout, base);
    }

    protected SSLContext getSSLContext() {
        try {
            KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword, trustStoreType);
            KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword, keyStoreType);
            if (trustStore == null && keyStore == null) {
                return null;
            }
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (trustStore != null) {
                sslContextBuilder.loadTrustMaterial(trustStore, null);
            }
            if (keyStore != null) {
                sslContextBuilder.loadKeyMaterial(keyStore, null);
            }
            return sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Cannot setup SSL context", e);
        }
    }

    protected KeyStore loadKeyStore(String path, String password, String type)
            throws GeneralSecurityException, IOException {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String keyStoreType = StringUtils.defaultIfBlank(type, KeyStore.getDefaultType());
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] passwordChars = StringUtils.isBlank(password) ? null : password.toCharArray();
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            keyStore.load(is, passwordChars);
        }
        return keyStore;
    }

}
