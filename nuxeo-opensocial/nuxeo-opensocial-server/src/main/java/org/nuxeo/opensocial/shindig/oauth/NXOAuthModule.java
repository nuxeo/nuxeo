package org.nuxeo.opensocial.shindig.oauth;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.OAuthModule.OAuthCrypterProvider;
import org.apache.shindig.gadgets.oauth.OAuthModule.OAuthRequestProvider;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class NXOAuthModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BlobCrypter.class).annotatedWith(Names.named(OAuthFetcherConfig.OAUTH_STATE_CRYPTER)).toProvider(OAuthCrypterProvider.class);

        // Use Nuxeo Store
        bind(OAuthStore.class).toProvider(NXOAuthStoreProvider.class);

        bind(OAuthRequest.class).toProvider(OAuthRequestProvider.class);

    }

}
