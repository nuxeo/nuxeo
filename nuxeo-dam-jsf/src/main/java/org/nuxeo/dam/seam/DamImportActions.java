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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
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
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.webapp.dnd.DndConfigurationHelper;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.model.UploadItem;

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
    protected transient WebActions webActions;

    @In(create = true)
    protected transient DndConfigurationHelper dndConfigHelper;

    protected DocumentModel importDocumentModel;

    protected Action selectedImportOption;

    protected List<Action> importOptions;

    protected String selectedImportFolderId;

    protected String currentBatchId;

    protected Collection<UploadItem> uploadedFiles = null;

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

    public String getSelectedImportFolderId() {
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

    public String generateBatchId() {
        currentBatchId = "batch-" + new Date().getTime() + "-"
                + random.nextInt(1000);
        return currentBatchId;
    }

    public String importAssets() throws ClientException {
        Map<String, Serializable> importOptionProperties = selectedImportOption.getProperties();
        String chainOrOperationId = null;
        if (importOptionProperties.containsKey("chainId")) {
            chainOrOperationId = "Chain."
                    + (String) importOptionProperties.get("chainId");
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
        DataModelProperties properties = new DataModelProperties(dms);

        Map<String, Object> contextParams = new HashMap<>();
        contextParams.put("docMetaData", properties);
        contextParams.put("currentDocument", selectedImportFolderId);

        try {
            if (dndConfigHelper.useHtml5DragAndDrop()) {
                importAssetsThroughBatchManager(chainOrOperationId, contextParams);
            } else {
                importAssetsThroughUploadItems(chainOrOperationId, contextParams);
            }
        } finally {
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
            for (UploadItem uploadItem : uploadedFiles) {
                String filename = FileUtils.getCleanFileName(uploadItem.getFileName());
                Blob blob = FileUtils.createTemporaryFileBlob(uploadItem.getFile(),
                        filename, uploadItem.getContentType());
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
            for (UploadItem uploadItem : getUploadedFiles()) {
                File tempFile = uploadItem.getFile();
                if (tempFile != null && tempFile.exists()) {
                    Framework.trackFile(tempFile, tempFile);
                }
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
    }

    public Collection<UploadItem> getUploadedFiles() {
        if (uploadedFiles == null) {
            uploadedFiles = new ArrayList<>();
        }
        return uploadedFiles;
    }

    public void setUploadedFiles(Collection<UploadItem> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

}
