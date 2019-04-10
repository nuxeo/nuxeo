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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
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
        newUser.setProperty("user", "username", username);
        newUser.setProperty("user", "password", password);
        newUser.setProperty("user", "email", email);
        newUser.setProperty("user", "firstName", firstName);
        newUser.setProperty("user", "lastName", lastName);
        newUser.setProperty("user", "company", company);
        newUser.setProperty("user", "tenantId", tenantId);
        newUser.setProperty("user", "groups", groups);

        userManager.createUser(newUser);
    }
}
