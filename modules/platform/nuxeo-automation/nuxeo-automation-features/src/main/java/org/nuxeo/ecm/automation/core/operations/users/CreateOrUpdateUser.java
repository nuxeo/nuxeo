/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Michael Vachette
 *     Florent Guillaume
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

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Operation to create or update a user.
 *
 * @since 9.1
 */
@Operation(id = CreateOrUpdateUser.ID, //
        aliases = { "Services.CreateUser" }, //
        category = Constants.CAT_USERS_GROUPS, //
        label = "Create or Update User", //
        description = "Create or Update User.")
public class CreateOrUpdateUser {

    public static final String ID = "User.CreateOrUpdate";

    public static final String CREATE_OR_UPDATE = "createOrUpdate";

    public static final String CREATE = "create";

    public static final String UPDATE = "update";

    protected static final String USER_COLON = SCHEMA_NAME + ':';

    @Context
    protected UserManager userManager;

    @Param(name = "username")
    protected String username;

    @Param(name = "password", required = false)
    protected String password;

    @Param(name = "email", required = false)
    protected String email;

    @Param(name = "firstName", required = false)
    protected String firstName;

    @Param(name = "lastName", required = false)
    protected String lastName;

    @Param(name = "company", required = false)
    protected String company;

    @Param(name = "tenantId", required = false)
    protected String tenantId;

    @Param(name = "groups", required = false)
    protected StringList groups;

    @Param(name = "properties", required = false)
    protected Properties properties = new Properties();

    @Param(name = "mode", required = false, values = { CREATE_OR_UPDATE, CREATE, UPDATE })
    protected String mode;

    @OperationMethod
    public void run() throws OperationException {
        boolean create;
        DocumentModel userDoc = userManager.getUserModel(username);
        if (userDoc == null) {
            if (UPDATE.equals(mode)) {
                throw new OperationException("Cannot update non-existent user: " + username);
            }
            create = true;
            userDoc = userManager.getBareUserModel();
            userDoc.setProperty(SCHEMA_NAME, USERNAME_COLUMN, username);
        } else {
            if (CREATE.equals(mode)) {
                throw new OperationException("Cannot create already-existing user: " + username);
            }
            create = false;
        }
        if (groups != null) {
            userDoc.setProperty(SCHEMA_NAME, GROUPS_COLUMN, groups);
        }
        for (Entry<String, String> entry : Arrays.asList( //
                new SimpleEntry<>(TENANT_ID_COLUMN, tenantId), //
                new SimpleEntry<>(PASSWORD_COLUMN, password), //
                new SimpleEntry<>(EMAIL_COLUMN, email), //
                new SimpleEntry<>(FIRSTNAME_COLUMN, firstName), //
                new SimpleEntry<>(LASTNAME_COLUMN, lastName), //
                new SimpleEntry<>(COMPANY_COLUMN, company))) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                properties.put(key, value);
            }
        }
        for (Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(USER_COLON)) {
                key = key.substring(USER_COLON.length());
            }
            userDoc.setProperty(SCHEMA_NAME, key, value);
        }
        if (create) {
            userDoc = userManager.createUser(userDoc);
        } else {
            userManager.updateUser(userDoc);
            userDoc = userManager.getUserModel(username);
        }
    }

}
