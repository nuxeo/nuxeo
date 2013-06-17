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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.api.Framework;

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

    protected DocumentModel importDocumentModel;

    protected Action selectedImportOption;

    protected List<Action> importOptions;

    protected String currentBatchId;

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

    public String generateBatchId() {
        currentBatchId = "batch-" + new Date().getTime() + "-"
                + random.nextInt(1000);
        return currentBatchId;
    }

    protected int getUploadWaitTimeout() {
        String t = Framework.getProperty("org.nuxeo.batch.upload.wait.timeout",
                "5");
        return Integer.parseInt(t);
    }

    public String importAssets() throws ClientException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);

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

        bm.executeAndClean(currentBatchId, chainOrOperationId, documentManager,
                contextParams, null);

        return null;
    }

    public void cancel() {
        if (currentBatchId != null) {
            BatchManager bm = Framework.getLocalService(BatchManager.class);
            bm.clean(currentBatchId);
            importDocumentModel = null;
        }
    }

}
