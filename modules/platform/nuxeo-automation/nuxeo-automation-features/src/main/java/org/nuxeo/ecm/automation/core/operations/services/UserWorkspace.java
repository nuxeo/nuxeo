/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@Operation(id = UserWorkspace.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Home", description = "Retrieve user's personal workspace.", aliases = { "UserWorkspace.Get" })
public class UserWorkspace {

    public static final String ID = "User.GetUserWorkspace";

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected AutomationService as;

    @Context
    protected OperationContext context;

    @OperationMethod
    public DocumentModel run() {
        UserWorkspaceService uws = Framework.getService(UserWorkspaceService.class);
        DocumentModel home = uws.getUserPersonalWorkspace(session.getPrincipal().getName(), session.getRootDocument());
        return home;
    }
}
