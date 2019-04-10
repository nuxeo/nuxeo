/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
    public void run() throws Exception {
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
