/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public abstract class DirectoryBasedEditor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected DocumentModelList entries;

    protected DocumentModel editableEntry;

    protected DocumentModel creationEntry;

    protected abstract String getDirectoryName();

    protected abstract String getSchemaName();

    protected boolean showAddForm = false;

    @In(create = true)
    protected transient CoreSession documentManager;

    public boolean getShowAddForm() {
        return showAddForm;
    }

    public void toggleShowAddForm() {
        showAddForm = !showAddForm;
    }

    public DocumentModel getCreationEntry() throws PropertyException {
        if (creationEntry == null) {
            creationEntry = BaseSession.createEntryModel(null, getSchemaName(), null, null);
        }
        return creationEntry;
    }

    public void refresh() {
        entries = null;
    }

    public void createEntry() {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = ds.open(getDirectoryName())) {
                session.createEntry(creationEntry);
                creationEntry = null;
                showAddForm = false;
                entries = null;
            }
        });
    }

    public void resetCreateEntry() {
        creationEntry = null;
        showAddForm = false;
    }

    public void resetEditEntry() {
        editableEntry = null;
        showAddForm = false;
    }

    public DocumentModel getEditableEntry() {
        return editableEntry;
    }

    protected Map<String, Serializable> getQueryFilter() {
        return Collections.emptyMap();
    }

    protected Set<String> getOrderSet() {
        return Collections.emptySet();
    }

    public DocumentModelList getEntries() {
        if (entries == null) {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Framework.doPrivileged(() -> {
                try (Session session = ds.open(getDirectoryName())) {
                    Map<String, Serializable> emptyMap = getQueryFilter();
                    Set<String> emptySet = getOrderSet();
                    entries = session.query(emptyMap, emptySet, null, true);
                }
            });
        }
        return entries;
    }

    public void editEntry(String entryId) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = ds.open(getDirectoryName())) {
                editableEntry = session.getEntry(entryId);
            }
        });
    }

    public void saveEntry() {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session directorySession = ds.open(getDirectoryName())) {
                UnrestrictedSessionRunner sessionRunner = new UnrestrictedSessionRunner(documentManager) {
                    @Override
                    public void run() {
                        directorySession.updateEntry(editableEntry);
                    }
                };
                sessionRunner.runUnrestricted();
                editableEntry = null;
                entries = null;
            }
        });
    }

    public void deleteEntry(String entryId) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session directorySession = ds.open(getDirectoryName())) {
                UnrestrictedSessionRunner sessionRunner = new UnrestrictedSessionRunner(documentManager) {
                    @Override
                    public void run() {
                        directorySession.deleteEntry(entryId);
                    }
                };
                sessionRunner.runUnrestricted();
                if (editableEntry != null && editableEntry.getId().equals(entryId)) {
                    editableEntry = null;
                }
                entries = null;
            }
        });
    }
}
