package org.nuxeo.opensocial.shindig.oauth;

import org.apache.shindig.auth.AnonymousAuthenticationHandler;
import org.apache.shindig.auth.UrlParameterAuthenticationHandler;
import org.apache.shindig.social.core.oauth.AuthenticationHandlerProvider;
import org.apache.shindig.social.core.oauth.OAuthAuthenticationHandler;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class NxAuthHandlerProvider extends AuthenticationHandlerProvider {

    @Inject
    public NxAuthHandlerProvider(UrlParameterAuthenticationHandler urlParam,
            OAuthAuthenticationHandler twoLegged,
            AnonymousAuthenticationHandler anonymous) {
        super(urlParam, twoLegged, anonymous);
        handlers = Lists.newArrayList(new NXAuthHandler(), urlParam, twoLegged,
                anonymous);
    }

}
