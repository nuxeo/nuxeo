/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.auth.saml.key;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.SAMLRuntimeException;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * @since 2023.44
 */
public class KeyHolder {

    protected static final String KEYSTORE_TYPE = "JKS";

    protected final KeyDescriptor configuration;

    protected final KeyStore keyStore;

    protected final KeyStoreCredentialResolver credentialResolver;

    public KeyHolder(KeyDescriptor configuration) {
        try {
            this.configuration = configuration;
            keyStore = instantiateKeyStore(configuration.getKeystoreFilePath(), configuration.getKeystorePassword());
            credentialResolver = new KeyStoreCredentialResolver(keyStore, configuration.getPasswords());
        } catch (SecurityException e) {
            throw new NuxeoException("Unable to instantiate the KeyHolder", e);
        }
    }

    public Optional<Credential> getSigningCredential() {
        return getCredential(configuration.getSigningKey());
    }

    public Optional<Credential> getEncryptionCredential() {
        return getCredential(configuration.getEncryptionKey());
    }

    public Optional<Credential> getTlsCredential() {
        return getCredential(configuration.getTlsKey());
    }

    protected Optional<Credential> getCredential(String keyName) {
        if (keyName == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(
                    credentialResolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(keyName))));
        } catch (ResolverException e) {
            throw new SAMLRuntimeException("Can't obtain SP signing key", e);
        }
    }

    private static KeyStore instantiateKeyStore(String path, String password) throws SecurityException {
        try {
            File rootKeystoreFile = new File(path);
            if (!rootKeystoreFile.exists()) {
                throw new SecurityException(
                        "Unable to find keyStore at " + new File(".").getAbsolutePath() + File.separator + path);
            }
            try (InputStream keystoreIS = new FileInputStream(rootKeystoreFile)) {
                KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
                ks.load(keystoreIS, password.toCharArray());
                return ks;
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new SecurityException("Unable to load the key store", e);
        }
    }
}
