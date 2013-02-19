package org.nuxeo.ecm.admin.oauth2;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;

@Name("oauth2ProvidersTokensActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ProvidersTokensActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    @Override
    protected Map<String, Serializable> getQueryFilter() {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        // filter.put("clientToken", 1);
        return filter;
    }

    @Override
    protected String getDirectoryName() {
        return OAuth2TokenStore.DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return "oauth2Token";
    }

}
