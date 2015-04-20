package org.nuxeo.ecm.admin.oauth2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
    protected String getDirectoryName() {
        return OAuth2TokenStore.DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return "oauth2Token";
    }

    public List<String> getSharedWith() {
        List<String> sharedWith = new ArrayList<>();
        String sharedWithProperty = (String) editableEntry.getProperty(getSchemaName(), "sharedWith");
        if (sharedWithProperty != null) {
            sharedWith = Arrays.asList(sharedWithProperty.split(","));
        }
        return sharedWith;
    }

    public void setSharedWith(List<String> sharedWith) {
        String list = StringUtils.join(sharedWith, ",");
        editableEntry.setProperty(getSchemaName(), "sharedWith", list);
    }
}
