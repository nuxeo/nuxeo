package org.nuxeo.ecm.platform.usermanager;

import org.nuxeo.ecm.core.api.DocumentModel;

public class DefaultUserMultiTenantManagementMock extends DefaultUserMultiTenantManagement {

    @Override
    protected String getDirectorySuffix(DocumentModel context) {
        if (context == null) {
            return null;
        }
        return "-tenanta";
    }
}
