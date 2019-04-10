/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.dam.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.dam.DamConstants.REFRESH_DAM_SEARCH;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.dam.AssetLibrary;
import org.nuxeo.dam.DamService;
import org.nuxeo.dam.provider.ImportFolderPageProvider;
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
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.webapp.dnd.DndConfigurationHelper;
import org.nuxeo.ecm.webapp.filemanager.NxUploadedFile;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.FileUploadEvent;

/**
 * Handles DAM import related actions.
 *
 * @since 5.7
 */
@Name("damImportActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamImportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamImportActions.class);

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

    protected DocumentModel importDocumentModel;

    protected Action selectedImportOption;

    protected List<Action> importOptions;

    protected String selectedImportFolderId;

    protected String currentBatchId;

    protected Collection<NxUploadedFile> uploadedFiles = null;

    protected String selectedNewAssetType;

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
            // try to get the Asset Library
            DamService damService = Framework.getLocalService(DamService.class);
            AssetLibrary assetLibrary = damService.getAssetLibrary();
            if (assetLibrary != null) {
                PathRef ref = new PathRef(
                        damService.getAssetLibrary().getPath());
                if (documentManager.exists(ref)) {
                    DocumentModel doc = documentManager.getDocument(ref);
                    if (ImportFolderPageProvider.COMPOUND_FILTER.accept(doc)) {
                        selectedImportFolderId = doc.getId();
                    }
                }
            }
        }
        return selectedImportFolderId;
    }

    public void setSelectedImportFolderId(String selectedImportFolderId) {
        this.selectedImportFolderId = selectedImportFolderId;
    }

    public List<Type> getSubTypesFor(DocumentModel doc) {
        TypeManager typeManager = Framework.getLocalService(TypeManager.class);
        List<Type> types = new ArrayList<>();
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        for (String typeName : typeInfo.getAllowedSubTypes().keySet()) {
            types.add(typeManager.getType(typeName));
        }
        return types;
    }

    public List<Type> getAllowedImportFolderSubTypes() throws ClientException {
        if (StringUtils.isBlank(selectedImportFolderId)) {
            return Collections.emptyList();
        }

        DocumentModel doc = documentManager.getDocument(new IdRef(
                selectedImportFolderId));
        TypeManager typeManager = Framework.getLocalService(TypeManager.class);
        DamService damService = Framework.getLocalService(DamService.class);
        List<Type> allowedAssetTypes = damService.getAllowedAssetTypes();
        Collection<Type> allowedSubTypes = typeManager.getAllowedSubTypes(
                doc.getType(), doc);
        List<Type> types = new ArrayList<>();
        for (Type type : allowedAssetTypes) {
            if (allowedSubTypes.contains(type)) {
                types.add(type);
            }
        }
        return types;
    }

    /*
     * ----- Asset bulk import -----
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

    public String importAssets() throws ClientException {
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

        try {
            if (dndConfigHelper.useHtml5DragAndDrop()) {
                importAssetsThroughBatchManager(chainOrOperationId,
                        contextParams);
            } else {
                importAssetsThroughUploadItems(chainOrOperationId,
                        contextParams);
            }
        } finally {
            // reset batch state
            cancel();
        }

        return null;
    }

    protected void importAssetsThroughBatchManager(String chainOrOperationId,
            Map<String, Object> contextParams) throws ClientException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.executeAndClean(currentBatchId, chainOrOperationId, documentManager,
                contextParams, null);
    }

    protected void importAssetsThroughUploadItems(String chainOrOperationId,
            Map<String, Object> contextParams) throws ClientException {
        if (uploadedFiles == null) {
            return;
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
                as.run(ctx, chainOrOperationId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                OperationParameters params = new OperationParameters(
                        chainOrOperationId, new HashMap<String, Object>());
                chain.add(params);
                as.run(ctx, chain);
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

    /*
     * ----- Asset creation -----
     */

    /**
     * Gets the selected new asset type.
     * <p>
     * If selected type is null, initialize it to the first one, and initialize
     * the changeable document with this document type.
     */
    public String getSelectedNewAssetType() throws ClientException {
        if (selectedNewAssetType == null) {
            List<Type> allowedAssetTypes = getAllowedImportFolderSubTypes();
            if (!allowedAssetTypes.isEmpty()) {
                selectedNewAssetType = allowedAssetTypes.get(0).getId();
            }
            if (selectedNewAssetType != null) {
                selectNewAssetType();
            }
        }
        return selectedNewAssetType;
    }

    public void setSelectedNewAssetType(String selectedNewAssetType) {
        this.selectedNewAssetType = selectedNewAssetType;
    }

    public void selectNewAssetType() throws ClientException {
        String selectedType = getSelectedNewAssetType();
        if (selectedType == null) {
            // ignore
            return;
        }
        Map<String, Object> context = new HashMap<String, Object>();
        DocumentModel changeableDocument = documentManager.createDocumentModel(
                selectedType, context);
        navigationContext.setChangeableDocument(changeableDocument);
    }

    public void saveNewAsset() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        if (StringUtils.isBlank(selectedImportFolderId)
                || changeableDocument.getId() != null) {
            return;
        }
        PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
        DocumentModel doc = documentManager.getDocument(new IdRef(
                selectedImportFolderId));
        changeableDocument.setPathInfo(doc.getPathAsString(),
                pss.generatePathSegment(changeableDocument));

        changeableDocument = documentManager.createDocument(changeableDocument);
        documentManager.save();

        // reset changeable document and selected type
        cancelNewAsset();

        // refresh the current dam search
        Events.instance().raiseEvent(REFRESH_DAM_SEARCH);

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("document_saved"),
                messages.get(changeableDocument.getType()));
    }

    public void cancelNewAsset() {
        selectedImportFolderId = null;
        navigationContext.setChangeableDocument(null);
        setSelectedNewAssetType(null);
    }

    /**
     * @since 6.0
     */
    public void processUpload(FileUploadEvent uploadEvent) {
        try {
            if (uploadedFiles == null) {
                uploadedFiles = new ArrayList<NxUploadedFile>();
            }
            File file = File.createTempFile("ImportActions", null);
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
