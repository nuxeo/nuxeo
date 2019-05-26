/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.ecm.admin.oauth2;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistryImpl;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.List;

@Name("oauth2ServiceProvidersActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ServiceProvidersActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY = OAuth2ServiceProviderRegistryImpl.DIRECTORY_NAME;

    protected static final String SCHEMA = NuxeoOAuth2ServiceProvider.SCHEMA;

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }

    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

    public String getAuthorizationURL(String provider) {
        OAuth2ServiceProviderRegistry oauth2ProviderRegistry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        OAuth2ServiceProvider serviceProvider = oauth2ProviderRegistry.getProvider(provider);

        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return serviceProvider.getAuthorizationUrl(request);
    }

    public List<DocumentModel> getEnabledProviders() {
        List<DocumentModel> enabledProviders = new ArrayList<>();
        for (DocumentModel entry : getEntries()) {
            boolean isEnabled = (boolean) entry.getProperty(SCHEMA, "enabled");
            String clientId = (String) entry.getProperty(SCHEMA, "clientId");
            String clientSecret = (String) entry.getProperty(SCHEMA, "clientSecret");

            if (isEnabled && clientId != null && clientSecret != null) {
                enabledProviders.add(entry);
            }
        }
        return enabledProviders;
    }
}
