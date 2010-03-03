package org.nuxeo.opensocial.shindig.oauth;

import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerIndex;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;

import com.google.inject.Singleton;

@Singleton
public class NXOAuthStore extends BasicOAuthStore {

    public NXOAuthStore() {
        super();
    }

    @Override
    public void setConsumerKeyAndSecret(
            BasicOAuthStoreConsumerIndex providerKey,
            BasicOAuthStoreConsumerKeyAndSecret keyAndSecret) {

        String consumerKey = keyAndSecret.getConsumerKey();
        if (consumerKey == null) {
            consumerKey = keyAndSecret.getKeyName();
        }
        BasicOAuthStoreConsumerKeyAndSecret kas = new BasicOAuthStoreConsumerKeyAndSecret(
                consumerKey, keyAndSecret.getConsumerSecret(),
                keyAndSecret.getKeyType(), null, keyAndSecret.getCallbackUrl());

        super.setConsumerKeyAndSecret(providerKey, kas);
    }
}
