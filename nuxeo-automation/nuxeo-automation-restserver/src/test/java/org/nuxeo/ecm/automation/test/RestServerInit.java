/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import java.util.Arrays;

import org.nuxeo.ecm.automation.test.adapters.BusinessBeanAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

/**
 * Repo init to test Rest API
 *
 *
 * @since 5.7.2
 */
public class RestServerInit implements RepositoryInit {

    /**
     *
     */
    private static final String POWER_USER_LOGIN = "user0";

    public static final String[] FIRSTNAMES = { "Steve", "John", "Georges",
            "Bill" };

    public static final String[] LASTNAMES = { "Jobs", "Lennon", "Harrisson",
            "Gates" };

    public static final String[] GROUPNAMES = { "Stark", "Lannister",
            "Targaryen", "Greyjoy" };

    @Override
    public void populate(CoreSession session) throws ClientException {
        // Create some docs
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/", "folder_" + i,
                    "Folder");
            doc.setPropertyValue("dc:title", "Folder " + i);
            doc = session.createDocument(doc);
        }

        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/folder_1",
                    "note_" + i, "Note");
            doc.setPropertyValue("dc:title", "Note " + i);

            doc.getAdapter(BusinessBeanAdapter.class).setNote("Note " + i);
            doc = session.createDocument(doc);
        }

        session.save();

        UserManager um = Framework.getLocalService(UserManager.class);
        // Create some users
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

    private void createGroup(UserManager um, String groupId, String groupLabel)
            throws ClientException, GroupAlreadyExistsException {
        NuxeoGroup group = um.getGroup(groupId);
        if (group == null) {

            DocumentModel groupModel = um.getBareGroupModel();
            String schemaName = um.getGroupSchemaName();
            groupModel.setProperty(schemaName, "groupname", groupId);
            groupModel.setProperty(schemaName, "grouplabel", groupLabel);
            groupModel = um.createGroup(groupModel);
        }
    }

    public static DocumentModel getFolder(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_" + index));
    }

    public static DocumentModel getNote(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_1/note_" + index));
    }

    public static NuxeoPrincipal getPowerUser() throws ClientException {
        UserManager um = Framework.getLocalService(UserManager.class);
        return um.getPrincipal(POWER_USER_LOGIN);
    }

}
