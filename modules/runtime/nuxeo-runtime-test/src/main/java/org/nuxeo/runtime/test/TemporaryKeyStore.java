/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.test;

import static org.nuxeo.common.function.ThrowableConsumer.asConsumer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.rules.ExternalResource;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2023.0
 */
public class TemporaryKeyStore extends ExternalResource {

    protected final String keyStoreType;

    protected final String keyStorePassword;

    protected final List<KeyStoreEntry<Key>> keyEntries;

    protected final List<KeyStoreEntry<KeyPair>> keyPairEntries;

    protected Path keyStorePath;

    protected TemporaryKeyStore(Builder builder) {
        this.keyStoreType = builder.keyStoreType;
        this.keyStorePassword = builder.keyStorePassword;
        this.keyEntries = builder.keyEntries;
        this.keyPairEntries = builder.keyPairEntries;
    }

    @Override
    protected void before() {
        create();
    }

    @Override
    protected void after() {
        delete();
    }

    protected void create() {
        try {
            // create an empty keyStore
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            // add keys to the store
            keyEntries.forEach(asConsumer(entry -> keyStore.setKeyEntry(entry.alias(), entry.key(),
                    entry.password().toCharArray(), entry.certificates())));
            // add keyPairs to the store
            keyPairEntries.forEach(asConsumer(entry -> keyStore.setKeyEntry(entry.alias(), entry.key().getPrivate(),
                    entry.password().toCharArray(), entry.certificates())));
            // write the keyStore content to the disk
            keyStorePath = Files.createTempFile(Path.of(FeaturesRunner.getBuildDirectory()), "nuxeo-keyStore-",
                    "." + keyStoreType.toLowerCase());
            try (OutputStream out = Files.newOutputStream(keyStorePath)) {
                keyStore.store(out, keyStorePassword.toCharArray());
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new AssertionError("Unable to build the keyStore", e);
        }
    }

    protected void delete() {
        if (keyStorePath != null) {
            try {
                Files.deleteIfExists(keyStorePath);
            } catch (IOException e) {
                throw new AssertionError("Unable to delete keyStore", e);
            }
        }
    }

    public Path getPath() {
        return keyStorePath;
    }

    public KeyStoreEntry<KeyPair> getKeyPair(String alias) {
        return keyPairEntries.stream()
                             .filter(entry -> alias.equals(entry.alias()))
                             .findFirst()
                             .orElseThrow(() -> new AssertionError(
                                     String.format("The keyPair with alias: %s doesn't exist", alias)));
    }

    public static final class Builder {

        protected final String keyStoreType;

        protected final String keyStorePassword;

        protected final List<KeyStoreEntry<Key>> keyEntries = new ArrayList<>();

        protected final List<KeyStoreEntry<KeyPair>> keyPairEntries = new ArrayList<>();

        public Builder(String keyStoreType, String keyStorePassword) {
            this.keyStoreType = keyStoreType;
            this.keyStorePassword = keyStorePassword;
        }

        /**
         * Generates a {@link Key} to set to the {@link KeyStore}.
         */
        public Builder generateKey(String alias, String password, String algorithm, int keySize) {
            try {
                var keyGenerator = KeyGenerator.getInstance(algorithm);
                keyGenerator.init(keySize);
                var key = keyGenerator.generateKey();
                keyEntries.add(new KeyStoreEntry<>(alias, password, key, null));
                return this;
            } catch (GeneralSecurityException e) {
                throw new AssertionError("Unable to generate a key", e);
            }
        }

        /**
         * Generates a {@link KeyPair} with {@code RSA} algorithm and a {@code 2048} size to set to the
         * {@link KeyStore}.
         */
        public Builder generateKeyPair(String alias, String password) {
            return generateKeyPair(alias, password, "RSA", 2048, "CN=Nuxeo O=Nuxeo", "SHA256withRSA");
        }

        /**
         * Generates a {@link KeyPair} to set to the {@link KeyStore}.
         */
        public Builder generateKeyPair(String alias, String password, String algorithm, int keySize,
                String certificateDnName, String certificateAlgorithm) {
            try {
                var keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
                keyPairGenerator.initialize(keySize);
                var keyPair = keyPairGenerator.generateKeyPair();
                keyPairEntries.add(new KeyStoreEntry<>(alias, password, keyPair,
                        generateCertificate(certificateDnName, keyPair, certificateAlgorithm)));
                return this;
            } catch (GeneralSecurityException | OperatorCreationException e) {
                throw new AssertionError("Unable to generate a keyPair", e);
            }
        }

        protected X509Certificate generateCertificate(String dnName, KeyPair keyPair, String sigAlgName)
                throws GeneralSecurityException, OperatorCreationException {
            var validityBeginDate = Date.from(ZonedDateTime.now().minusDays(1).toInstant());
            var validityEndDate = Date.from(ZonedDateTime.now().plusYears(2).toInstant());

            // Define the content of the certificate
            var issuer = new X500Principal(dnName);
            var certificateBuilder = new JcaX509v1CertificateBuilder(issuer,
                    BigInteger.valueOf(System.currentTimeMillis()), validityBeginDate, validityEndDate, issuer,
                    keyPair.getPublic());

            var certSigner = new JcaContentSignerBuilder(sigAlgName).setProvider(new BouncyCastleProvider())
                                                                    .build(keyPair.getPrivate());
            return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                                                    .getCertificate(certificateBuilder.build(certSigner));
        }

        public TemporaryKeyStore build() {
            return new TemporaryKeyStore(this);
        }
    }

    public record KeyStoreEntry<K> (String alias, String password, K key, X509Certificate certificate) {

        public Certificate[] certificates() {
            if (certificate == null) {
                return null;
            }
            return new Certificate[] { certificate };
        }
    }
}
