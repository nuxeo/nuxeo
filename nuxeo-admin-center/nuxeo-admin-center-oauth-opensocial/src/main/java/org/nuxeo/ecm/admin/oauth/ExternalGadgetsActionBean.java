package org.nuxeo.ecm.admin.oauth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("externalGadgetsActions")
@Scope(ScopeType.CONVERSATION)
public class ExternalGadgetsActionBean extends DirectoryBasedEditor {

    protected static final String DIRECTORY = "external gadget list";
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