/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.action;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.webapp.helpers.EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.FancyNavigationHandler;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webapp.dnd.DndConfigurationHelper;
import org.nuxeo.ecm.webapp.filemanager.FileManageActions;
import org.nuxeo.ecm.webapp.filemanager.NxUploadedFile;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.FileUploadEvent;

/**
 * @since 6.0
 */
@Name("importActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ImportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = getLog(ImportActions.class);

    public static final String LABEL_IMPORT_PROBLEM = "label.bulk.import.documents.error";

    public static final String LABEL_IMPORT_CANNOT_CREATE_ERROR = "label.bulk.import.documents.cannotCreateError";

    public static final String DOCUMENTS_IMPORTED = "documentImported";

    protected static Random random = new Random();

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient DndConfigurationHelper dndConfigHelper;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @RequestParameter
    protected String fancyboxFormId;

    protected DocumentModel importDocumentModel;

    protected Action selectedImportOption;

    protected List<Action> importOptions;

    protected String selectedImportFolderId;

    protected String currentBatchId;

    protected Collection<NxUploadedFile> uploadedFiles = null;

    public DocumentModel getImportDocumentModel() {
        if (importDocumentModel == null) {
            importDocumentModel = new SimpleDocumentModel();
        }
        return importDocumentModel;
    }

    public String getSelectedImportOptionId() {
        if (selectedImportOption == null) {
            selectedImportOption = importOptions != null
                    && importOptions.size() > 0 ? importOptions.get(0) : null;
        }
        return selectedImportOption != null ? selectedImportOption.getId()
                : null;
    }

    public void setSelectedImportOptionId(String id) {
        for (Action importOption : importOptions) {
            if (importOption.getId().equals(id)) {
                selectedImportOption = importOption;
                break;
            }
        }
    }

    public Action getSelectedImportOption() {
        if (selectedImportOption == null) {
            selectedImportOption = importOptions != null
                    && importOptions.size() > 0 ? importOptions.get(0) : null;
        }
        return selectedImportOption;
    }

    public List<Action> getImportOptions(String dropContext) {
        if (importOptions == null) {
            importOptions = new ArrayList<>();
            importOptions.addAll(webActions.getActionsList(dropContext));
        }
        return importOptions;
    }

    public String getSelectedImportFolderId() throws ClientException {
        if (selectedImportFolderId == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument == null) {
                return null;
            }
            if (currentDocument.isFolder()
                    && !"/".equals(currentDocument.getPathAsString())
                    && documentManager.hasPermission(currentDocument.getRef(),
                            SecurityConstants.ADD_CHILDREN)) {
                selectedImportFolderId = currentDocument.getId();
            } else {
                // try to find the first folderish parent
                List<DocumentModel> parents = documentManager.getParentDocuments(currentDocument.getRef());
                Collections.reverse(parents);

                for (DocumentModel parent : parents) {
                    if (parent.isFolder()
                            && documentManager.hasPermission(parent.getRef(),
                                    SecurityConstants.ADD_CHILDREN)) {
                        selectedImportFolderId = parent.getId();
                        break;
                    }
                }
            }
        }
        return selectedImportFolderId;
    }

    public void setSelectedImportFolderId(String selectedImportFolderId) {
        this.selectedImportFolderId = selectedImportFolderId;
    }

    /*
     * ----- Document bulk import -----
     */

    public String generateBatchId() {
        if (currentBatchId == null) {
            currentBatchId = "batch-" + new Date().getTime() + "-"
                    + random.nextInt(1000);
        }
        return currentBatchId;
    }

    public boolean hasUploadedFiles() {
        if (currentBatchId != null) {
            BatchManager batchManager = Framework.getLocalService(BatchManager.class);
            return batchManager.hasBatch(currentBatchId);
        }
        return false;
    }

    public String importDocuments() {
        Map<String, Serializable> importOptionProperties = selectedImportOption.getProperties();
        String chainOrOperationId = null;
        if (importOptionProperties.containsKey("chainId")) {
            chainOrOperationId = (String) importOptionProperties.get("chainId");
        } else if (importOptionProperties.containsKey("operationId")) {
            chainOrOperationId = (String) importOptionProperties.get("operationId");
        } else {
            // fallback on action id
            chainOrOperationId = selectedImportOption.getId();
        }

        List<DataModel> dms = new ArrayList<>();
        for (String schema : importDocumentModel.getSchemas()) {
            dms.add(importDocumentModel.getDataModel(schema));
        }
        DataModelProperties properties = new DataModelProperties(dms, true);

        Map<String, Object> contextParams = new HashMap<>();
        contextParams.put("docMetaData", properties);
        contextParams.put("currentDocument", selectedImportFolderId);

        boolean resetBatchState = true;
        try {
            List<DocumentModel> importedDocuments;
            if (dndConfigHelper.useHtml5DragAndDrop()) {
                importedDocuments = importDocumentsThroughBatchManager(
                        chainOrOperationId, contextParams);

            } else {
                importedDocuments = importDocumentsThroughUploadItems(
                        chainOrOperationId, contextParams);
            }

            Events.instance().raiseEvent(DOCUMENTS_IMPORTED, importedDocuments,
                    importDocumentModel);

            if (selectedImportFolderId != null) {
                return navigationContext.navigateToRef(new IdRef(
                        selectedImportFolderId));
            }
        } catch (ClientException e) {
            log.debug(e, e);
            Throwable t = ExceptionHelper.unwrapException(e);
            // FIXME NXP-XXXXX
            if (t.getMessage().contains("Cannot create")) {
                // do not reset state
                resetBatchState = false;
                FacesMessage message = new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        Messages.instance().get(LABEL_IMPORT_CANNOT_CREATE_ERROR), null);
                FacesContext faces = FacesContext.getCurrentInstance();
                if (fancyboxFormId != null && fancyboxFormId.startsWith(":")) {
                    faces.addMessage(fancyboxFormId.substring(1), message);
                } else {
                    faces.addMessage(fancyboxFormId, message);
                }
                HttpServletRequest httpRequest = (HttpServletRequest) faces.getExternalContext().getRequest();
                // avoid redirect for message to be displayed (and fancybox to
                // be reopened)
                httpRequest.setAttribute(
                        FancyNavigationHandler.DISABLE_REDIRECT_FOR_URL_REWRITE,
                        Boolean.TRUE);
            } else {
                facesMessages.addFromResourceBundle(
                        StatusMessage.Severity.ERROR, Messages.instance().get(LABEL_IMPORT_PROBLEM));
            }
        } finally {
            if (resetBatchState) {
                // reset batch state
                cancel();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> importDocumentsThroughBatchManager(
            String chainOrOperationId, Map<String, Object> contextParams)
            throws ClientException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        return (List<DocumentModel>) bm.execute(currentBatchId,
                chainOrOperationId, documentManager, contextParams, null);
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> importDocumentsThroughUploadItems(
            String chainOrOperationId, Map<String, Object> contextParams)
            throws ClientException {
        if (uploadedFiles == null) {
            return Collections.emptyList();
        }

        try {
            List<Blob> blobs = new ArrayList<>();
            for (NxUploadedFile uploadItem : uploadedFiles) {
                String filename = FileUtils.getCleanFileName(uploadItem.getName());
                Blob blob = FileUtils.createTemporaryFileBlob(
                        uploadItem.getFile(), filename,
                        uploadItem.getContentType());
                blobs.add(blob);
            }

            OperationContext ctx = new OperationContext(documentManager);
            ctx.setInput(new BlobList(blobs));
            ctx.putAll(contextParams);

            AutomationService as = Framework.getLocalService(AutomationService.class);
            if (chainOrOperationId.startsWith("Chain.")) {
                return (List<DocumentModel>) as.run(ctx,
                        chainOrOperationId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                OperationParameters params = new OperationParameters(
                        chainOrOperationId, new HashMap<String, Object>());
                chain.add(params);
                return (List<DocumentModel>) as.run(ctx, chain);
            }
        } catch (Exception e) {
            log.error("Error while executing automation batch ", e);
            throw ClientException.wrap(e);
        } finally {
            for (NxUploadedFile uploadItem : getUploadedFiles()) {
                // FIXME: check if a temp file needs to be tracked for
                // deletion
                // File tempFile = uploadItem.getFile();
                // if (tempFile != null && tempFile.exists()) {
                // Framework.trackFile(tempFile, tempFile);
                // }
            }
            uploadedFiles = null;
        }
    }

    public void cancel() {
        if (currentBatchId != null) {
            BatchManager bm = Framework.getLocalService(BatchManager.class);
            bm.clean(currentBatchId);
        }
        importDocumentModel = null;
        selectedImportFolderId = null;
        uploadedFiles = null;
        currentBatchId = null;
    }

    public Collection<NxUploadedFile> getUploadedFiles() {
        if (uploadedFiles == null) {
            uploadedFiles = new ArrayList<>();
        }
        return uploadedFiles;
    }

    public void setUploadedFiles(Collection<NxUploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    @Observer(value = USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED)
    public void invalidateSelectedImportFolder() throws ClientException {
        selectedImportFolderId = null;
    }

    /**
     * @since 6.0
     */
    public void processUpload(FileUploadEvent uploadEvent) {
        try {
            if (uploadedFiles == null) {
                uploadedFiles = new ArrayList<NxUploadedFile>();
            }
            String jstTmpFileDir = Framework.getProperty(FileManageActions.NUXEO_JSF_TMP_DIR_PROP);
            File file = null;
            if (StringUtils.isNotBlank(jstTmpFileDir)) {
                file = File.createTempFile("ImportActions", null, new File(jstTmpFileDir));
            } else {
                file = File.createTempFile("ImportActions", null);
            }
            InputStream in = uploadEvent.getUploadedFile().getInputStream();
            org.nuxeo.common.utils.FileUtils.copyToFile(in, file);
            uploadedFiles.add(new NxUploadedFile(
                    uploadEvent.getUploadedFile().getName(),
                    uploadEvent.getUploadedFile().getContentType(), file));
        } catch (Exception e) {
            log.error(e, e);
        }
    }

}
