/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.oauth2.tokens;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class OAuth2TokenStore <V extends Serializable> implements DataStore<StoredCredential> {

    protected static final Log log = LogFactory.getLog(OAuth2TokenStore.class);

    public static final String DIRECTORY_NAME = "oauth2Tokens";

    private String serviceName;

    private DataStoreFactory dataStoreFactory;

    public OAuth2TokenStore(String serviceName, DataStoreFactory dataStoreFactory) {
        this.serviceName = serviceName;
        this.dataStoreFactory = dataStoreFactory;
    }

    @Override
    public DataStore<StoredCredential> set(String userId, StoredCredential credential) throws IOException {
        store(userId, new NuxeoOAuth2Token(credential));
        return this;
    }

    public void store(String userId, NuxeoOAuth2Token token) {
        token.setServiceName(serviceName);
        token.setNuxeoLogin(userId);
        try {
            storeTokenAsDirectoryEntry(token);
        } catch (ClientException e) {
            log.error("Error during token storage", e);
        }
    }

    public NuxeoOAuth2Token refresh(String refreshToken, String clientId) throws ClientException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("clientId", clientId);
        filter.put("refreshToken", refreshToken);
        filter.put("serviceName", serviceName);

        NuxeoOAuth2Token token = getToken(filter);
        if (token != null) {
            delete(token.getAccessToken(), clientId);
            token.refresh();
            return storeTokenAsDirectoryEntry(token);
        }
        return null;
    }

    public void delete(String token, String clientId) throws ClientException {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = ds.open(DIRECTORY_NAME);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("serviceName", serviceName);
            filter.put("clientId", clientId);
            filter.put("accessToken", token);

            DocumentModelList entries = session.query(filter);
            for (DocumentModel entry : entries) {
                session.deleteEntry(entry);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public DataStore<StoredCredential> delete(String key) throws IOException {
        return null;
    }

    @Override
    public StoredCredential get(String serviceLogin) throws IOException {
        NuxeoOAuth2Token token = getToken(serviceName, serviceLogin);
        if (token == null) {
            return null;
        }

        StoredCredential credential = new StoredCredential();
        credential.setAccessToken(token.getAccessToken());
        credential.setRefreshToken(token.getRefreshToken());
        credential.setExpirationTimeMilliseconds(token.getExpirationTimeMilliseconds());
        return credential;
    }

    public NuxeoOAuth2Token getToken(String token) throws ClientException {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("serviceName", serviceName);
        filter.put("accessToken", token);

        return getToken(filter);
    }

    protected NuxeoOAuth2Token getToken(Map<String, Serializable> filter) throws ClientException {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = ds.open(DIRECTORY_NAME);
            DocumentModelList entries = session.query(filter);
            if (entries.size() == 0) {
                return null;
            }
            if (entries.size() > 1) {
                log.error("Found several tokens");
            }
            return getTokenFromDirectoryEntry(entries.get(0));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public NuxeoOAuth2Token getToken(String serviceName, String serviceLogin) throws ClientException {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("serviceName", serviceName);
        filter.put("serviceLogin", serviceLogin);

        NuxeoOAuth2Token token = getToken(filter);
        if (token == null) {
            // fallback to get a token using nuxeoLogin when serviceLogin is not set
            filter.replace("serviceLogin", null);
            filter.put("nuxeoLogin", serviceLogin);
            token = getToken(filter);
        }
        return token;
    }

    protected NuxeoOAuth2Token getTokenFromDirectoryEntry(DocumentModel entry) throws ClientException {
        return new NuxeoOAuth2Token(entry);
    }

    protected NuxeoOAuth2Token storeTokenAsDirectoryEntry(NuxeoOAuth2Token aToken) throws ClientException {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = ds.open(DIRECTORY_NAME);

            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            Map<String, Object> aTokenMap = aToken.toMap();
            filter.put("refreshToken", (String) aTokenMap.get("refreshToken"));
            DocumentModelList entries = session.query(filter);
            DocumentModel entry;

            if (entries.isEmpty()) {
                // add new token
                entry = session.createEntry(aTokenMap);
            } else {
                // update existing token
                entry = entries.get(0);
                entry.setProperty("oauth2Token", "accessToken", aTokenMap.get("accessToken"));
                entry.setProperty("oauth2Token", "creationDate", aTokenMap.get("creationDate"));
            }

            session.updateEntry(entry);
            return getTokenFromDirectoryEntry(entry);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public DataStoreFactory getDataStoreFactory() {
        return dataStoreFactory;
    }

    @Override
    public String getId() {
        return serviceName;
    }

    @Override
    public int size() throws IOException {
        return 0;
    }

    @Override
    public boolean isEmpty() throws IOException {
        return false;
    }

    @Override
    public boolean containsKey(String key) throws IOException {
        return false;
    }

    @Override
    public boolean containsValue(StoredCredential value) throws IOException {
        return false;
    }

    @Override
    public Set<String> keySet() throws IOException {
        return null;
    }

    @Override
    public Collection<StoredCredential> values() throws IOException {
        return null;
    }

    @Override
    public DataStore<StoredCredential> clear() throws IOException {
        return null;
    }
}
