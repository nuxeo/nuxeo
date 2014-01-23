package org.nuxeo.ecm.admin.oauth2;

import static org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry.OAUTH2CLIENT_DIRECTORY_NAME;
import static org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry.OAUTH2CLIENT_SCHEMA;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@Name("oauth2ClientsActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ClientsActions extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDirectoryName() {
        return OAUTH2CLIENT_DIRECTORY_NAME;
    }

    @Override
    protected String getSchemaName() {
        return OAUTH2CLIENT_SCHEMA;
    }
}
