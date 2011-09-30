package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

@Operation(id = UserWorkspace.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Home", description = "Retrieve user's personal workspace.")
public class UserWorkspace {

    public static final String ID = "Userworkspace.Get";

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected AutomationService as;

    @Context
    protected OperationContext context;

    @OperationMethod
    public DocumentModel run() throws Exception {
        UserWorkspaceService uws = Framework.getLocalService(UserWorkspaceService.class);
        DocumentModel home = uws.getUserPersonalWorkspace(session.getPrincipal().getName(), session.getRootDocument());
        return home;
    }
}
