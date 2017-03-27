/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.automation.core.operations.users;

import static org.nuxeo.ecm.platform.usermanager.UserConfig.COMPANY_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.EMAIL_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.FIRSTNAME_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.GROUPS_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.LASTNAME_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.PASSWORD_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.SCHEMA_NAME;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.TENANT_ID_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.USERNAME_COLUMN;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * @since 5.6
 */
@Operation(id = CreateUser.ID, category = Constants.CAT_SERVICES, label = "Creates a new user", description = "Creates a new user.")
public class CreateUser {

    public static final String ID = "Services.CreateUser";

    @Context
    protected UserManager userManager;

    @Param(name = "username", required = true)
    protected String username;

    @Param(name = "password", required = true)
    protected String password;

    @Param(name = "email", required = true)
    protected String email;

    @Param(name = "firstName", required = false)
    protected String firstName;

    @Param(name = "lastName", required = false)
    protected String lastName;

    @Param(name = "company", required = false)
    protected String company;

    @Param(name = "tenantId", required = false)
    protected String tenantId = "";

    @Param(name = "groups", required = false)
    protected StringList groups;

    @OperationMethod
    public void run() {
        if (userManager.getPrincipal(username) != null) {
            return;
        }
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty(SCHEMA_NAME, USERNAME_COLUMN, username);
        newUser.setProperty(SCHEMA_NAME, PASSWORD_COLUMN, password);
        newUser.setProperty(SCHEMA_NAME, EMAIL_COLUMN, email);
        newUser.setProperty(SCHEMA_NAME, FIRSTNAME_COLUMN, firstName);
        newUser.setProperty(SCHEMA_NAME, LASTNAME_COLUMN, lastName);
        newUser.setProperty(SCHEMA_NAME, COMPANY_COLUMN, company);
        newUser.setProperty(SCHEMA_NAME, TENANT_ID_COLUMN, tenantId);
        newUser.setProperty(SCHEMA_NAME, GROUPS_COLUMN, groups);
        userManager.createUser(newUser);
    }

}
