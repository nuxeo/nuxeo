package org.nuxeo.opensocial.shindig.oauth;

import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class NXOAuthStoreProvider implements Provider<OAuthStore>{

    protected NXOAuthStore store;

    public NXOAuthStoreProvider() {

        store = new NXOAuthStore();
        OpenSocialService os = Framework.getLocalService(OpenSocialService.class);
        store.setDefaultCallbackUrl(os.getOAuthCallbackUrl());
        // XXX to be moved to oAuth package
        String privateKey = os.getOAuthPrivateKeyContent();
        privateKey = BasicOAuthStore.convertFromOpenSsl(privateKey);
        String signingKeyName = os.getOAuthPrivateKeyName();
        BasicOAuthStoreConsumerKeyAndSecret key = new BasicOAuthStoreConsumerKeyAndSecret(null, privateKey, KeyType.RSA_PRIVATE,signingKeyName, null);
        store.setDefaultKey(key);

        // XXX load entries from OpenSocial Service config
    }

    @Override
    public OAuthStore get() {
        return store;
    }

}