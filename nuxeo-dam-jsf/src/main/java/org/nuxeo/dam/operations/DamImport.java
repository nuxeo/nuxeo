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

package org.nuxeo.dam.operations;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.dam.AssetLibrary;
import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.jsf.operations.AddMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Operation creating asset(s) from file(s) inside the configured Asset Library
 * or the current document.
 * <p>
 * If the {@code docType} parameter is not null, documents of this type will be
 * created, otherwise it relies on the {@code FileManager} to create documents.
 *
 * @since 5.7
 */
@Operation(id = DamImport.ID, category = "Dam", label = "Create Asset(s) from file(s)", description = "Create Asset(s) from Blob(s) inside the configured "
        + "Asset Library or the current document. Create documents using the "
        + "given document type if not null, otherwise relies on the FileManagerService")
public class DamImport {

    public static final String ID = "Dam.Import";

    protected static final String SKIP_UPDATE_AUDIT_LOGGING = "org.nuxeo.filemanager.skip.audit.logging.forupdates";

    // duplicated from Audit module to avoid circular dependency
    protected static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected DamService damService;

    @Context
    protected OperationContext context;

    @Context
    protected AutomationService as;

    @Param(name = "overwrite", required = false)
    protected Boolean overwrite = false;

    @Param(name = "importInCurrentDocument", required = false)
    protected Boolean importInCurrentDocument = false;

    /**
     * @since 5.9.5
     */
    @Param(name = "docType", required = false)
    protected String docType;

    protected DocumentModel getCurrentDocument() throws Exception {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws Exception {
        AssetLibrary assetLibrary = damService.getAssetLibrary();
        String title = assetLibrary.getTitle();
        String path = assetLibrary.getPath();
        if (importInCurrentDocument) {
            DocumentModel doc = getCurrentDocument();
            if (doc != null) {
                title = doc.getTitle();
                path = doc.getPathAsString();
            }
        }

        try {
            DocumentModel doc;
            if (StringUtils.isBlank(docType)) {
                doc = fileManager.createDocumentFromBlob(session, blob, path,
                        overwrite, blob.getFilename());
            } else {
                doc = createDocument(blob, path);
            }
            ctx.put(AddMessage.MESSAGE_PARAMS_KEY, new Object[] { 1 });
            return doc;
        } catch (ClientException e) {
            String[] params = { blob.getFilename(), title };
            throw new RecoverableClientException("Cannot import asset",
                    "label.dam.import.asset.error", params, e);
        }
    }

    protected DocumentModel createDocument(Blob blob, String path)
            throws ClientException {
        DocumentModel doc = FileManagerUtils.getExistingDocByFileName(session,
                path, blob.getFilename());
        boolean skipCheckInAfterAdd = false;
        if (overwrite && doc != null) {
            if (!skipCheckInForBlob(getBlob(doc))) {
                checkIn(doc);
            }
            updateDocument(doc, blob);
            if (Framework.isBooleanPropertyTrue(SKIP_UPDATE_AUDIT_LOGGING)) {
                doc.putContextData(DISABLE_AUDIT_LOGGER, true);
            }
            doc = doc.getCoreSession().saveDocument(doc);
        } else {
            doc = session.createDocumentModel(docType);
            doc.setPropertyValue("dc:title", blob.getFilename());
            PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
            doc.setPathInfo(path, pss.generatePathSegment(doc));
            updateDocument(doc, blob);
            skipCheckInAfterAdd = skipCheckInForBlob(blob);
            doc.getAdapter(BlobHolder.class).setBlob(blob);
            doc = session.createDocument(doc);
        }

        if (!skipCheckInAfterAdd) {
            checkInAfterAdd(doc);
        }
        session.save();
        return doc;
    }

    protected Blob getBlob(DocumentModel doc) throws ClientException {
        return doc.getAdapter(BlobHolder.class).getBlob();
    }

    protected boolean skipCheckInForBlob(Blob blob) {
        return blob == null || blob.getLength() == 0;
    }

    protected void checkIn(DocumentModel doc) throws ClientException {
        VersioningOption option = fileManager.getVersioningOption();
        if (option != null && option != VersioningOption.NONE) {
            if (doc.isCheckedOut()) {
                doc.checkIn(option, null);
            }
        }
    }

    protected void updateDocument(DocumentModel doc, Blob blob)
            throws ClientException {
        try {
            blob = blob.persist();
        } catch (IOException e) {
            throw new ClientException(e);
        }
        doc.getAdapter(BlobHolder.class).setBlob(blob);
    }

    protected void checkInAfterAdd(DocumentModel doc) throws ClientException {
        if (fileManager.doVersioningAfterAdd()) {
            checkIn(doc);
        }
    }

    @OperationMethod
    public DocumentModelList run(BlobList blobs) throws Exception {
        DocumentModelList result = new DocumentModelListImpl();
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        ctx.put(AddMessage.MESSAGE_PARAMS_KEY, new Object[] { result.size() });
        return result;
    }

}
