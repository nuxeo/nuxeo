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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.select2;

import java.io.Serializable;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Helper class for directory select2 widgets.
 *
 * @since 5.9.1
 */
@Name("select2DirectoryActions")
@Scope(ScopeType.PAGE)
public class Select2DirectoryActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected DirectoryUIManager directoryUIManager;

    protected String directoryName;

    protected DocumentModel newDirectoryEntry;

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public DocumentModel getCreationDirectoryEntry(String directoryName) {
        if (newDirectoryEntry == null || (directoryName != null && !directoryName.equals(getDirectoryName()))) {
            setDirectoryName(directoryName);
            DirectoryService dirService = DirectoryHelper.getDirectoryService();
            String schema = dirService.getDirectorySchema(directoryName);
            newDirectoryEntry = BaseSession.createEntryModel(null, schema, null, null);
        }
        return newDirectoryEntry;
    }

    public String getCreationDirectoryEntryLayout(String directoryName) {
        DirectoryUI currentDirectoryInfo = directoryUIManager.getDirectoryInfo(directoryName);
        if (currentDirectoryInfo != null) {
            return currentDirectoryInfo.getLayout();
        }
        return null;
    }

    public void createDirectoryEntry() {
        DirectoryService dirService = DirectoryHelper.getDirectoryService();
        String dirName = getDirectoryName();
        try (Session dirSession = dirService.open(dirName)) {
            // check if entry already exists
            String schema = dirService.getDirectorySchema(dirName);
            String idField = dirService.getDirectoryIdField(dirName);
            Object id = newDirectoryEntry.getProperty(schema, idField);
            if (id instanceof String && dirSession.hasEntry((String) id)) {
                facesMessages.addToControl("suggestAddNewDirectoryEntry", StatusMessage.Severity.ERROR,
                        messages.get("vocabulary.entry.identifier.already.exists"));
                return;
            }
            dirSession.createEntry(newDirectoryEntry);

            reset();
            Events.instance().raiseEvent(EventNames.DIRECTORY_CHANGED, dirName);

            facesMessages.add(StatusMessage.Severity.INFO, messages.get("vocabulary.entry.added"));
        }
    }

    public void cancelCreateDirectoryEntry() {
        reset();
    }

    public void reset() {
        directoryName = null;
        newDirectoryEntry = null;
    }

}
