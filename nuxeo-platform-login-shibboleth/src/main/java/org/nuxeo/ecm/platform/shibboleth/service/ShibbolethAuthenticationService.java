package org.nuxeo.ecm.platform.shibboleth.service;

public interface ShibbolethAuthenticationService {

    ShibbolethAuthenticationConfig getConfig();

    /**
     * Returns the computed login URL to Shibboleth , or {@code null} if no
     * login URL is configured.
     *
     * @param redirectURL
     * @return
     */
    String getLoginURL(String redirectURL);

    /**
     * Returns the computed logout URL to Shibboleth, or {@code null} if no
     * logout URL is configured.
     */
    String getLogoutURL(String redirectURL);

}
