package org.nuxeo.ecm.platform.ui.web.auth.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.oauth.OAuth;

public class RequestTokenStore {

    private static RequestTokenStore instance;

    public static RequestTokenStore instance() {
        if (instance==null) {
            instance = new RequestTokenStore();
        }
        return instance;
    }

    protected Map<String, Map<String, String>> store = new HashMap<String, Map<String,String>>();

    public Map<String, String> store(String consumerKey, String callBack) {

        Map<String, String> data = new HashMap<String, String>();
        data.put(OAuth.OAUTH_CONSUMER_KEY, consumerKey);
        data.put(OAuth.OAUTH_CALLBACK, callBack);

        String token = "NX-RT-" + consumerKey + "-" + UUID.randomUUID().toString();
        String tokenSecret = "NX-RTS-" + consumerKey + UUID.randomUUID().toString();

        data.put(OAuth.OAUTH_TOKEN, token);
        data.put(OAuth.OAUTH_TOKEN_SECRET, tokenSecret);

        store.put(token, data);

        return data;
    }

    public Map<String, String> generateVerifier(String token) {

        String verif = "NX-VERIF-" + UUID.randomUUID().toString();

        Map<String, String> data = store.get(token);
        data.put("oauth_verifier", verif);
        return data;
    }

    public Map<String, String> get(String token) {
        return store.get(token);
    }

    public void remove(String token) {
        store.remove(token);
    }
}
