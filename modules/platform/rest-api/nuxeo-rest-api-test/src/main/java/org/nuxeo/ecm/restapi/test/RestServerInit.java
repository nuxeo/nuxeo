/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Repo init to test Rest API
 *
 * @since 5.7.2
 */
public class RestServerInit implements RepositoryInit {

    public static final int MAX_NOTE = 5;

    /**
     *
     */
    private static final String POWER_USER_LOGIN = "user0";

    public static final String[] FIRSTNAMES = { "Steve", "John", "Georges", "Bill", "Bill" };

    public static final String[] LASTNAMES = { "Jobs", "Lennon", "Harrisson", "Gates", "Murray" };

    public static final String[] GROUPNAMES = { "Stark", "Lannister", "Targaryen", "Greyjoy" };

    @Override
    public void populate(CoreSession session) {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        if (!jsonFactoryManager.isStackDisplay()) {
            jsonFactoryManager.toggleStackDisplay();
        }
        // try to prevent NXP-15404
        // clearRepositoryCaches(session.getRepositoryName());
        // Create some docs
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/", "folder_" + i, "Folder");
            doc.setPropertyValue("dc:title", "Folder " + i);
            if (i == 0) {
                // set dc:issued value for queries on dates
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, 2007);
                cal.set(Calendar.MONTH, 1); // 0-based
                cal.set(Calendar.DAY_OF_MONTH, 17);
                doc.setPropertyValue("dc:issued", cal);
            }
            Framework.doPrivileged(() -> session.createDocument(doc));
        }

        for (int i = 0; i < MAX_NOTE; i++) {
            DocumentModel doc = session.createDocumentModel("/folder_1", "note_" + i, "Note");
            doc.setPropertyValue("dc:title", "Note " + i);
            doc.setPropertyValue("dc:source", "Source" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i % 2);
            doc.setPropertyValue("dc:coverage", "Coverage" + i % 3);
            doc.setPropertyValue("note:note", "Note " + i);
            Framework.doPrivileged(() -> session.createDocument(doc));
        }

        // Create a file
        DocumentModel doc = session.createDocumentModel("/folder_2", "file", "File");
        doc.setPropertyValue("dc:title", "File");
        doc = session.createDocument(doc);
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("blob.json");
        try {
            Blob fb = Blobs.createBlob(fieldAsJsonFile, "image/jpeg");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        session.saveDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try {
            Framework.getService(WorkManager.class).awaitCompletion(10, TimeUnit.SECONDS);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(cause);
        }

        UserManager um = Framework.getService(UserManager.class);
        // Create some users
        if (um != null) {
            Framework.doPrivileged(() -> createUsersAndGroups(um));
        }
    }

    private void createUsersAndGroups(UserManager um) throws UserAlreadyExistsException,
            GroupAlreadyExistsException {

        // Create some groups
        for (int idx = 0; idx < 4; idx++) {
            String groupId = "group" + idx;
            String groupLabel = GROUPNAMES[idx];
            createGroup(um, groupId, groupLabel);
        }

        for (int idx = 0; idx < 5; idx++) {
            String userId = "user" + idx;
            String firstName = FIRSTNAMES[idx];
            String lastName = LASTNAMES[idx];
            createUser(um, userId, firstName, lastName);
        }

        // Create the power user group
        createGroup(um, "powerusers", "Power Users");

        // Add the power user group to user0
        NuxeoPrincipal principal = um.getPrincipal(POWER_USER_LOGIN);
        principal.setGroups(Arrays.asList("powerusers"));
        um.updateUser(principal.getModel());

        createGroup(um, "foogroup", "foo group");
        createUser(um, "foouser", "Foo", "Foo");
    }

    private void createGroup(UserManager um, String groupId, String groupLabel) throws
            GroupAlreadyExistsException {
        NuxeoGroup group = um.getGroup(groupId);
        if (group != null) {
            um.deleteGroup(groupId);
        }

        DocumentModel groupModel = um.getBareGroupModel();
        String schemaName = um.getGroupSchemaName();
        groupModel.setProperty(schemaName, "groupname", groupId);
        groupModel.setProperty(schemaName, "grouplabel", groupLabel);
        groupModel.setProperty(schemaName, "description", "description of " + groupId);
        groupModel = um.createGroup(groupModel);
    }

    protected void createUser(UserManager um, String userId, String firstName, String lastName) {
        NuxeoPrincipal principal = um.getPrincipal(userId);

        if (principal != null) {
            um.deleteUser(principal.getModel());
        }

        DocumentModel userModel = um.getBareUserModel();
        String schemaName = um.getUserSchemaName();
        userModel.setProperty(schemaName, "username", userId);
        userModel.setProperty(schemaName, "firstName", firstName);
        userModel.setProperty(schemaName, "lastName", lastName);
        userModel.setProperty(schemaName, "password", userId);
        um.createUser(userModel);
        principal = um.getPrincipal(userId);
        principal.setGroups(Arrays.asList("group1"));
        um.updateUser(principal.getModel());
    }

    public static DocumentModel getFolder(int index, CoreSession session) {
        return session.getDocument(new PathRef("/folder_" + index));
    }

    public static DocumentModel getNote(int index, CoreSession session) {
        return session.getDocument(new PathRef("/folder_1/note_" + index));
    }

    public static DocumentModel getFile(int index, CoreSession session) {
        return session.getDocument(new PathRef("/folder_2/file"));
    }

    public static NuxeoPrincipal getPowerUser() {
        UserManager um = Framework.getService(UserManager.class);
        return um.getPrincipal(POWER_USER_LOGIN);
    }

}
