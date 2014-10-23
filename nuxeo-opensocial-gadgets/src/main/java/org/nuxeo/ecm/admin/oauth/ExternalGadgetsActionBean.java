package org.nuxeo.ecm.admin.oauth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("externalGadgetsActions")
@Scope(ScopeType.CONVERSATION)
public class ExternalGadgetsActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY = "externalGadgets";

    protected static final String SCHEMA = "externalgadget";

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }

    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

}
