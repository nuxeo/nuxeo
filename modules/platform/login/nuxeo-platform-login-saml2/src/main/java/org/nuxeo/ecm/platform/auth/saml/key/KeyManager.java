/*
 * (C) Copyright 2014-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.key;

import static org.nuxeo.ecm.platform.auth.saml.key.KeyDescriptor.DEFAULT_NAME;

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * A manager for {@link KeyHolder}s.
 *
 * @since 6.0
 */
public interface KeyManager extends CredentialResolver {

    /**
     * @since 2023.44
     */
    default Optional<KeyHolder> getKeyHolder(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * @deprecated since 2023.44, only used internally, no replacement
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    Credential getCredential(String keyName);

    /**
     * @deprecated since 2023.44, not used, no replacement
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    Set<String> getAvailableCredentials();

    /**
     * @deprecated since 2023.44, not used, no replacement
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    X509Certificate getCertificate(String alias);

    /**
     * @deprecated since 2023.44, first retrieve a {@link KeyHolder} with {@link #getKeyHolder(String)} and use
     *             {@link KeyHolder#getSigningCredential()}
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    default Credential getSigningCredential() {
        return getKeyHolder(DEFAULT_NAME).flatMap(KeyHolder::getSigningCredential).orElse(null);
    }

    /**
     * @deprecated since 2023.44, first retrieve a {@link KeyHolder} with {@link #getKeyHolder(String)} and use
     *             {@link KeyHolder#getEncryptionCredential()}
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    default Credential getEncryptionCredential() {
        return getKeyHolder(DEFAULT_NAME).flatMap(KeyHolder::getEncryptionCredential).orElse(null);
    }

    /**
     * @deprecated since 2023.44, first retrieve a {@link KeyHolder} with {@link #getKeyHolder(String)} and use
     *             {@link KeyHolder#getTlsCredential()}
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    default Credential getTlsCredential() {
        return getKeyHolder(DEFAULT_NAME).flatMap(KeyHolder::getTlsCredential).orElse(null);
    }

    /**
     * @deprecated since 2023.44, only used internally, no replacement
     */
    @Nonnull
    @Override
    @Deprecated(since = "2023.44", forRemoval = true)
    Iterable<Credential> resolve(@Nullable CriteriaSet criteria) throws ResolverException;

    /**
     * @deprecated since 2023.44, only used internally, no replacement
     */
    @Nullable
    @Override
    @Deprecated(since = "2023.44", forRemoval = true)
    Credential resolveSingle(@Nullable CriteriaSet criteria) throws ResolverException;
}
