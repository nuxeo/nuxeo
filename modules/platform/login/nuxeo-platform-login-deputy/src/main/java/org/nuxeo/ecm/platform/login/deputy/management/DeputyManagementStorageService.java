/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.login.deputy.management;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class DeputyManagementStorageService implements DeputyManager {

    private static final String DIR_NAME = "deputies";

    private static final String DIR_COL_ID = "id";

    private static final String DIR_COL_USERID = "userid";

    private static final String DIR_COL_DEPUTY = "deputy";

    private static final String DIR_COL_VALIDATE_DATE = "validateDate";

    private static final String DIR_COL_START_VALIDITY = "validityStartDate";

    private static final String DIR_COL_END_VALIDITY = "validityEndDate";

    private DirectoryService directoryService;

    private Session dirSession;

    private String directorySchema;

    @Override
    public String getDeputySchemaName() {
        return directorySchema;
    }

    protected void initPersistentService() {
        if (directoryService == null) {
            directoryService = Framework.getService(DirectoryService.class);
        }
        dirSession = directoryService.open(DIR_NAME);
        directorySchema = directoryService.getDirectorySchema(DIR_NAME);
    }

    private void releasePersistenceService() {
        // for now directory sessions are lost during passivation of the
        // DirectoryFacade
        // this can't be tested on the client side
        // => release directorySession after each call ...

        if (directoryService == null) {
            dirSession = null;
            return;
        }
        if (dirSession != null) {
            try {
                dirSession.close();
            } catch (DirectoryException e) {
                // do nothing
            }
        }
        dirSession = null;
    }

    public void resetDeputies() {
        initPersistentService();

        try {
            DocumentModelList allEntries = dirSession.getEntries();
            List<String> ids = new ArrayList<>();
            for (DocumentModel entry : allEntries) {
                ids.add(entry.getId());
            }
            for (String id : ids) {
                dirSession.deleteEntry(id);
            }

        } finally {
            releasePersistenceService();
        }

    }

    @Override
    public List<String> getPossiblesAlternateLogins(String userName) {
        List<String> users = new ArrayList<>();
        List<String> outdatedEntriesId = new ArrayList<>();

        initPersistentService();

        try {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(DIR_COL_DEPUTY, userName);

            DocumentModelList entries = null;

            entries = dirSession.query(filter);

            long currentTime = System.currentTimeMillis();
            for (DocumentModel entry : entries) {

                String alternateId = (String) entry.getProperty(directorySchema, DIR_COL_USERID);
                Calendar startDate = (Calendar) entry.getProperty(directorySchema, DIR_COL_START_VALIDITY);
                Calendar endDate = (Calendar) entry.getProperty(directorySchema, DIR_COL_END_VALIDITY);

                boolean validateDate = (Boolean) entry.getProperty(directorySchema, DIR_COL_VALIDATE_DATE);
                boolean valid = true;
                if (validateDate && (startDate != null) && (startDate.getTimeInMillis() > currentTime)) {
                    valid = false;
                }
                if (validateDate && (endDate != null) && (endDate.getTimeInMillis() < currentTime)) {
                    valid = false;
                }

                if (valid) {
                    users.add(alternateId);
                } else {
                    outdatedEntriesId.add(entry.getId());
                }
            }

            for (String outDatedId : outdatedEntriesId) {
                dirSession.deleteEntry(outDatedId);
            }

            return users;
        } finally {

            releasePersistenceService();
        }
    }

    @Override
    public List<String> getAvalaibleDeputyIds(String userName) {
        List<String> deputies = new ArrayList<>();

        for (DocumentModel entry : getAvalaibleMandates(userName)) {
            String alternateId = (String) entry.getProperty(directorySchema, DIR_COL_DEPUTY);
            deputies.add(alternateId);
        }

        return deputies;
    }

    @Override
    public List<DocumentModel> getAvalaibleMandates(String userName) {
        List<DocumentModel> deputies = new ArrayList<>();

        initPersistentService();

        try {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(DIR_COL_USERID, userName);
            return dirSession.query(filter);
        } catch (DirectoryException e) {
            return deputies;
        } finally {
            releasePersistenceService();
        }

    }

    @Override
    public DocumentModel newMandate(String username, String deputy) {

        initPersistentService();

        try {
            DocumentModel entry = newEntry(username, deputy);
            Calendar cal = Calendar.getInstance();
            entry.setProperty(directorySchema, DIR_COL_VALIDATE_DATE, Boolean.FALSE);
            entry.setProperty(directorySchema, DIR_COL_START_VALIDITY, cal);
            entry.setProperty(directorySchema, DIR_COL_END_VALIDITY, cal);
            return entry;
        } finally {
            releasePersistenceService();
        }
    }

    protected DocumentModel newEntry(String username, String deputy) {
        DataModel data = new DataModelImpl(directorySchema, new HashMap<String, Object>());
        DocumentModelImpl entry = new DocumentModelImpl(directorySchema, "0", null, null, null,
                new String[] { directorySchema }, null, null, false, null, null, null);
        entry.addDataModel(data);
        entry.setProperty(directorySchema, DIR_COL_ID, id(username, deputy));
        entry.setProperty(directorySchema, DIR_COL_USERID, username);
        entry.setProperty(directorySchema, DIR_COL_DEPUTY, deputy);
        return entry;
    }

    @Override
    public DocumentModel newMandate(String username, String deputy, Calendar start, Calendar end)
            {

        initPersistentService();

        try {
            DocumentModel entry = newEntry(username, deputy);
            entry.setProperty(directorySchema, DIR_COL_VALIDATE_DATE, Boolean.TRUE);
            entry.setProperty(directorySchema, DIR_COL_START_VALIDITY, start);
            entry.setProperty(directorySchema, DIR_COL_END_VALIDITY, end);
            return entry;
        } finally {
            releasePersistenceService();
        }
    }

    @Override
    public void addMandate(DocumentModel entry) {

        initPersistentService();

        try {
            String id = id(entry);

            if (dirSession.getEntry(id) != null) {
                // first remove entry
                dirSession.deleteEntry(id);
            }

            entry.setProperty(directorySchema, DIR_COL_ID, id);

            dirSession.createEntry(entry);

        } catch (DirectoryException e) {
            throw new NuxeoException(e);
        } finally {
            releasePersistenceService();
        }
        return;
    }

    @Override
    public void removeMandate(String username, String deputy) {

        initPersistentService();

        try {
            String id = id(username, deputy);
            dirSession.deleteEntry(id);
        } finally {
            releasePersistenceService();
        }
    }

    protected String id(DocumentModel entry) {
        return id((String) entry.getProperty(directorySchema, "userid"),
                (String) entry.getProperty(directorySchema, "deputy"));
    }

    protected String id(String username, String deputy) {
        return username + ":" + deputy;
    }

}
