/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.dam.platform.context;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.dam.core.Constants;
import org.nuxeo.dam.webapp.filter.FilterActions;
import org.nuxeo.dam.webapp.helper.DamEventNames;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

@Name("importActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ImportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(ImportActions.class);

    public static final String IMPORT_ROOT_PATH = Framework.getProperty(
            "import.root.path", "/default-domain/import-root");

    protected DocumentModel newImportSet;

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true, required = false)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected Context eventContext;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true)
    protected transient FilterActions filterActions;

    protected FileManager fileManagerService;

    protected Blob blob;

    protected String importFolderId;

    protected FileManager getFileManagerService() throws Exception {
        if (fileManagerService == null) {
            fileManagerService = Framework.getService(FileManager.class);
        }
        return fileManagerService;
    }

    public DocumentModel getNewImportSet() throws ClientException {
        if (newImportSet == null) {
            newImportSet = documentManager.createDocumentModel(Constants.IMPORT_SET_TYPE);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "yyyyMMdd HH:mm");
            Calendar calendar = Calendar.getInstance();

            String fullName;
            if (currentNuxeoPrincipal != null) {
                fullName = Functions.principalFullName(currentNuxeoPrincipal);
            } else {
                fullName = Functions.principalFullName((NuxeoPrincipal) documentManager.getPrincipal());
            }

            String defaultTitle = fullName + " - "
                    + simpleDateFormat.format(calendar.getTime());
            newImportSet.setPropertyValue("dc:title", defaultTitle);
        }

        return newImportSet;
    }

    protected DocumentModel createContainerFolder(String title)
            throws ClientException {
        DocumentModel folder = documentManager.createDocumentModel(
                IMPORT_ROOT_PATH, IdUtils.generateId(title),
                Constants.IMPORT_FOLDER_TYPE);
        folder.setPropertyValue("dc:title", title);
        folder = documentManager.createDocument(folder);
        documentManager.save();
        Events.instance().raiseEvent(DamEventNames.FOLDERLIST_CHANGED);
        return folder;
    }

    public String createImportSet() throws Exception {
        String title = (String) newImportSet.getProperty("dublincore", "title");
        if (title == null) {
            title = "";
        }
        DocumentModel importFolder;
        if (importFolderId == null) {
            importFolder = createContainerFolder(title);
            importFolderId = importFolder.getId();
        } else {
            importFolder = documentManager.getDocument(new IdRef(importFolderId));
        }
        String name = IdUtils.generateId(title);
        // set parent path and name for document model
        newImportSet.setPathInfo(importFolder.getPathAsString(), name);
        try {
            newImportSet = documentManager.createDocument(newImportSet);
            if (blob != null) {
                getFileManagerService().createDocumentFromBlob(documentManager,
                        blob, newImportSet.getPathAsString(), true,
                        blob.getFilename());
            }
        } catch (Exception e) {
            log.error(e, e);
        } finally {
            // delete the temporary file that was made by the richfaces
            if (blob != null) {
                ((FileBlob) blob).getFile().delete();
            }
        }
        documentManager.save();
        sendImportSetCreationEvent();
        invalidateImportContext();

        // CB: DAM-392 - Create new filter widget for Importset -> When user
        // finishes an import (and gets back the focus), he must see only his
        // importset assets - his last import will be selected by default in the
        // filter.
        if (filterActions != null) {
            List<SelectItem> userImportSetsSelectItems = filterActions.getUserImportSetsSelectItems();
            if (userImportSetsSelectItems != null
                    && !userImportSetsSelectItems.isEmpty()) {
                String folderPath = (String) userImportSetsSelectItems.get(0).getValue();
                DocumentModel filterDocument = filterActions.getFilterDocument();
                if (filterDocument != null) {
                    filterDocument.setPropertyValue(
                            FilterActions.PATH_FIELD_XPATH, folderPath);
                }
            }
        }

        return "nxstartup";
    }

    protected void sendImportSetCreationEvent() {
        Events.instance().raiseEvent(DamEventNames.IMPORTSET_CREATED);

        logDocumentWithTitle("document_saved", "Created the document: ",
                newImportSet);
    }

    public void uploadListener(UploadEvent event) throws Exception {
        UploadItem item = event.getUploadItem();
        blob = new FileBlob(item.getFile());
        blob.setFilename(item.getFileName());
    }

    public String cancel() {
        invalidateImportContext();
        return "nxstartup";
    }

    public void invalidateImportContext() {
        newImportSet = null;
    }

    /**
     * Logs a {@link DocumentModel} title and the passed string (info).
     */
    public void logDocumentWithTitle(String facesMessage, String someLogString,
            DocumentModel document) {

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(facesMessage),
                resourcesAccessor.getMessages().get(newImportSet.getType()));

        if (null != document) {
            log.trace('[' + getClass().getSimpleName() + "] " + someLogString
                    + ' ' + document.getId());
            log.debug("CURRENT DOC PATH: " + document.getPathAsString());
        } else {
            log.trace('[' + getClass().getSimpleName() + "] " + someLogString
                    + " NULL DOC");
        }
    }

    public List<SelectItem> getImportFolders() throws ClientException {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (documentManager.hasPermission(new PathRef(IMPORT_ROOT_PATH),
                SecurityConstants.ADD_CHILDREN)) {
            items.add(new SelectItem(null, resourcesAccessor.getMessages().get(
                    "label.widget.newFolder")));
        }
        DocumentModelList docs = queryModelActions.get("IMPORT_FOLDERS").getDocuments(
                documentManager);
        for (DocumentModel doc : docs) {
            if (documentManager.hasPermission(doc.getRef(),
                    SecurityConstants.ADD_CHILDREN)) {
                items.add(new SelectItem(doc.getId(), doc.getTitle()));
            }
        }
        return items;
    }

    public String getImportFolder() {
        return importFolderId;
    }

    public void setImportFolder(String importFolder) {
        this.importFolderId = importFolder;
    }

}
