package org.nuxeo.ecm.platform.ui.web.auth.shib;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ShibbolethAuthenticationServiceImpl extends DefaultComponent implements ShibbolethAuthenticationService {

    protected ShibbolethAuthenticationConfig config;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("config".equals(extensionPoint)) {
            config = (ShibbolethAuthenticationConfig) contribution;
        }
    }

    public ShibbolethAuthenticationConfig getConfig() {
        return config;
    }

}
