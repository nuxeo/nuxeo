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

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
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
import org.nuxeo.common.utils.Path;
import org.nuxeo.dam.Constants;
import org.nuxeo.dam.DamService;
import org.nuxeo.dam.importer.core.DamImporterExecutor;
import org.nuxeo.dam.importer.core.MetadataFileHelper;
import org.nuxeo.dam.importer.core.helper.UnrestrictedSessionRunnerHelper;
import org.nuxeo.dam.webapp.chainselect.ChainSelectCleaner;
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
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.importer.properties.MetadataFile;
import org.nuxeo.ecm.platform.importer.source.FileWithMetadataSourceNode;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import de.schlichtherle.io.File;

@Name("importActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ImportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ASYNCHRONOUS_IMPORT_PROPERTY = "org.nuxeo.dam.import.async";

    protected static final Log log = LogFactory.getLog(ImportActions.class);

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

    protected Blob blob;

    protected String importFolderId;

    protected String newImportFolder;

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
            newImportSet.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                    defaultTitle);
        }

        return newImportSet;
    }

    protected DocumentModel createContainerFolder(String title)
            throws ClientException {
        // Create the folder in another thread where we can easily start and
        // stop a new transaction
        // to avoid locking tables in h2
        ContainerFolderCreator cfc = new ContainerFolderCreator(
                documentManager.getRepositoryName(), title);
        UnrestrictedSessionRunnerHelper.runInNewThread(cfc);
        Events.instance().raiseEvent(DamEventNames.FOLDERLIST_CHANGED);
        return documentManager.getDocument(cfc.folder.getRef());

    }

    public void createImportSet() throws Exception {
        String title = (String) newImportSet.getProperty("dublincore", "title");
        if (title == null) {
            title = "";
        }

        String tmpDirectory = System.getProperty("java.io.tmpdir");
        Path tmpPath = new Path(tmpDirectory).append("import_"
                + System.nanoTime());
        File outDir = new File(tmpPath.toString());
        outDir.mkdirs();

        MetadataFile mdFile = MetadataFileHelper.createFrom(newImportSet);
        String principalName = documentManager.getPrincipal().getName();
        mdFile.addProperty(Constants.DUBLINCORE_CREATOR_PROPERTY, principalName);
        mdFile.addProperty(Constants.DUBLINCORE_CONTRIBUTORS_PROPERTY,
                new String[] { principalName });
        mdFile.writeTo(new File(outDir,
                FileWithMetadataSourceNode.METADATA_FILENAME));

        java.io.File tmp = null;
        try {
            String extension = null;
            String filename = blob.getFilename();
            if (filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
            }
            tmp = File.createTempFile("import", extension);
            File archive = new File(tmp);
            blob.transferTo(archive);
            if (archive.isArchive()) {
                archive.archiveCopyAllTo(outDir);
            } else {
                archive.copyTo(new File(outDir, blob.getFilename()));
            }
        } finally {
            // delete the temporary file that was made by the richfaces
            if (blob != null) {
                ((FileBlob) blob).getFile().delete();
            }
            // delete the copied file
            if (tmp != null) {
                tmp.delete();
            }
        }

        DocumentModel importFolder = getOrCreateImportFolder(title);
        boolean interactiveMode = !Boolean.parseBoolean(Framework.getProperty(
                ASYNCHRONOUS_IMPORT_PROPERTY, "false"));
        try {
            DamImporterExecutor importer = new DamImporterExecutor(
                    outDir.getAbsolutePath(), importFolder.getPathAsString(),
                    title, interactiveMode, true);
            importer.run();
        } catch (Exception e) {
            log.error(e, e);
        }

        if (interactiveMode) {
            documentManager.save();
            sendImportSetCreationEvent();
            invalidateImportContext();

            // CB: DAM-392 - Create new filter widget for Importset -> When user
            // finishes an import (and gets back the focus), he must see only
            // his
            // importset assets - his last import will be selected by default in
            // the
            // filter.
            if (filterActions != null) {
                List<SelectItem> userImportSetsSelectItems = filterActions.getUserImportSetsSelectItems();
                if (userImportSetsSelectItems != null
                        && !userImportSetsSelectItems.isEmpty()) {
                    String folderPath = (String) userImportSetsSelectItems.get(
                            0).getValue();
                    DocumentModel filterDocument = filterActions.getFilterDocument();
                    if (filterDocument != null) {
                        filterDocument.setPropertyValue(
                                FilterActions.PATH_FIELD_XPATH, folderPath);
                    }
                }
            }
        }
    }

    protected DocumentModel getOrCreateImportFolder(String title)
            throws ClientException {
        DocumentModel importFolder;
        if (importFolderId == null) {
            String importFolderTitle = newImportFolder != null
                    && newImportFolder.trim().length() > 0 ? newImportFolder
                    : title;
            importFolder = createContainerFolder(importFolderTitle);
            importFolderId = importFolder.getId();
        } else {
            importFolder = documentManager.getDocument(new IdRef(importFolderId));
        }
        return importFolder;
    }

    protected void sendImportSetCreationEvent() {
        Events.instance().raiseEvent(DamEventNames.IMPORTSET_CREATED);

        logDocumentWithTitle("document_saved", "Created the document: ",
                newImportSet);
    }

    public void uploadListener(UploadEvent event) throws Exception {
        UploadItem item = event.getUploadItem();
        blob = new FileBlob(item.getFile());
        // Retrieve only the real filename
        // IE stores the full path of the file as the filename (ie. Z:\\path\\to\\file)
        blob.setFilename(FilenameUtils.getName(item.getFileName()));
    }

    public void cancel() {
        invalidateImportContext();
    }

    public void invalidateImportContext() {
        newImportSet = null;
        newImportFolder = null;
        importFolderId = null;
        ChainSelectCleaner.cleanup(ChainSelectCleaner.IMPORT_COVERAGE_CHAIN_SELECT_ID);
        ChainSelectCleaner.cleanup(ChainSelectCleaner.IMPORT_SUBJECTS_CHAIN_SELECT_ID);
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
        DamService damService = Framework.getLocalService(DamService.class);
        String assetLibraryPath = damService.getAssetLibraryPath();
        if (documentManager.hasPermission(new PathRef(
                assetLibraryPath), SecurityConstants.ADD_CHILDREN)) {
            items.add(new SelectItem(null, resourcesAccessor.getMessages().get(
                    "label.widget.newFolder")));
        }
        DocumentModelList docs = queryModelActions.get("IMPORT_FOLDERS").getDocuments(
                documentManager, new Object[] { assetLibraryPath });
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

    public String getNewImportFolder() {
        return newImportFolder;
    }

    public void setNewImportFolder(String newImportFolder) {
        this.newImportFolder = newImportFolder;
    }

}

/**
 * Create the {@code ImportFolder}, from its title, where the import will be
 * created in an Unrestricted session.
 */
class ContainerFolderCreator extends UnrestrictedSessionRunner {

    private static final Log log = LogFactory.getLog(ContainerFolderCreator.class);

    protected String folderTitle;

    public DocumentModel folder;

    public ContainerFolderCreator(String repositoryName, String folderTitle) {
        super(repositoryName);
        this.folderTitle = folderTitle;
    }

    @Override
    public void run() throws ClientException {
        try {
            DamService damService = Framework.getLocalService(DamService.class);
            folder = session.createDocumentModel(damService.getAssetLibraryPath(),
                    IdUtils.generateId(folderTitle),
                    Constants.IMPORT_FOLDER_TYPE);
            folder.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                    folderTitle);
            folder = session.createDocument(folder);
            session.save();
        } catch (ClientException e) {
            log.error(e, e);
        }
    }

}
