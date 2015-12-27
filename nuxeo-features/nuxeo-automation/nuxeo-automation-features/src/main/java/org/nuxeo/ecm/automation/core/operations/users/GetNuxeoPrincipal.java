/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.automation.core.operations.users;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.directory.AbstractDirectoryOperation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Fetch a user from {@link UserManager} and return it as a {@link DocumentModel}. Using the DocumentModel rather that a
 * POJO allow to also fetch custom properties.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@Operation(id = GetNuxeoPrincipal.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Nuxeo Principal", description = "Retrieve Nuxeo principal and export it as a DocumentModel, if login parameter is not set the Operation will return informations about the current user, otherwise Directory Administration rights are required.", aliases = { "NuxeoPrincipal.Get" })
public class GetNuxeoPrincipal extends AbstractDirectoryOperation {

    public static final String ID = "User.Get";

    @Context
    protected UserManager umgr;

    @Param(name = "login", required = false)
    protected String login;

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run() {

        if (login == null || login.isEmpty()) {
            return umgr.getUserModel(ctx.getPrincipal().getName());
        } else {
            validateCanManageDirectories(ctx);
            return umgr.getUserModel(login);
        }
    }

}
