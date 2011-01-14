package org.nuxeo.ecm.platform.oauth.providers;

import java.util.List;

public interface OAuthServiceProviderRegistry {

    public abstract NuxeoOAuthServiceProvider getProvider(String gadgetUri,
            String serviceName);

    public abstract NuxeoOAuthServiceProvider addReadOnlyProvider(String gadgetUri,
            String serviceName, String consumerKey, String consumerSecret,
            String publicKey);

    public abstract void deleteProvider(String gadgetUri, String serviceName);

    public abstract void deleteProvider(String providerId);

    public abstract List<NuxeoOAuthServiceProvider> listProviders();

}