package org.nuxeo.opensocial.shindig.oauth;

import java.util.List;

import org.apache.shindig.auth.AnonymousAuthenticationHandler;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.UrlParameterAuthenticationHandler;
import org.apache.shindig.social.core.oauth.AuthenticationHandlerProvider;
import org.apache.shindig.social.core.oauth.OAuthConsumerRequestAuthenticationHandler;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class NxAuthHandlerProvider extends AuthenticationHandlerProvider {
    protected List<AuthenticationHandler> handlers;

    @Inject
    public NxAuthHandlerProvider(UrlParameterAuthenticationHandler urlParam,
            OAuthConsumerRequestAuthenticationHandler twoLegged,
            AnonymousAuthenticationHandler anonymous) {
        super(urlParam, twoLegged, anonymous);
        handlers = Lists.newArrayList(new NXAuthHandler(), urlParam, twoLegged,
                anonymous);
    }

    @Override
    public List<AuthenticationHandler> get() {
        return handlers;
    }
}
