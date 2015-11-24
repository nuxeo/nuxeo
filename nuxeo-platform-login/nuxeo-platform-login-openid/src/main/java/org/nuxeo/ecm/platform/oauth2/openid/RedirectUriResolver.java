package org.nuxeo.ecm.platform.oauth2.openid;

import javax.servlet.http.HttpServletRequest;

/**
 * Helper class to determine the redirect URI based on the current OpenID provider and
 * HTTP request
 *
 * @since 5.7
 */
public interface RedirectUriResolver {
    String getRedirectUri(OpenIDConnectProvider openIDConnectProvider, HttpServletRequest request);
}