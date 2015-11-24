package org.nuxeo.ecm.platform.shibboleth.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.URIUtils;
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

}
