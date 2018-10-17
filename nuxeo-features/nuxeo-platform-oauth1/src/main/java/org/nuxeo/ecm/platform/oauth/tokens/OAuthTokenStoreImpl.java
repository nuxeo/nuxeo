/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.tokens;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service implementation for {@link OAuthTokenStore}.
 * <p>
 * This service is responsible for managing storage of the {@link OAuthToken}. A simple SQL Directory is used for ACCESS
 * Token whereas a simple in memory storage is used for REQUEST Tokens.
 *
 * @author tiry
 */
public class OAuthTokenStoreImpl extends DefaultComponent implements OAuthTokenStore {

    protected static final Log log = LogFactory.getLog(OAuthTokenStoreImpl.class);

    public static final String DIRECTORY_NAME = "oauthTokens";

    protected Map<String, OAuthToken> requestTokenStore = new HashMap<String, OAuthToken>();

    @Override
    public OAuthToken addVerifierToRequestToken(String token, Long duration) {
        NuxeoOAuthToken rToken = (NuxeoOAuthToken) getRequestToken(token);
        if (rToken != null) {
            rToken.verifier = "NX-VERIF-" + UUID.randomUUID().toString();
            rToken.durationInMinutes = duration;
        }
        return rToken;
    }

    @Override
    public OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken) {
        NuxeoOAuthToken aToken = new NuxeoOAuthToken((NuxeoOAuthToken) requestToken);
        String token = "NX-AT-" + UUID.randomUUID().toString();
        aToken.token = token;
        aToken.tokenSecret = "NX-ATS-" + UUID.randomUUID().toString();
        aToken.type = OAuthToken.Type.ACCESS;

        try {
            aToken = storeAccessTokenAsDirectoryEntry(aToken);
            removeRequestToken(requestToken.getToken());
            return aToken;
        } catch (DirectoryException e) {
            log.error("Error during directory persistence", e);
            return null;
        }
    }

    @Override
    public NuxeoOAuthToken getClientAccessToken(String appId, String owner) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("appId", appId);
            filter.put("clientId", owner);
            filter.put("clientToken", 1);
            DocumentModelList entries = session.query(filter);
            if (entries.size() == 0) {
                return null;
            }
            if (entries.size() > 1) {
                log.error("Found several tokens");
            }
            return getTokenFromDirectoryEntry(entries.get(0));
        }
    }

    @Override
    public void removeClientAccessToken(String appId, String owner) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("appId", appId);
            filter.put("clientId", owner);
            filter.put("clientToken", 1);
            DocumentModelList entries = session.query(filter);
            if (entries.size() == 0) {
                return;
            }
            if (entries.size() > 1) {
                log.error("Found several tokens");
            }
            session.deleteEntry(entries.get(0));
        }
    }

    @Override
    public void storeClientAccessToken(String consumerKey, String callBack, String token, String tokenSecret,
            String appId, String owner) {
        NuxeoOAuthToken aToken = new NuxeoOAuthToken(consumerKey, callBack);
        aToken.token = token;
        aToken.tokenSecret = tokenSecret;
        if (appId != null) {
            aToken.appId = appId;
        }

        aToken.clientToken = true;
        aToken.clientId = owner;
        try {
            aToken = storeAccessTokenAsDirectoryEntry(aToken);
        } catch (DirectoryException e) {
            log.error("Error during directory persistence", e);
        }
    }

    protected NuxeoOAuthToken getTokenFromDirectory(String token) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                DocumentModel entry = session.getEntry(token);
                if (entry == null) {
                    return null;
                }
                return getTokenFromDirectoryEntry(entry);
            }
        });
    }

    protected NuxeoOAuthToken getTokenFromDirectoryEntry(DocumentModel entry) {
        return new NuxeoOAuthToken(entry);
    }

    protected NuxeoOAuthToken storeAccessTokenAsDirectoryEntry(NuxeoOAuthToken aToken) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                DocumentModel entry = session.getEntry(aToken.getToken());
                if (entry == null) {
                    entry = session.createEntry(Collections.singletonMap("token", aToken.getToken()));
                }

                aToken.updateEntry(entry);
                session.updateEntry(entry);

                return getTokenFromDirectoryEntry(session.getEntry(aToken.getToken()));
            }
        });
    }

    @Override
    public OAuthToken createRequestToken(String consumerKey, String callBack) {

        NuxeoOAuthToken rToken = new NuxeoOAuthToken(consumerKey, callBack);
        String token = "NX-RT-" + consumerKey + "-" + UUID.randomUUID().toString();
        rToken.token = token;
        rToken.tokenSecret = "NX-RTS-" + consumerKey + UUID.randomUUID().toString();
        rToken.type = OAuthToken.Type.REQUEST;
        requestTokenStore.put(token, rToken);

        return rToken;
    }

    @Override
    public OAuthToken getAccessToken(String token) {

        try {
            return getTokenFromDirectory(token);
        } catch (DirectoryException e) {
            log.error("Error while accessing Token SQL storage", e);
            return null;
        }
    }

    @Override
    public OAuthToken getRequestToken(String token) {
        return requestTokenStore.get(token);
    }

    @Override
    public List<OAuthToken> listAccessTokenForConsumer(String consumerKey) {
        List<OAuthToken> result = new ArrayList<OAuthToken>();

        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("consumerKey", consumerKey);
            filter.put("clientToken", 0);
            DocumentModelList entries = session.query(filter);
            for (DocumentModel entry : entries) {
                result.add(new NuxeoOAuthToken(entry));
            }
        } catch (DirectoryException e) {
            log.error("Error during token listing", e);
        }
        return result;
    }

    @Override
    public List<OAuthToken> listAccessTokenForUser(String login) {
        List<OAuthToken> result = new ArrayList<OAuthToken>();
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("nuxeoLogin", login);
            filter.put("clientToken", 0);
            DocumentModelList entries = session.query(filter);
            for (DocumentModel entry : entries) {
                result.add(new NuxeoOAuthToken(entry));
            }
        } catch (DirectoryException e) {
            log.error("Error during token listing", e);
        }
        return result;
    }

    @Override
    public void removeAccessToken(String token) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(DIRECTORY_NAME)) {
            session.deleteEntry(token);
        }
    }

    @Override
    public void removeRequestToken(String token) {
        requestTokenStore.remove(token);
    }

}
