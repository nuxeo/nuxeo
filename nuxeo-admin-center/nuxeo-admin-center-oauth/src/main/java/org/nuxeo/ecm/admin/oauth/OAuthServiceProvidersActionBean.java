package org.nuxeo.ecm.admin.oauth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.oauth.providers.NuxeoOAuthServiceProvider;
import org.nuxeo.ecm.platform.oauth.providers.OAuthServiceProviderRegistryImpl;

@Name("oauthServiceProvidersActions")
@Scope(ScopeType.CONVERSATION)
public class OAuthServiceProvidersActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY = OAuthServiceProviderRegistryImpl.DIRECTORY_NAME;

    protected static final String SCHEMA = NuxeoOAuthServiceProvider.SCHEMA;

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }

    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

}
