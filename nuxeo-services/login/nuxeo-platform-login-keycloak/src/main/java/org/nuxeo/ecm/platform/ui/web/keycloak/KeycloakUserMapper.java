/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     François Maturel
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.extension.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for the UserMapper to manage mapping between Ketcloack user and Nuxeo counterpart
 *
 * @since 7.4
 */
public class KeycloakUserMapper implements UserMapper {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserMapper.class);

    protected static String userSchemaName = "user";

    protected static String groupSchemaName = "group";

    protected UserManager userManager;

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject) {
        return getOrCreateAndUpdateNuxeoPrincipal(userObject, true, true, null);
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject, boolean createIfNeeded, boolean update,
            Map<String, Serializable> params) {

        KeycloakUserInfo userInfo = (KeycloakUserInfo) userObject;
        for (String role : userInfo.getRoles()) {
            findOrCreateGroup(role, userInfo.getUserName());
        }

        // Remember that username is email by default
        DocumentModel userDoc = findUser(userInfo);
        if (userDoc == null) {
            userDoc = createUser(userInfo);
        }

        userDoc = updateUser(userDoc, userInfo);

        String userId = (String) userDoc.getPropertyValue(userManager.getUserIdField());
        return userManager.getPrincipal(userId);
    }

    @Override
    public void init(Map<String, String> params) throws Exception {
        userManager = Framework.getService(UserManager.class);
        userSchemaName = userManager.getUserSchemaName();
        groupSchemaName = userManager.getGroupSchemaName();
    }

    private DocumentModel findOrCreateGroup(String role, String userName) {
        DocumentModel groupDoc = findGroup(role);
        if (groupDoc == null) {
            groupDoc = userManager.getBareGroupModel();
            groupDoc.setPropertyValue(userManager.getGroupIdField(), role);
            groupDoc.setProperty(groupSchemaName, "groupname", role);
            groupDoc.setProperty(groupSchemaName, "grouplabel", role + " group");
            groupDoc.setProperty(groupSchemaName, "description",
                    "Group automatically created by Keycloak based on user role [" + role + "]");
            groupDoc = userManager.createGroup(groupDoc);
        }
        List<String> users = userManager.getUsersInGroupAndSubGroups(role);
        if (!users.contains(userName)) {
            users.add(userName);
            groupDoc.setProperty(groupSchemaName, userManager.getGroupMembersField(), users);
            userManager.updateGroup(groupDoc);
        }
        return groupDoc;
    }

    private DocumentModel findGroup(String role) {
        Map<String, Serializable> query = new HashMap<>();
        query.put(userManager.getGroupIdField(), role);
        DocumentModelList groups = userManager.searchGroups(query, null);

        if (groups.isEmpty()) {
            return null;
        }
        return groups.get(0);
    }

    private DocumentModel findUser(UserIdentificationInfo userInfo) {
        Map<String, Serializable> query = new HashMap<>();
        query.put(userManager.getUserIdField(), userInfo.getUserName());
        DocumentModelList users = userManager.searchUsers(query, null);

        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    private DocumentModel createUser(KeycloakUserInfo userInfo) {
        DocumentModel userDoc;
        try {
            userDoc = userManager.getBareUserModel();
            userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
            userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
            userManager.createUser(userDoc);
        } catch (NuxeoException e) {
            String message = "Error while creating user [" + userInfo.getUserName() + "] in UserManager";
            log.error(message, e);
            throw new RuntimeException(message);
        }
        return userDoc;
    }

    private DocumentModel updateUser(DocumentModel userDoc, KeycloakUserInfo userInfo) {
        userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
        userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
        userDoc.setProperty(userSchemaName, "firstName", userInfo.getFirstName());
        userDoc.setProperty(userSchemaName, "lastName", userInfo.getLastName());
        userDoc.setProperty(userSchemaName, "password", userInfo.getPassword());
        userDoc.setProperty(userSchemaName, "company", userInfo.getCompany());
        userManager.updateUser(userDoc);
        return userDoc;
    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object nativePrincipal, Map<String, Serializable> params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
    }

}
