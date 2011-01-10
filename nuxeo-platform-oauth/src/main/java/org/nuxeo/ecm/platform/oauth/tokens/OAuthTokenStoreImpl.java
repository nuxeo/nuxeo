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

public class OAuthTokenStoreImpl extends DefaultComponent implements
        OAuthTokenStore {

    protected static final Log log = LogFactory.getLog(OAuthTokenStoreImpl.class);

    protected static final String DIRECTORY_NAME = "oauthTokens";

    protected Map<String, OAuthToken> requestTokenStore=new HashMap<String, OAuthToken>();


    @Override
    public OAuthToken addVerifierToRequestToken(String token) {

        NuxeoOAuthToken rToken = (NuxeoOAuthToken) getRequestToken(token);
        if (rToken!=null) {
            rToken.verifier = "NX-VERIF-" + UUID.randomUUID().toString();
        }
        return rToken;

    }

    @Override
    public OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken) {

        NuxeoOAuthToken aToken = new NuxeoOAuthToken((NuxeoOAuthToken)requestToken);
        String token = "NX-AT-" +  UUID.randomUUID().toString();
        aToken.token =token;
        aToken.tokenSecret = "NX-ATS-" + UUID.randomUUID().toString();
        aToken.type=OAuthToken.Type.ACCESS;

        try {
            aToken= storeAccessTokenAsDirectoryEntry(aToken);
            removeRequestToken(requestToken.getToken());
            return aToken;
        }
        catch (Exception e) {
            log.error("Error during directory persistence", e);
            return null;
        }
     }

    protected NuxeoOAuthToken getTokenFromDirectory(String token) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            DocumentModel entry = session.getEntry(token);
            if (entry==null) {
                return null;
            }
            return getTokenFromDirectoryEntry(entry);
        }
        finally {
            session.close();
        }
    }

    protected NuxeoOAuthToken getTokenFromDirectoryEntry(DocumentModel entry) throws ClientException {
        return new NuxeoOAuthToken(entry);
    }

    protected NuxeoOAuthToken storeAccessTokenAsDirectoryEntry(NuxeoOAuthToken aToken) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            DocumentModel entry = session.getEntry(aToken.getToken());
            if (entry==null) {
                Map<String, Object> init = new HashMap<String, Object>();
                init.put("token", aToken.getToken());
                entry = session.createEntry(init);
            }

            aToken.updateEntry(entry);
            session.updateEntry(entry);

            return getTokenFromDirectoryEntry(session.getEntry(aToken.getToken()));
        }
        finally {
            session.commit();
            session.close();
        }
    }


    @Override
    public OAuthToken createRequestToken(String consumerKey, String callBack) {

        NuxeoOAuthToken rToken = new NuxeoOAuthToken(consumerKey, callBack);
        String token = "NX-RT-" + consumerKey + "-" + UUID.randomUUID().toString();
        rToken.token =token;
        rToken.tokenSecret = "NX-RTS-" + consumerKey + UUID.randomUUID().toString();
        rToken.type = OAuthToken.Type.REQUEST;
        requestTokenStore.put(token, rToken);

        return rToken;
    }

    @Override
    public OAuthToken getAccessToken(String token) {

        try {
            return getTokenFromDirectory(token);
        }
        catch (Exception e) {
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
                DocumentModelList entries = session.query(filter);
                for (DocumentModel entry :entries) {
                    result.add(new NuxeoOAuthToken(entry));
                }
            }
            finally {
                session.commit();
                session.close();
            }
        }
        catch (Exception e) {
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
                DocumentModelList entries = session.query(filter);
                for (DocumentModel entry :entries) {
                    result.add(new NuxeoOAuthToken(entry));
                }
            }
            finally {
                session.commit();
                session.close();
            }
        }
        catch (Exception e) {
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
        }
        finally {
            session.commit();
            session.close();
        }
    }

    @Override
    public void removeRequestToken(String token) {
        requestTokenStore.remove(token);
    }

}
