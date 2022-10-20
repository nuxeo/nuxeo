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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the {@link OAuth2ServiceProviderRegistry}. The storage backend is a SQL Directory.
 */
public class OAuth2ServiceProviderRegistryImpl extends DefaultComponent implements OAuth2ServiceProviderRegistry {

    protected static final Log log = LogFactory.getLog(OAuth2ServiceProviderRegistryImpl.class);

    public static final String PROVIDER_EP = "providers";

    public static final String DIRECTORY_NAME = "oauth2ServiceProviders";

    public static final String SCHEMA = "oauth2ServiceProvider";

    /**
     * Registry of contributed providers. These providers can extend and/or override the default provider class.
     */
    protected OAuth2ServiceProviderContributionRegistry registry = new OAuth2ServiceProviderContributionRegistry();

    protected DocumentModel getProviderDocModel(String serviceName) {
        try {
            if (StringUtils.isBlank(serviceName)) {
                log.warn("Can not find provider without a serviceName!");
                return null;
            }

            Map<String, Serializable> filter = new HashMap<>();
            filter.put("serviceName", serviceName);

            List<DocumentModel> providers = queryProviders(filter, 1);
            return providers.isEmpty() ? null : providers.get(0);
        } catch (DirectoryException e) {
            log.error("Unable to read provider from Directory backend", e);
            return null;
        }
    }

    @Override
    public OAuth2ServiceProvider getProvider(String serviceName) {
        DocumentModel model = getProviderDocModel(serviceName);
        return model == null ? null : buildProvider(model);
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

    @Override
    public OAuth2ServiceProvider addProvider(String serviceName, String description, String tokenServerURL,
            String authorizationServerURL, String userAuthorizationURL, String clientId, String clientSecret,
            List<String> scopes, Boolean isEnabled) {
        if (StringUtils.isBlank(serviceName)) {
            throw new IllegalArgumentException("The provider's service name cannot be blank!");
        }
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            DocumentModel creationEntry = BaseSession.createEntryModel(null, SCHEMA, null, null);
            DocumentModel entry = Framework.doPrivileged(() -> session.createEntry(creationEntry));
            entry.setProperty(SCHEMA, "serviceName", serviceName);
            entry.setProperty(SCHEMA, "description", description);
            entry.setProperty(SCHEMA, "authorizationServerURL", authorizationServerURL);
            entry.setProperty(SCHEMA, "tokenServerURL", tokenServerURL);
            entry.setProperty(SCHEMA, "userAuthorizationURL", userAuthorizationURL);
            entry.setProperty(SCHEMA, "clientId", clientId);
            entry.setProperty(SCHEMA, "clientSecret", clientSecret);
            entry.setProperty(SCHEMA, "scopes", String.join(",", scopes));
            boolean enabled = (clientId != null && clientSecret != null);
            entry.setProperty(SCHEMA, "enabled", Boolean.valueOf(enabled && (isEnabled == null ? false : isEnabled)));
            if (!enabled) {
                log.info("OAuth2 provider for " + serviceName
                        + " is disabled because clientId and/or clientSecret are empty");
            }
            Framework.doPrivileged(() -> session.updateEntry(entry));
            return getProvider(serviceName);
        }
    }

    @Override
    public OAuth2ServiceProvider updateProvider(String serviceName, OAuth2ServiceProvider provider) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            DocumentModel entry = getProviderDocModel(serviceName);
            entry.setProperty(SCHEMA, "serviceName", provider.getServiceName());
            entry.setProperty(SCHEMA, "description", provider.getDescription());
            entry.setProperty(SCHEMA, "authorizationServerURL", provider.getAuthorizationServerURL());
            entry.setProperty(SCHEMA, "tokenServerURL", provider.getTokenServerURL());
            entry.setProperty(SCHEMA, "userAuthorizationURL", provider.getUserAuthorizationURL());
            entry.setProperty(SCHEMA, "clientId", provider.getClientId());
            entry.setProperty(SCHEMA, "clientSecret", provider.getClientSecret());
            entry.setProperty(SCHEMA, "scopes", String.join(",", provider.getScopes()));
            boolean enabled = provider.getClientId() != null && provider.getClientSecret() != null;
            entry.setProperty(SCHEMA, "enabled", Boolean.valueOf(enabled && provider.isEnabled()));
            if (!enabled) {
                log.info("OAuth2 provider for " + serviceName
                        + " is disabled because clientId and/or clientSecret are empty");
            }
            session.updateEntry(entry);
            return getProvider(serviceName);
        }
    }

    @Override
    public void deleteProvider(String serviceName) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            DocumentModel entry = getProviderDocModel(serviceName);
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
     * Instantiates the provider merging the contribution and the directory entry
     */
    protected OAuth2ServiceProvider buildProvider(DocumentModel entry) {
        String serviceName = (String) entry.getProperty(SCHEMA, "serviceName");
        OAuth2ServiceProvider provider = registry.getProvider(serviceName);
        if (provider == null) {
            provider = new NuxeoOAuth2ServiceProvider();
            provider.setServiceName(serviceName);
        }
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
        return provider;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PROVIDER_EP.equals(extensionPoint)) {
            OAuth2ServiceProviderDescriptor provider = (OAuth2ServiceProviderDescriptor) contribution;
            log.info("OAuth2 provider for " + provider.getName() + " will be registered at application startup");
            // delay registration because data sources may not be available
            // at this point
            registry.addContribution(provider);
        }
    }

    @Override
    public void start(ComponentContext context) {
        registerCustomProviders();
    }

    protected void registerCustomProviders() {
        for (OAuth2ServiceProviderDescriptor provider : registry.getContribs()) {
            if (getProvider(provider.getName()) == null) {
                addProvider(provider.getName(), provider.getDescription(), provider.getTokenServerURL(),
                        provider.getAuthorizationServerURL(), provider.getClientId(), provider.getClientSecret(),
                        Arrays.asList(provider.getScopes()));
            } else {
                log.info("Provider " + provider.getName()
                        + " is already in the Database, XML contribution  won't overwrite it");
            }
        }
    }
}
