/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.search.test;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.search.core.SavedSearch;
import org.nuxeo.ecm.platform.search.core.SavedSearchService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.3
 */
public class RestServerInit implements RepositoryInit {

    /** @since 11.1 */
    public static final String USER_1 = "user1";

    protected static final int MAX_FILE = 5;

    @Override
    public void populate(CoreSession session) {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        if (!jsonFactoryManager.isStackDisplay()) {
            jsonFactoryManager.toggleStackDisplay();
        }
        // try to prevent NXP-15404
        // clearRepositoryCaches(session.getRepositoryName());
        // Create some docs
        for (int i = 0; i < MAX_FILE; i++) {
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
            doc = session.createDocument(doc);
        }

        for (int i = 0; i < MAX_FILE; i++) {
            DocumentModel doc = session.createDocumentModel("/folder_1", "note_" + i, "Note");
            doc.setPropertyValue("dc:title", "Note " + i);
            doc.setPropertyValue("dc:source", "Source" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i % 2);
            doc.setPropertyValue("dc:coverage", "Coverage" + i % 3);
            doc.setPropertyValue("note:note", "Note " + i);
            doc = session.createDocument(doc);
        }

        // Create a file
        DocumentModel doc = session.createDocumentModel("/folder_2", "file", "File");
        doc.setPropertyValue("dc:title", "File");
        session.createDocument(doc);

        // Create some saved searches
        SavedSearchService savedSearchService = Framework.getService(SavedSearchService.class);
        SavedSearch search;
        try {
            search = savedSearchService.createSavedSearch(session, "my saved search 1", "$currentUser", null,
                    "select * from Document where dc:creator = ?", "NXQL", null, 2L, null, null, null, null, null);
            savedSearchService.saveSavedSearch(session, search);
        } catch (IOException e) {
        }

        try {
            search = savedSearchService.createSavedSearch(session, "my saved search 2",
                    RestServerInit.getFolder(1, session).getId(), null, null, null, "TEST_PP", null, null, null, null,
                    null, null);
            savedSearchService.saveSavedSearch(session, search);
        } catch (IOException e) {
        }

        try {
            search = savedSearchService.createSavedSearch(session, "my saved search 3", null, null, null, null,
                    "default_search", 2L, null, null, null, null, null);
            DocumentHelper.setProperty(session, search.getDocument(), "ecm_fulltext", "Note*");
            DocumentHelper.setProperty(session, search.getDocument(), "dc_modified_agg", "[\"lastWeek\"]");
            savedSearchService.saveSavedSearch(session, search);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        UserManager um = Framework.getService(UserManager.class);
        // Create some user1
        if (um != null) {
            Framework.doPrivileged(() -> createUser(um));
        }
    }

    public static DocumentModel getFolder(int index, CoreSession session) {
        return session.getDocument(new PathRef("/folder_" + index));
    }

    public static String getSavedSearchId(int index, CoreSession session) {
        UserWorkspaceService userWorkspaceService = Framework.getService(UserWorkspaceService.class);
        DocumentModel uws = userWorkspaceService.getCurrentUserPersonalWorkspace(session, null);
        return session.getDocument(new PathRef(uws.getPathAsString() + "/my saved search " + index)).getId();
    }

    protected void createUser(UserManager userManager) {
        NuxeoPrincipal principal = userManager.getPrincipal(USER_1);
        if (principal != null) {
            userManager.deleteUser(principal.getModel());
        }

        DocumentModel userModel = userManager.getBareUserModel();
        String schemaName = userManager.getUserSchemaName();
        userModel.setProperty(schemaName, "username", USER_1);
        userModel.setProperty(schemaName, "firstName", USER_1);
        userModel.setProperty(schemaName, "lastName", USER_1);
        userModel.setProperty(schemaName, "password", USER_1);
        userManager.createUser(userModel);
        principal = userManager.getPrincipal(USER_1);
        principal.setGroups(List.of("members"));
        userManager.updateUser(principal.getModel());
    }

}
