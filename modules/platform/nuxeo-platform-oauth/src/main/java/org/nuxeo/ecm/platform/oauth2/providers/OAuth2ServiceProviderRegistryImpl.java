/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the {@link OAuth2ServiceProviderRegistry}. The storage backend is a SQL Directory.
 */
public class OAuth2ServiceProviderRegistryImpl extends DefaultComponent implements OAuth2ServiceProviderRegistry {

    private static final Logger log = LogManager.getLogger(OAuth2ServiceProviderRegistryImpl.class);

    public static final String PROVIDER_EP = "providers";

    public static final String DIRECTORY_NAME = "oauth2ServiceProviders";

    public static final String SCHEMA = "oauth2ServiceProvider";

    protected static final String SERVICE_NAME_KEY = "serviceName";

    @Override
    public void start(ComponentContext context) {
        persistProviders();
    }

    protected DocumentModel getPersistedProvider(String serviceName) {
        if (StringUtils.isBlank(serviceName)) {
            log.warn("Cannot fetch provider without a serviceName");
            return null;
        }

        List<DocumentModel> providers = queryProviders(Map.of(SERVICE_NAME_KEY, serviceName), 1);
        return providers.isEmpty() ? null : providers.get(0);
    }

    protected void persistProviders() {
        this.<OAuth2ServiceProviderDescriptor> getRegistryContributions(PROVIDER_EP).forEach(desc -> {
            String name = desc.getName();
            if (getPersistedProvider(name) == null) {
                addProvider(name, desc.getDescription(), desc.getTokenServerURL(), desc.getAuthorizationServerURL(),
                        desc.getClientId(), desc.getClientSecret(), Arrays.asList(desc.getScopes()));
            } else {
                log.info("Provider {} is already in the Database, XML contribution  won't overwrite it", desc::getName);
            }
        });
    }

    @Override
    public OAuth2ServiceProvider getProvider(String serviceName) {
        return buildProvider(getPersistedProvider(serviceName));
    }

    @Override
    public List<OAuth2ServiceProvider> getProviders() {
        List<DocumentModel> providers = queryProviders(Collections.emptyMap(), 0);
        return providers.stream().map(this::buildProvider).collect(Collectors.toList());
    }

    @Override
    public OAuth2ServiceProvider addProvider(String serviceName, String description, String tokenServerURL,
            String authorizationServerURL, String clientId, String clientSecret, List<String> scopes) {
        return addProvider(serviceName, description, tokenServerURL, authorizationServerURL, null, clientId,
                clientSecret, scopes, Boolean.TRUE);
    }

    protected void fillEntryProperties(DocumentModel entry, String serviceName, String description,
            String tokenServerURL, String authorizationServerURL, String userAuthorizationURL, String clientId,
            String clientSecret, List<String> scopes, Boolean isEnabled) {
        entry.setProperty(SCHEMA, SERVICE_NAME_KEY, serviceName);
        entry.setProperty(SCHEMA, "description", description);
        entry.setProperty(SCHEMA, "authorizationServerURL", authorizationServerURL);
        entry.setProperty(SCHEMA, "tokenServerURL", tokenServerURL);
        entry.setProperty(SCHEMA, "userAuthorizationURL", userAuthorizationURL);
        entry.setProperty(SCHEMA, "clientId", clientId);
        entry.setProperty(SCHEMA, "clientSecret", clientSecret);
        entry.setProperty(SCHEMA, "scopes", String.join(",", scopes));
        boolean enabled = (clientId != null && clientSecret != null);
        entry.setProperty(SCHEMA, "enabled", Boolean.valueOf(enabled && Boolean.TRUE.equals(isEnabled)));
        if (!enabled) {
            log.info("OAuth2 provider for {} is disabled because clientId and/or clientSecret are empty", serviceName);
        }
    }

    protected void fillProviderProperties(OAuth2ServiceProvider provider, DocumentModel entry) {
        provider.setId((Long) entry.getProperty(SCHEMA, "id"));
        provider.setDescription((String) entry.getProperty(SCHEMA, "description"));
        provider.setAuthorizationServerURL((String) entry.getProperty(SCHEMA, "authorizationServerURL"));
        provider.setTokenServerURL((String) entry.getProperty(SCHEMA, "tokenServerURL"));
        provider.setUserAuthorizationURL((String) entry.getProperty(SCHEMA, "userAuthorizationURL"));
        provider.setClientId((String) entry.getProperty(SCHEMA, "clientId"));
        provider.setClientSecret((String) entry.getProperty(SCHEMA, "clientSecret"));
        String scopes = (String) entry.getProperty(SCHEMA, "scopes");
        provider.setScopes(StringUtils.split(scopes, ","));
        provider.setEnabled((Boolean) entry.getProperty(SCHEMA, "enabled"));
    }

    @Override
    public OAuth2ServiceProvider addProvider(String serviceName, String description, String tokenServerURL,
            String authorizationServerURL, String userAuthorizationURL, String clientId, String clientSecret,
            List<String> scopes, Boolean isEnabled) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            DocumentModel creationEntry = BaseSession.createEntryModel(SCHEMA, null, null);
            DocumentModel entry = Framework.doPrivileged(() -> session.createEntry(creationEntry));
            fillEntryProperties(entry, serviceName, description, tokenServerURL, authorizationServerURL,
                    userAuthorizationURL, clientId, clientSecret, scopes, isEnabled);
            Framework.doPrivileged(() -> session.updateEntry(entry));
            return getProvider(serviceName);
        }
    }

    @Override
    public OAuth2ServiceProvider updateProvider(String serviceName, OAuth2ServiceProvider provider) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            DocumentModel entry = getPersistedProvider(serviceName);
            fillEntryProperties(entry, serviceName, provider.getDescription(), provider.getTokenServerURL(),
                    provider.getAuthorizationServerURL(), provider.getUserAuthorizationURL(), provider.getClientId(),
                    provider.getClientSecret(), provider.getScopes(), provider.isEnabled());
            session.updateEntry(entry);
            return getProvider(serviceName);
        }
    }

    @Override
    public void deleteProvider(String serviceName) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            DocumentModel entry = getPersistedProvider(serviceName);
            session.deleteEntry(entry);
        }
    }

    protected List<DocumentModel> queryProviders(Map<String, Serializable> filter, int limit) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                Set<String> fulltext = Collections.emptySet();
                Map<String, String> orderBy = Collections.emptyMap();
                return session.query(filter, fulltext, orderBy, true, limit, 0);
            } catch (DirectoryException e) {
                log.error("Error while fetching provider directory", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Instantiates the provider merging the contribution and the directory entry.
     */
    protected OAuth2ServiceProvider buildProvider(DocumentModel entry) {
        if (entry == null) {
            return null;
        }
        String serviceName = (String) entry.getProperty(SCHEMA, SERVICE_NAME_KEY);
        OAuth2ServiceProvider provider = createProviderInstance(serviceName);
        if (provider == null) {
            provider = new NuxeoOAuth2ServiceProvider();
            provider.setServiceName(serviceName);
        }
        fillProviderProperties(provider, entry);
        return provider;
    }

    protected OAuth2ServiceProvider createProviderInstance(String name) {
        return this.<OAuth2ServiceProviderDescriptor> getRegistryContribution(PROVIDER_EP, name).map(desc -> {
            OAuth2ServiceProvider provider = null;
            try {
                Class<? extends OAuth2ServiceProvider> providerClass = desc.getProviderClass();
                provider = providerClass.getDeclaredConstructor().newInstance();
                provider.setDescription(desc.getDescription());
                provider.setAuthorizationServerURL(desc.getAuthorizationServerURL());
                provider.setTokenServerURL(desc.getTokenServerURL());
                provider.setServiceName(desc.getName());
                provider.setClientId(desc.getClientId());
                provider.setClientSecret(desc.getClientSecret());
                provider.setScopes(desc.getScopes());
                provider.setEnabled(true);
            } catch (Exception e) {
                log.error("Failed to instantiate UserResolver", e);
            }
            return provider;
        }).orElse(null);

    }

}
