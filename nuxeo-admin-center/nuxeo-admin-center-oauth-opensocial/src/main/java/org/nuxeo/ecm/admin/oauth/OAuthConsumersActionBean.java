package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("oauthConsumersActions")
@Scope(ScopeType.CONVERSATION)
public class OAuthConsumersActionBean extends DirectoryBasedEditor implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY="oauthConsumers";
    protected static final String SCHEMA="oauthConsumer";

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }
    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

}
