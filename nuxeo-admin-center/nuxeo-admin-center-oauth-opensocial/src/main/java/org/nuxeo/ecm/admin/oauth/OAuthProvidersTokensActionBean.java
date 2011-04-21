package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.oauth.tokens.NuxeoOAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStoreImpl;

@Name("oauthProvidersTokensActions")
@Scope(ScopeType.CONVERSATION)
public class OAuthProvidersTokensActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    @Override
    protected Map<String, Serializable> getQueryFilter() {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("clientToken", 1);
        return filter;
    }

    @Override
    protected String getDirectoryName() {
        return OAuthTokenStoreImpl.DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return NuxeoOAuthToken.SCHEMA;
    }

}
