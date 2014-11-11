/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
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

/**
 * Simple operation to get the User's personal Workspace
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
@Operation(id = UserWorkspace.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Home", description = "Retrieve user's personal workspace.")
public class UserWorkspace {

    public static final String ID = "UserWorkspace.Get";

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
        DocumentModel home = uws.getUserPersonalWorkspace(
                session.getPrincipal().getName(), session.getRootDocument());
        return home;
    }
}
