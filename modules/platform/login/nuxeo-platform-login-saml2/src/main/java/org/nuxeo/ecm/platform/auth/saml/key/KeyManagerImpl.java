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

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.SAMLRuntimeException;
import org.opensaml.security.credential.Credential;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * An implementation of {@link KeyManager} that uses a JKS key store.
 */
public class KeyManagerImpl extends DefaultComponent implements KeyManager {

    private static final Logger log = LogManager.getLogger(KeyManagerImpl.class);

    protected static final String XP_CONFIGURATION = "configuration";

    protected Map<String, KeyHolder> keyHolders;

    protected KeyDescriptor config;

    private Set<String> availableCredentials;

    @Override
    public int getApplicationStartedOrder() {
        // start before PluggableAuthenticationService to correctly initialize SAMLAuthenticationProvider
        var authenticationService = Framework.getService(PluggableAuthenticationService.class);
        if (authenticationService == null) { // tests do not deploy the service
            return super.getApplicationStartedOrder();
        } else {
            return authenticationService.getApplicationStartedOrder() - 1;
        }
    }

    @Override
    public void start(ComponentContext context) {
        keyHolders = this.<KeyDescriptor> getDescriptors(XP_CONFIGURATION)
                         .stream()
                         .collect(Collectors.toMap(Descriptor::getId, KeyHolder::new));
    }

    @Override
    public Optional<KeyHolder> getKeyHolder(String name) {
        return Optional.ofNullable(keyHolders.get(name));
    }

    @Override
    @SuppressWarnings("removal")
    public Credential getCredential(String keyName) {
        try {
            return resolveSingle(new CriteriaSet(new EntityIdCriterion(keyName)));
        } catch (ResolverException e) {
            throw new SAMLRuntimeException("Can't obtain SP signing key", e);
        }
    }

    @Override
    @SuppressWarnings("removal")
    public Set<String> getAvailableCredentials() {
        if (availableCredentials != null) {
            return availableCredentials;
        }
        try {
            availableCredentials = new HashSet<>();
            Enumeration<String> aliases = keyHolders.get(KeyDescriptor.DEFAULT_NAME).keyStore.aliases();
            while (aliases.hasMoreElements()) {
                availableCredentials.add(aliases.nextElement());
            }
            return availableCredentials;
        } catch (KeyStoreException e) {
            throw new RuntimeException("Unable to load aliases from keyStore", e);
        }
    }

    @Override
    @SuppressWarnings("removal")
    public X509Certificate getCertificate(String alias) {
        if (alias == null || alias.length() == 0) {
            return null;
        }
        try {
            return (X509Certificate) keyHolders.get(KeyDescriptor.DEFAULT_NAME).keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            log.error("Error loading certificate", e);
        }
        return null;
    }

    @NotNull
    @Override
    @SuppressWarnings("removal")
    public Iterable<Credential> resolve(CriteriaSet criteria) throws ResolverException {
        return keyHolders.get(KeyDescriptor.DEFAULT_NAME).credentialResolver.resolve(criteria);
    }

    @Override
    @SuppressWarnings("removal")
    public Credential resolveSingle(CriteriaSet criteria) throws ResolverException {
        return keyHolders.get(KeyDescriptor.DEFAULT_NAME).credentialResolver.resolveSingle(criteria);
    }

}
