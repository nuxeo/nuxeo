package org.nuxeo.ecm.platform.ui.web.auth.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.oauth.OAuth;

public class AccessTokenStore {

    private static AccessTokenStore instance;

    public static AccessTokenStore instance() {
        if (instance==null) {
            instance = new AccessTokenStore();
        }
        return instance;
    }

    protected Map<String, Map<String, String>> store = new HashMap<String, Map<String,String>>();

    public Map<String, String> generate(Map<String, String> tmpData) {

        if (tmpData==null) {
            tmpData = new HashMap<String, String>();
        }
        Map<String, String> data = tmpData;

        String token = "NX-AT-" +  UUID.randomUUID().toString();
        String tokenSecret = "NX-ATS-" + UUID.randomUUID().toString();

        data.put(OAuth.OAUTH_TOKEN, token);
        data.put(OAuth.OAUTH_TOKEN_SECRET, tokenSecret);

        store.put(token, data);

        return data;
    }

    public Map<String, String> get(String token) {
        return store.get(token);
    }
}
