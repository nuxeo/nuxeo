package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("externalGadgetsActions")
@Scope(ScopeType.CONVERSATION)
public class ExternalGadgetsActionBean extends DirectoryBasedEditor implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY="external gadget list";
    protected static final String SCHEMA="externalgadget";

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }
    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

}