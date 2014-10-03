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
package org.nuxeo.ecm.restapi.test;

import java.io.File;
import java.util.Arrays;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

/**
 * Repo init to test Rest API
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
        // try to prevent NXP-15404
        clearRepositoryCaches(session.getRepositoryName());
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
            doc.setPropertyValue("dc:source", "Source" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i % 2);
            doc.setPropertyValue("dc:coverage", "Coverage" + i % 3);
            doc.setPropertyValue("note:note", "Note " + i);
            doc = session.createDocument(doc);
        }

        // Create a file
        DocumentModel doc = session.createDocumentModel("/folder_2",
                "file", "File");
        doc.setPropertyValue("dc:title", "File");
        doc = session.createDocument(doc);
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("blob.json");
        FileBlob fb = new FileBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        session.saveDocument(doc);

        session.save();

        UserManager um = Framework.getLocalService(UserManager.class);
        // Create some users
        if (um != null) {
            createUsersAndGroups(um);
        }

    }

    private void clearRepositoryCaches(String repositoryName) {
        SQLRepositoryService repoService = Framework.getService(SQLRepositoryService.class);
        RepositoryManagement repo = repoService.getRepository(repositoryName);
        repo.clearCaches();
    }

    private void createUsersAndGroups(UserManager um) throws ClientException,
            UserAlreadyExistsException, GroupAlreadyExistsException {
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
        if (group != null) {
            um.deleteGroup(groupId);
        }

        DocumentModel groupModel = um.getBareGroupModel();
        String schemaName = um.getGroupSchemaName();
        groupModel.setProperty(schemaName, "groupname", groupId);
        groupModel.setProperty(schemaName, "grouplabel", groupLabel);
        groupModel = um.createGroup(groupModel);
    }

    public static DocumentModel getFolder(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_" + index));
    }

    public static DocumentModel getNote(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_1/note_" + index));
    }

    public static DocumentModel getFile(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_2/file"));
    }

    public static NuxeoPrincipal getPowerUser() throws ClientException {
        UserManager um = Framework.getLocalService(UserManager.class);
        return um.getPrincipal(POWER_USER_LOGIN);
    }

}
