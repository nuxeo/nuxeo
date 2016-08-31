/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 *     André Justo
 */
package org.nuxeo.ecm.platform.oauth2.tokens;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

/**
 * {@link DataStore} backed by a Nuxeo Directory
 *
 * @since 7.3
 */
public class OAuth2TokenStore implements DataStore<StoredCredential> {

    protected static final Log log = LogFactory.getLog(OAuth2TokenStore.class);

    public static final String DIRECTORY_NAME = "oauth2Tokens";

    public static final String ENTRY_ID = "id";

    private String serviceName;

    public OAuth2TokenStore(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public DataStore<StoredCredential> set(String key, StoredCredential credential) throws IOException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put(ENTRY_ID, key);
        DocumentModel entry = find(filter);

        if (entry == null) {
            store(key, new NuxeoOAuth2Token(credential));
        } else {
            refresh(entry, new NuxeoOAuth2Token(credential));
        }
        return this;
    }

    @Override
    public DataStore<StoredCredential> delete(String key) throws IOException {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("serviceName", serviceName);
            filter.put(ENTRY_ID, key);

            DocumentModelList entries = session.query(filter);
            for (DocumentModel entry : entries) {
                session.deleteEntry(entry);
            }
        }
        return this;
    }

    @Override
    public StoredCredential get(String key) throws IOException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put(ENTRY_ID, key);
        DocumentModel entry = find(filter);
        return entry != null ? NuxeoOAuth2Token.asCredential(entry) : null;
    }

    @Override
    public DataStoreFactory getDataStoreFactory() {
        return null;
    }

    public final String getId() {
        return this.serviceName;
    }

    @Override
    public boolean containsKey(String key) throws IOException {
        return this.get(key) != null;
    }

    @Override
    public boolean containsValue(StoredCredential value) throws IOException {
        return this.values().contains(value);
    }

    @Override
    public boolean isEmpty() throws IOException {
        return this.size() == 0;
    }

    @Override
    public int size() throws IOException {
        return this.keySet().size();
    }

    @Override
    public Set<String> keySet() throws IOException {
        Set<String> keys = new HashSet<>();
        DocumentModelList entries = query();
        for (DocumentModel entry : entries) {
            keys.add((String) entry.getProperty(NuxeoOAuth2Token.SCHEMA, ENTRY_ID));
        }
        return keys;
    }

    @Override
    public Collection<StoredCredential> values() throws IOException {
        List<StoredCredential> results = new ArrayList<>();
        DocumentModelList entries = query();
        for (DocumentModel entry : entries) {
            results.add(NuxeoOAuth2Token.asCredential(entry));
        }
        return results;
    }

    @Override
    public DataStore<StoredCredential> clear() throws IOException {
        return null;
    }

    /*
     * Methods used by Nuxeo when acting as OAuth2 provider
     */
    public void store(String userId, NuxeoOAuth2Token token) {
        token.setServiceName(serviceName);
        token.setNuxeoLogin(userId);
        try {
            storeTokenAsDirectoryEntry(token);
        } catch (DirectoryException e) {
            log.error("Error during token storage", e);
        }
    }

    public NuxeoOAuth2Token refresh(String refreshToken, String clientId) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("clientId", clientId);
        filter.put("refreshToken", refreshToken);
        filter.put("serviceName", serviceName);

        DocumentModel entry = find(filter);
        if (entry != null) {
            NuxeoOAuth2Token token = getTokenFromDirectoryEntry(entry);
            delete(token.getAccessToken(), clientId);
            token.refresh();
            return storeTokenAsDirectoryEntry(token);
        }
        return null;
    }

    public NuxeoOAuth2Token refresh(DocumentModel entry, NuxeoOAuth2Token token) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                entry.setProperty("oauth2Token", "accessToken", token.getAccessToken());
                entry.setProperty("oauth2Token", "refreshToken", token.getRefreshToken());
                entry.setProperty("oauth2Token", "creationDate", token.getCreationDate());
                entry.setProperty("oauth2Token", "expirationTimeMilliseconds", token.getExpirationTimeMilliseconds());
                session.updateEntry(entry);
                return getTokenFromDirectoryEntry(entry);
            }
        });
    }

    public void delete(String token, String clientId) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put("serviceName", serviceName);
                filter.put("clientId", clientId);
                filter.put("accessToken", token);

                DocumentModelList entries = session.query(filter);
                for (DocumentModel entry : entries) {
                    session.deleteEntry(entry);
                }
            }
        });
    }

    /**
     * Retrieve an entry by it's accessToken
     */
    public NuxeoOAuth2Token getToken(String token) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("accessToken", token);

        DocumentModelList entries = query(filter);
        if (entries.size() == 0) {
            return null;
        }
        if (entries.size() > 1) {
            log.error("Found several tokens");
        }
        return getTokenFromDirectoryEntry(entries.get(0));
    }

    public DocumentModelList query() {
        return query(new HashMap<>());
    }

    public DocumentModelList query(Map<String, Serializable> filter) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                filter.put("serviceName", serviceName);
                return session.query(filter);
            }
        });
    }

    protected NuxeoOAuth2Token getTokenFromDirectoryEntry(DocumentModel entry) {
        return new NuxeoOAuth2Token(entry);
    }

    protected NuxeoOAuth2Token storeTokenAsDirectoryEntry(NuxeoOAuth2Token aToken) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                DocumentModel entry = session.createEntry(aToken.toMap());
                session.updateEntry(entry);
                return getTokenFromDirectoryEntry(entry);
            }
        });
    }

    protected DocumentModel find(Map<String, Serializable> filter) {
        DocumentModelList entries = query(filter);
        if (entries.size() == 0) {
            return null;
        }
        if (entries.size() > 1) {
            log.error("Found several tokens");
        }
        return entries.get(0);
    }
}
