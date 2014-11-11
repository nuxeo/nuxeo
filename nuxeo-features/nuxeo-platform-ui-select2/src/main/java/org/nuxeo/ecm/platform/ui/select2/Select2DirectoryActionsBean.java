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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
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

    public DocumentModel getCreationDirectoryEntry(String directoryName)
            throws ClientException {
        if (newDirectoryEntry == null
                || (directoryName != null && !directoryName.equals(getDirectoryName()))) {
            setDirectoryName(directoryName);
            DirectoryService dirService = DirectoryHelper.getDirectoryService();
            String schema = dirService.getDirectorySchema(directoryName);
            newDirectoryEntry = BaseSession.createEntryModel(null, schema,
                    null, null);
        }
        return newDirectoryEntry;
    }

    public String getCreationDirectoryEntryLayout(String directoryName)
            throws ClientException {
        DirectoryUI currentDirectoryInfo = directoryUIManager.getDirectoryInfo(directoryName);
        if (currentDirectoryInfo != null) {
            return currentDirectoryInfo.getLayout();
        }
        return null;
    }

    public void createDirectoryEntry() throws ClientException {
        Session dirSession = null;
        try {
            // check if entry already exists
            DirectoryService dirService = DirectoryHelper.getDirectoryService();
            String dirName = getDirectoryName();
            String schema = dirService.getDirectorySchema(dirName);
            String idField = dirService.getDirectoryIdField(dirName);
            Object id = newDirectoryEntry.getProperty(schema, idField);
            dirSession = dirService.open(dirName);
            if (id instanceof String && dirSession.hasEntry((String) id)) {
                facesMessages.addToControl(
                        "suggestAddNewDirectoryEntry",
                        StatusMessage.Severity.ERROR,
                        messages.get("vocabulary.entry.identifier.already.exists"));
                return;
            }
            dirSession.createEntry(newDirectoryEntry);

            reset();
            Events.instance().raiseEvent(EventNames.DIRECTORY_CHANGED, dirName);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("vocabulary.entry.added"));
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

    public void cancelCreateDirectoryEntry() throws ClientException {
        reset();
    }

    public void reset() {
        directoryName = null;
        newDirectoryEntry = null;
    }

}
