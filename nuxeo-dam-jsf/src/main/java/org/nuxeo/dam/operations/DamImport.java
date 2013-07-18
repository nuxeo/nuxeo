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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;

/**
 * Operation creating asset(s) from file(s) inside the configured Asset Library.
 *
 * @since 5.7
 */
@Operation(id = DamImport.ID, category = "Dam", label = "Create Asset(s) from file(s)", description = "Create Asset(s) from Blob(s) using the FileManagerService.")
public class DamImport {

    public static final String ID = "Dam.Import";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected DamService damService;

    @Param(name = "overwrite", required = false)
    protected Boolean overwrite = false;

    @OperationMethod
    public DocumentModel run(Blob blob) throws Exception {
        AssetLibrary assetLibrary = damService.getAssetLibrary();
        ctx.put(AddMessage.MESSAGE_PARAMS_KEY, new Object[] { 1 });
        return fileManager.createDocumentFromBlob(session, blob,
                assetLibrary.getPath(), overwrite, blob.getFilename());
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
