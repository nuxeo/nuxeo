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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.tests;

import java.util.Arrays;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author tiry
 */
public class ScimServerInit implements RepositoryInit {

    /**
     *
     */
    private static final String POWER_USER_LOGIN = "user0";

    public static final String[] FIRSTNAMES = { "Steve", "John", "Georges", "Bill" };

    public static final String[] LASTNAMES = { "Jobs", "Lennon", "Harrisson", "Gates" };

    public static final String[] GROUPNAMES = { "Stark", "Lannister", "Targaryen", "Greyjoy" };

    @Override
    public void populate(CoreSession session) {

        UserManager um = Framework.getService(UserManager.class);
        // Create some users
        if (um != null) {
            Framework.doPrivileged(() -> createUsersAndGroups(um));
        }
    }

    private void createUsersAndGroups(UserManager um) throws UserAlreadyExistsException,
            GroupAlreadyExistsException {
        for (int idx = 0; idx < 4; idx++) {
            String userId = "user" + idx;

            NuxeoPrincipal principal = um.getPrincipal(userId);

            if (principal != null) {
                um.deleteUser(principal.getModel());
            }

            DocumentModel userModel = um.getBareUserModel();
            String schemaName = um.getUserSchemaName();
            userModel.setProperty(schemaName, "username", userId);
            userModel.setProperty(schemaName, "firstName", FIRSTNAMES[idx]);
            userModel.setProperty(schemaName, "lastName", LASTNAMES[idx]);
            userModel.setProperty(schemaName, "password", userId);
            userModel = um.createUser(userModel);
            principal = um.getPrincipal(userId);

        }

        // Create some groups
        for (int idx = 0; idx < 4; idx++) {
            String groupId = "group" + idx;
            String groupLabel = GROUPNAMES[idx];
            createGroup(um, groupId, groupLabel);
        }

        // Create the power user group
        createGroup(um, "powerusers", "Power Users");

        // Add the power user group to user0
        NuxeoPrincipal principal = um.getPrincipal(POWER_USER_LOGIN);
        principal.setGroups(Arrays.asList(new String[] { "powerusers" }));
        um.updateUser(principal.getModel());
    }

    private void createGroup(UserManager um, String groupId, String groupLabel) throws GroupAlreadyExistsException {
        NuxeoGroup group = um.getGroup(groupId);
        if (group != null) {
            um.deleteGroup(groupId);
        }

        DocumentModel groupModel = um.getBareGroupModel();
        String schemaName = um.getGroupSchemaName();
        groupModel.setProperty(schemaName, "groupname", groupId);
        groupModel.setProperty(schemaName, "grouplabel", groupLabel);
        groupModel = um.createGroup(groupModel);
    }

    public static NuxeoPrincipal getPowerUser() {
        UserManager um = Framework.getService(UserManager.class);
        return um.getPrincipal(POWER_USER_LOGIN);
    }

}
