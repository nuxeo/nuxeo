package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
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
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;

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

    protected DocumentModel getCurrentDocument() throws Exception {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws Exception {
        DocumentModel userws = userWorkspace.getCurrentUserPersonalWorkspace(session, getCurrentDocument());
        DocumentModel doc = fileManager.createDocumentFromBlob(session, blob, userws.getPathAsString(),false , blob.getFilename());
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(BlobList blobs) throws Exception {
        DocumentModelList result = new DocumentModelListImpl();
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        return result;
    }

}
