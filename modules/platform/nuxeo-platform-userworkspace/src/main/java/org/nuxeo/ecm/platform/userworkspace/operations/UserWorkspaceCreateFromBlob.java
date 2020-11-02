/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.userworkspace.operations;

import java.io.IOException;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;

/**
 * Uses {@link FileManager} to import files inside the User's personal workspace.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = UserWorkspaceCreateFromBlob.ID, category = Constants.CAT_SERVICES, label = "Create Document from file in User's workspace", description = "Create Document(s) in the user's workspace from Blob(s) using the FileManagerService.")
public class UserWorkspaceCreateFromBlob {

    public static final String ID = "UserWorkspace.CreateDocumentFromBlob";

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected OperationContext context;

    @Context
    protected UserWorkspaceService userWorkspace;

    @Context
    protected AutomationService as;

    protected DocumentModel getCurrentDocument() throws OperationException {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws OperationException, IOException {
        DocumentModel userws = userWorkspace.getCurrentUserPersonalWorkspace(session, getCurrentDocument());
        FileImporterContext fileImporterContext = FileImporterContext.builder(session, blob, userws.getPathAsString())
                                                                     .build();
        return fileManager.createOrUpdateDocument(fileImporterContext);
    }

    @OperationMethod
    public DocumentModelList run(BlobList blobs) throws OperationException, IOException {
        DocumentModelList result = new DocumentModelListImpl();
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        return result;
    }

}
