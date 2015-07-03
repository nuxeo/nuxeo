/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NuxeoUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NuxeoUserService.class);

    protected static final String USER_SCHEMANAME = "user";

    protected static final String GROUP_SCHEMANAME = "group";


    // @see https://en.wikipedia.org/wiki/Singleton_pattern
    private static volatile NuxeoUserService INSTANCE;

    private UserManager userManager;

    private NuxeoUserService() {
        userManager = Framework.getLocalService(UserManager.class);
    }

    public static NuxeoUserService getInstance() {
        if (INSTANCE == null) {
            synchronized (NuxeoUserService.class) {
                //double checking Singleton instance
                if (INSTANCE == null) {
                    INSTANCE = new NuxeoUserService();
                }
            }
        }
        return INSTANCE;
    }

    public String findOrCreateUser(KeycloakUserInfo userInfo, Set<String> roles) {

        for (String role : roles) {
            findOrCreateGroup(role, userInfo.getUserName());
        }

        // Remember that username is email by default
        DocumentModel userDoc = findUser(userInfo);
        if (userDoc == null) {
            userDoc = createUser(userInfo);
        }
        userDoc = updateUser(userDoc, userInfo);

        return (String) userDoc.getPropertyValue(userManager.getUserIdField());
    }

    private DocumentModel findOrCreateGroup(String role, String userName) {
        DocumentModel groupDoc = findGroup(role);
        if (groupDoc == null) {
            groupDoc = userManager.getBareGroupModel();
            groupDoc.setPropertyValue(userManager.getGroupIdField(), role);
            groupDoc.setProperty(GROUP_SCHEMANAME, "groupname", role);
            groupDoc.setProperty(GROUP_SCHEMANAME, "grouplabel", role + " group");
            groupDoc.setProperty(GROUP_SCHEMANAME, "description", "Group automatically created by Keycloak based on user role [" + role + "]");
            groupDoc = userManager.createGroup(groupDoc);
        }
        List<String> users = userManager.getUsersInGroupAndSubGroups(role);
        if (!users.contains(userName)) {
            users.add(userName);
            groupDoc.setProperty(GROUP_SCHEMANAME, userManager.getGroupMembersField(), users);
            userManager.updateGroup(groupDoc);
        }
        return groupDoc;
    }

    private DocumentModel findGroup(String role) {
        try {
            Map<String, Serializable> query = new HashMap<>();
            query.put(userManager.getGroupIdField(), role);
            DocumentModelList groups = userManager.searchGroups(query, null);

            if (groups.isEmpty()) {
                return null;
            }
            return groups.get(0);
        } catch (ClientException e) {
            String message = "Error while searching group [" + role + "] in UserManager";
            LOGGER.error(message, e);
            throw new RuntimeException(message);
        }
    }

    private DocumentModel findUser(UserIdentificationInfo userInfo) {
        try {
            Map<String, Serializable> query = new HashMap<>();
            query.put(userManager.getUserIdField(), userInfo.getUserName());
            DocumentModelList users = userManager.searchUsers(query, null);

            if (users.isEmpty()) {
                return null;
            }
            return users.get(0);
        } catch (ClientException e) {
            String message = "Error while searching user [" + userInfo.getUserName() + "] in UserManager";
            LOGGER.error(message, e);
            throw new RuntimeException(message);
        }
    }

    private DocumentModel createUser(KeycloakUserInfo userInfo) {
        DocumentModel userDoc;
        try {
            userDoc = userManager.getBareUserModel();
            userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
            userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
            userManager.createUser(userDoc);
        } catch (ClientException e) {
            String message = "Error while creating user [" + userInfo.getUserName() + "] in UserManager";
            LOGGER.error(message, e);
            throw new RuntimeException(message);
        }
        return userDoc;
    }

    private DocumentModel updateUser(DocumentModel userDoc, KeycloakUserInfo userInfo) {
        userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
        userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
        userDoc.setProperty(USER_SCHEMANAME, "firstName", userInfo.getFirstName());
        userDoc.setProperty(USER_SCHEMANAME, "lastName", userInfo.getLastName());
        userDoc.setProperty(USER_SCHEMANAME, "password", userInfo.getPassword());
        userDoc.setProperty(USER_SCHEMANAME, "company", userInfo.getCompany());
        userManager.updateUser(userDoc);
        return userDoc;
    }
}