/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.tokens;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service implementation for {@link OAuthTokenStore}.
 *
 * This service is responsible for managing storage of the {@link OAuthToken}. A
 * simple SQL Directory is used for ACCESS Token whereas a simple in memory
 * storage is used for REQUEST Tokens.
 *
 *
 * @author tiry
 *
 */
public class OAuthTokenStoreImpl extends DefaultComponent implements
        OAuthTokenStore {

    protected static final Log log = LogFactory.getLog(OAuthTokenStoreImpl.class);

    public static final String DIRECTORY_NAME = "oauthTokens";

    protected Map<String, OAuthToken> requestTokenStore = new HashMap<String, OAuthToken>();

    @Override
    public OAuthToken addVerifierToRequestToken(String token, Long duration) {

        NuxeoOAuthToken rToken = (NuxeoOAuthToken) getRequestToken(token);
        if (rToken != null) {
            rToken.verifier = "NX-VERIF-" + UUID.randomUUID().toString();
            rToken.durationInMinutes=duration;
        }
        return rToken;

    }

    @Override
    public OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken) {

        NuxeoOAuthToken aToken = new NuxeoOAuthToken(
                (NuxeoOAuthToken) requestToken);
        String token = "NX-AT-" + UUID.randomUUID().toString();
        aToken.token = token;
        aToken.tokenSecret = "NX-ATS-" + UUID.randomUUID().toString();
        aToken.type = OAuthToken.Type.ACCESS;

        try {
            aToken = storeAccessTokenAsDirectoryEntry(aToken);
            removeRequestToken(requestToken.getToken());
            return aToken;
        } catch (Exception e) {
            log.error("Error during directory persistence", e);
            return null;
        }
    }


    public NuxeoOAuthToken getClientAccessToken(String appId, String owner) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("appId", appId);
            filter.put("clientId", owner);
            filter.put("clientToken", 1);
            DocumentModelList entries = session.query(filter);
            if (entries.size()==0) {
                return  null;
            }
            if (entries.size()>1) {
                log.error("Found several tokens");
            }
            return getTokenFromDirectoryEntry(entries.get(0));
        } finally {
            session.close();
        }
    }

    public void removeClientAccessToken(String appId, String owner) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("appId", appId);
            filter.put("clientId", owner);
            filter.put("clientToken", 1);
            DocumentModelList entries = session.query(filter);
            if (entries.size()==0) {
                return;
            }
            if (entries.size()>1) {
                log.error("Found several tokens");
            }
            session.deleteEntry(entries.get(0));
        } finally {
            session.close();
        }

    }



    public void storeClientAccessToken(String consumerKey, String callBack, String token, String tokenSecret, String appId, String owner) {
        NuxeoOAuthToken aToken= new NuxeoOAuthToken(consumerKey,callBack);
        aToken.token = token;
        aToken.tokenSecret = tokenSecret;
        if (appId!=null) {
            aToken.appId = appId;
        }

        aToken.clientToken=true;
        aToken.clientId = owner;
        try {
            aToken = storeAccessTokenAsDirectoryEntry(aToken);
        } catch (Exception e) {
            log.error("Error during directory persistence", e);
        }

    }

    protected NuxeoOAuthToken getTokenFromDirectory(String token)
            throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            DocumentModel entry = session.getEntry(token);
            if (entry == null) {
                return null;
            }
            return getTokenFromDirectoryEntry(entry);
        } finally {
            session.close();
        }
    }

    protected NuxeoOAuthToken getTokenFromDirectoryEntry(DocumentModel entry)
            throws ClientException {
        return new NuxeoOAuthToken(entry);
    }

    protected NuxeoOAuthToken storeAccessTokenAsDirectoryEntry(
            NuxeoOAuthToken aToken) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            DocumentModel entry = session.getEntry(aToken.getToken());
            if (entry == null) {
                Map<String, Object> init = new HashMap<String, Object>();
                init.put("token", aToken.getToken());
                entry = session.createEntry(init);
            }

            aToken.updateEntry(entry);
            session.updateEntry(entry);

            return getTokenFromDirectoryEntry(session.getEntry(aToken.getToken()));
        } finally {
            session.commit();
            session.close();
        }
    }

    @Override
    public OAuthToken createRequestToken(String consumerKey, String callBack) {

        NuxeoOAuthToken rToken = new NuxeoOAuthToken(consumerKey, callBack);
        String token = "NX-RT-" + consumerKey + "-"
                + UUID.randomUUID().toString();
        rToken.token = token;
        rToken.tokenSecret = "NX-RTS-" + consumerKey
                + UUID.randomUUID().toString();
        rToken.type = OAuthToken.Type.REQUEST;
        requestTokenStore.put(token, rToken);

        return rToken;
    }

    @Override
    public OAuthToken getAccessToken(String token) {

        try {
            return getTokenFromDirectory(token);
        } catch (Exception e) {
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

        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(DIRECTORY_NAME);
            try {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put("consumerKey", consumerKey);
                filter.put("clientToken", 0);
                DocumentModelList entries = session.query(filter);
                for (DocumentModel entry : entries) {
                    result.add(new NuxeoOAuthToken(entry));
                }
            } finally {
                session.commit();
                session.close();
            }
        } catch (Exception e) {
            log.error("Error during token listing", e);
        }
        return result;
    }

    @Override
    public List<OAuthToken> listAccessTokenForUser(String login) {

        List<OAuthToken> result = new ArrayList<OAuthToken>();

        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(DIRECTORY_NAME);
            try {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put("nuxeoLogin", login);
                filter.put("clientToken", 0);
                DocumentModelList entries = session.query(filter);
                for (DocumentModel entry : entries) {
                    result.add(new NuxeoOAuthToken(entry));
                }
            } finally {
                session.commit();
                session.close();
            }
        } catch (Exception e) {
            log.error("Error during token listing", e);
        }
        return result;

    }

    @Override
    public void removeAccessToken(String token) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            session.deleteEntry(token);
        } finally {
            session.commit();
            session.close();
        }
    }

    @Override
    public void removeRequestToken(String token) {
        requestTokenStore.remove(token);
    }

}
