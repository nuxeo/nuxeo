package org.nuxeo.ecm.platform.ui.web.auth.service;

import javax.servlet.http.HttpServletRequest;

public interface LoginProviderLinkComputer {

    /**
     * Compute Url that should be used to login via this login provider.
     * 
     * Because the url can depend onb the context, it is computed by this method
     * rather than using a static property
     * 
     * @param req
     * @param requestedUrl
     * @return
     * @since 5.7
     */
    String computeUrl(HttpServletRequest req, String requestedUrl);
}
