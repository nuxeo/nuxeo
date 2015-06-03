/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
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

    public String getAuthorizationURL(String provider) throws ClientException {
        OAuth2ServiceProviderRegistry oauth2ProviderRegistry = Framework.getLocalService(OAuth2ServiceProviderRegistry.class);
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
