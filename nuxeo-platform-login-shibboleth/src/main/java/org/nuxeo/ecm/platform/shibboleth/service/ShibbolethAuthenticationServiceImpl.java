package org.nuxeo.ecm.platform.shibboleth.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ShibbolethAuthenticationServiceImpl extends DefaultComponent
        implements ShibbolethAuthenticationService {

    public static final String CONFIG_EP = "config";

    protected ShibbolethAuthenticationConfig config;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equals(extensionPoint)) {
            config = (ShibbolethAuthenticationConfig) contribution;
        }
    }

    public ShibbolethAuthenticationConfig getConfig() {
        return config;
    }

    @Override
    public String getLoginURL(String redirectURL) {
        if (config == null || config.getLoginURL() == null) {
            return null;
        }

        Map<String, String> urlParameters = new HashMap<String, String>();
        urlParameters.put(config.getLoginRedirectURLParameter(), redirectURL);
        return URIUtils.addParametersToURIQuery(config.getLoginURL(), urlParameters);
    }

    @Override
    public String getLogoutURL(String redirectURL) {
        if (config == null || config.getLogoutURL() == null) {
            return null;
        }

        Map<String, String> urlParameters = new HashMap<String, String>();
        urlParameters.put(config.getLogoutRedirectURLParameter(), redirectURL);
        return URIUtils.addParametersToURIQuery(config.getLogoutURL(), urlParameters);
    }

    protected String getRedirectUrl(HttpServletRequest request) {
        String redirectURL = VirtualHostHelper.getBaseURL(request);
        if (request.getAttribute(NXAuthConstants.REQUESTED_URL) != null) {
            redirectURL += request.getAttribute(NXAuthConstants.REQUESTED_URL);
        }
        return redirectURL;
    }

    @Override
    public String getLoginURL(HttpServletRequest request) {
        return getLoginURL(getRedirectUrl(request));
    }

    @Override
    public String getLogoutURL(HttpServletRequest request) {
        return getLogoutURL(getRedirectUrl(request));
    }

    @Override
    public String getUserID(HttpServletRequest httpRequest) {
        String idpUrl = httpRequest.getHeader(config.getIdpHeader());
        String uidHeader = config.getUidHeaders().get(idpUrl);
        if (uidHeader == null) {
            uidHeader = config.getDefaultUidHeader();
        }
        return httpRequest.getHeader(uidHeader);
    }

    @Override
    public Map<String, Object> getUserMetadata(String userIdField,
            HttpServletRequest httpRequest) {
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        for (String key : config.getFieldMapping().keySet()) {
            fieldMap.put(config.getFieldMapping().get(key),
                    httpRequest.getHeader(key));
        }
        // Force userIdField to shibb userId value in case of the IdP do
        // not use the same mapping as the default's one.
        fieldMap.put(userIdField, getUserID(httpRequest));
        return fieldMap;
    }

}
