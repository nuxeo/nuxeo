/*
 * (C) Copyright 2006-2018 Nuxeo(http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.platform.usermanager.UserConfig.LASTNAME_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.SCHEMA_NAME;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.TENANT_ID_COLUMN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.USERNAME_COLUMN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @since 5.6
 */
@Operation(id = QueryUsers.ID, //
        category = Constants.CAT_USERS_GROUPS, //
        aliases = { "Services.QueryUsers" }, //
        label = "Query users", //
        description = "Query users on a combination of their username, firstName and lastName fields, or on any of them (pattern).")
public class QueryUsers {

    public static final String ID = "User.Query";

    public static final Set<String> FULLTEXT_FIELDS = new HashSet<>(
            Arrays.asList(USERNAME_COLUMN, FIRSTNAME_COLUMN, LASTNAME_COLUMN));

    public static final String JSON_USERNAME = USERNAME_COLUMN;

    public static final String JSON_FIRSTNAME = FIRSTNAME_COLUMN;

    public static final String JSON_LASTNAME = LASTNAME_COLUMN;

    public static final String JSON_EMAIL = EMAIL_COLUMN;

    public static final String JSON_COMPANY = COMPANY_COLUMN;

    public static final String JSON_TENANT_ID = TENANT_ID_COLUMN;

    @Context
    protected UserManager userManager;

    @Param(name = "username", required = false)
    protected String username;

    @Param(name = "firstName", required = false)
    protected String firstName;

    @Param(name = "lastName", required = false)
    protected String lastName;

    @Param(name = "pattern", required = false)
    protected String pattern;

    @Param(name = "tenantId", required = false)
    protected String tenantId;

    @OperationMethod
    public Blob run() {
        List<DocumentModel> users;
        if (StringUtils.isBlank(pattern)) {
            Map<String, Serializable> filter = new HashMap<>();
            if (StringUtils.isNotBlank(username)) {
                filter.put(USERNAME_COLUMN, username);
            }
            if (StringUtils.isNotBlank(firstName)) {
                filter.put(FIRSTNAME_COLUMN, firstName);
            }
            if (StringUtils.isNotBlank(lastName)) {
                filter.put(LASTNAME_COLUMN, lastName);
            }
            if (StringUtils.isNotBlank(tenantId)) {
                filter.put(TENANT_ID_COLUMN, tenantId);
            }
            users = userManager.searchUsers(filter, FULLTEXT_FIELDS);
        } else {
            users = new ArrayList<>();
            Set<String> userIds = new HashSet<>();
            for (String field : FULLTEXT_FIELDS) {
                Map<String, Serializable> filter = new HashMap<>();
                filter.put(field, pattern);
                if (StringUtils.isNotBlank(tenantId)) {
                    filter.put(TENANT_ID_COLUMN, tenantId);
                }
                DocumentModelList userDocs = userManager.searchUsers(filter, Collections.singleton(field));
                for (DocumentModel userDoc : userDocs) {
                    if (userIds.add(userDoc.getId())) { // avoid duplicates
                        users.add(userDoc);
                    }
                }
            }
        }
        return buildResponse(users);
    }

    protected Blob buildResponse(List<DocumentModel> users) {
        JSONArray array = new JSONArray();
        for (DocumentModel user : users) {
            JSONObject o = new JSONObject();
            o.element(JSON_USERNAME, user.getProperty(SCHEMA_NAME, USERNAME_COLUMN));
            o.element(JSON_FIRSTNAME, user.getProperty(SCHEMA_NAME, FIRSTNAME_COLUMN));
            o.element(JSON_LASTNAME, user.getProperty(SCHEMA_NAME, LASTNAME_COLUMN));
            o.element(JSON_EMAIL, user.getProperty(SCHEMA_NAME, EMAIL_COLUMN));
            o.element(JSON_COMPANY, user.getProperty(SCHEMA_NAME, COMPANY_COLUMN));
            o.element(JSON_TENANT_ID, user.getProperty(SCHEMA_NAME, TENANT_ID_COLUMN));
            array.add(o);
        }
        JSONObject result = new JSONObject();
        result.put("users", array);
        return Blobs.createJSONBlob(result.toString());
    }

}
