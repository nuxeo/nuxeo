/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Anahide Tchertchian <at@nuxeo.com>
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.webapp.directory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIDeleteConstraint;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Manages directories editable by administrators.
 *
 * @author Anahide Tchertchian
 * @author Anahide Tchertchian
 */
@Name("directoryUIActions")
@Scope(ScopeType.CONVERSATION)
public class DirectoryUIActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DIRECTORY_DEFAULT_VIEW = "view_directory";

    @In(create = true)
    protected transient DirectoryUIManager directoryUIManager;

    // FIXME: use a business delegate
    protected transient DirectoryService dirService;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected List<String> directoryNames;

    protected DirectoryUI currentDirectoryInfo;

    protected DocumentModelList currentDirectoryEntries;

    protected DocumentModel selectedDirectoryEntry;

    protected boolean showAddForm = false;

    protected DocumentModel creationDirectoryEntry;

    protected String selectedDirectoryName;

    @Begin(join = true)
    @Create
    public void initialize() {
        initDirService();
    }

    private void initDirService() {
        if (dirService == null) {
            dirService = DirectoryHelper.getDirectoryService();
        }
    }

    public List<String> getDirectoryNames() throws ClientException {
        if (directoryNames == null) {
            directoryNames = directoryUIManager.getDirectoryNames();
            if (directoryNames.size() > 0) {
                selectedDirectoryName = directoryNames.get(0);
                selectDirectory();
            }
        }
        return directoryNames;
    }

    public String getSelectedDirectoryName() {
        return selectedDirectoryName;
    }

    public void setSelectedDirectoryName(String selectedDirectoryName) {
        this.selectedDirectoryName = selectedDirectoryName;
    }

    @Deprecated
    public String selectDirectory(String directoryName) throws ClientException {
        resetSelectedDirectoryData();
        currentDirectoryInfo = directoryUIManager.getDirectoryInfo(directoryName);
        String view = currentDirectoryInfo.getView();
        if (view == null) {
            view = DIRECTORY_DEFAULT_VIEW;
        }
        return view;
    }

    public void selectDirectory() throws ClientException {
        resetSelectedDirectoryData();
        currentDirectoryInfo = directoryUIManager.getDirectoryInfo(selectedDirectoryName);
    }

    public DirectoryUI getCurrentDirectory() throws ClientException {
        return currentDirectoryInfo;
    }

    public DocumentModelList getCurrentDirectoryEntries()
            throws ClientException {
        if (currentDirectoryEntries == null) {
            currentDirectoryEntries = new DocumentModelListImpl();
            Session dirSession = null;
            try {
                String dirName = currentDirectoryInfo.getName();
                dirSession = dirService.open(dirName);
                Map<String, Serializable> emptyMap = Collections.emptyMap();
                Set<String> emptySet = Collections.emptySet();
                DocumentModelList entries = dirSession.query(emptyMap,
                        emptySet, null, true);
                if (entries != null && !entries.isEmpty()) {
                    currentDirectoryEntries.addAll(entries);
                }
                // sort
                String sortField = currentDirectoryInfo.getSortField();
                if (sortField == null) {
                    sortField = dirService.getDirectoryIdField(dirName);
                }
                // sort
                Map<String, String> orderBy = new HashMap<String, String>();
                orderBy.put(sortField, DocumentModelComparator.ORDER_ASC);
                Collections.sort(
                        currentDirectoryEntries,
                        new DocumentModelComparator(
                                dirService.getDirectorySchema(dirName), orderBy));
            } catch (DirectoryException e) {
                throw new ClientException(e);
            } finally {
                if (dirSession != null) {
                    dirSession.close();
                }
            }
        }
        return currentDirectoryEntries;
    }

    public void resetSelectedDirectoryData() {
        currentDirectoryInfo = null;
        currentDirectoryEntries = null;
        selectedDirectoryEntry = null;
        showAddForm = false;
        creationDirectoryEntry = null;
    }

    public boolean getShowAddForm() {
        return showAddForm;
    }

    public void toggleShowAddForm() {
        showAddForm = !showAddForm;
    }

    public DocumentModel getCreationDirectoryEntry() throws ClientException {
        if (creationDirectoryEntry == null) {
            Session dirSession = null;
            try {
                String dirName = currentDirectoryInfo.getName();
                String schema = dirService.getDirectorySchema(dirName);
                creationDirectoryEntry = BaseSession.createEntryModel(null,
                        schema, null, null);
            } catch (DirectoryException e) {
                throw new ClientException(e);
            } finally {
                if (dirSession != null) {
                    dirSession.close();
                }
            }
        }
        return creationDirectoryEntry;
    }

    public void createDirectoryEntry() throws ClientException {
        Session dirSession = null;
        try {
            String dirName = currentDirectoryInfo.getName();
            // check if entry already exists
            String schema = dirService.getDirectorySchema(dirName);
            String idField = dirService.getDirectoryIdField(dirName);
            Object id = creationDirectoryEntry.getProperty(schema, idField);
            dirSession = dirService.open(dirName);
            if (id instanceof String && dirSession.hasEntry((String) id)) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "vocabulary.entry.identifier.already.exists"));
                return;
            }
            dirSession.createEntry(creationDirectoryEntry);

            resetCreateDirectoryEntry();
            // invalidate directory entries list
            currentDirectoryEntries = null;

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "vocabulary.entry.added"));
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

    public void resetCreateDirectoryEntry() {
        creationDirectoryEntry = null;
        showAddForm = false;
    }

    public void selectDirectoryEntry(String entryId) throws ClientException {
        Session dirSession = null;
        try {
            String dirName = currentDirectoryInfo.getName();
            dirSession = dirService.open(dirName);
            selectedDirectoryEntry = dirSession.getEntry(entryId);
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

    public DocumentModel getSelectedDirectoryEntry() throws ClientException {
        return selectedDirectoryEntry;
    }

    public void resetSelectedDirectoryEntry() {
        selectedDirectoryEntry = null;
    }

    public void editSelectedDirectoryEntry() throws ClientException {
        Session dirSession = null;
        try {
            String dirName = currentDirectoryInfo.getName();
            dirSession = dirService.open(dirName);
            dirSession.updateEntry(selectedDirectoryEntry);
            selectedDirectoryEntry = null;
            // invalidate directory entries list
            currentDirectoryEntries = null;

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "vocabulary.entry.edited"));
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

    public void deleteDirectoryEntry(String entryId) throws ClientException {
        String dirName = currentDirectoryInfo.getName();
        List<DirectoryUIDeleteConstraint> deleteConstraints = currentDirectoryInfo.getDeleteConstraints();
        if (deleteConstraints != null && !deleteConstraints.isEmpty()) {
            for (DirectoryUIDeleteConstraint deleteConstraint : deleteConstraints) {
                if (!deleteConstraint.canDelete(dirService, entryId)) {
                    facesMessages.add(
                            FacesMessage.SEVERITY_ERROR,
                            resourcesAccessor.getMessages().get(
                                    "feedback.directory.deleteEntry.constraintError"));
                    return;
                }
            }
        }
        Session dirSession = null;
        try {
            dirSession = dirService.open(dirName);
            dirSession.deleteEntry(entryId);
            // invalidate directory entries list
            currentDirectoryEntries = null;
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "vocabulary.entry.deleted"));
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

    public boolean isReadOnly() throws ClientException {

        Session dirSession = null;
        boolean isReadOnly = false;

        try {
            String dirName = currentDirectoryInfo.getName();
            dirSession = dirService.open(dirName);

            // Check Directory ReadOnly Status
            boolean dirReadOnly = dirSession.isReadOnly();

            // Check DirectoryUI ReadOnly Status
            boolean dirUIReadOnly = currentDirectoryInfo.isReadOnly() == null ? false
                    : currentDirectoryInfo.isReadOnly();

            isReadOnly = dirReadOnly || dirUIReadOnly;
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {

            if (dirSession != null) {
                dirSession.close();
            }

        }

        return isReadOnly;

    }
}
