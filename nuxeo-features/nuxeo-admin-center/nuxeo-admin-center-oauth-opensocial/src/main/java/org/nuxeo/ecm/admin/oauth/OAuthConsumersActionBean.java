package org.nuxeo.ecm.admin.oauth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistryImpl;

@Name("oauthConsumersActions")
@Scope(ScopeType.CONVERSATION)
public class OAuthConsumersActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY = OAuthConsumerRegistryImpl.DIRECTORY_NAME;

    protected static final String SCHEMA = NuxeoOAuthConsumer.SCHEMA;

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }

    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

}
