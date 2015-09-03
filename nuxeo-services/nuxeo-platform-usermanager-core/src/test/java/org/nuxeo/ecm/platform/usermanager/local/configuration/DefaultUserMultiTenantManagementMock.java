package org.nuxeo.ecm.platform.usermanager.local.configuration;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.DefaultUserMultiTenantManagement;

public class DefaultUserMultiTenantManagementMock extends DefaultUserMultiTenantManagement {

    @Override
    protected String getDirectorySuffix(DocumentModel context) {
        if (context == null) {
            return null;
        }
        return "-tenanta";
    }
}
