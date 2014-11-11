package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

@Operation(id = BlobHolderAttach.ID, category = Constants.CAT_BLOB, label = "Attach File or files to the currentDocument.",
        description = "Attach the input file(s) to the current document using the BlobHolder abstraction")
public class BlobHolderAttach {

    public static final String ID = "BlobHolder.Attach";

    @Context
    protected CoreSession session;

    @Context
    protected AutomationService as;

    @Context
    protected OperationContext context;

    @Param(name="useMainBlob", required=false)
    protected boolean useMainBlob=true;

    protected DocumentModel getCurrentDocument() throws Exception {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws Exception {
        DocumentModel currentDocument = getCurrentDocument();
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh==null) {
            return currentDocument;
        }
        bh.setBlob(blob);
        currentDocument = session.saveDocument(currentDocument);
        context.put("currentDocument", currentDocument);
        return currentDocument;
    }

    @OperationMethod
    public DocumentModel run(BlobList blobs) throws Exception {
        DocumentModel currentDocument=null;
        if (useMainBlob) {
            Blob mainBlob = blobs.remove(0);
            currentDocument = run(mainBlob);
        } else {
            currentDocument= getCurrentDocument();
        }
        if (blobs.size()>0) {
            if (currentDocument.hasSchema("files")) {
                List<Map<String, Object>> existingBlobs = (List<Map<String, Object>>) currentDocument.getPropertyValue("files:files");
                if (existingBlobs==null) {
                    existingBlobs = new ArrayList<Map<String, Object>>();
                }
                for (Blob blob : blobs) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("file", blob);
                    map.put("filename", blob.getFilename());
                    existingBlobs.add(map);
                }
                currentDocument.setPropertyValue("files:files",(Serializable) existingBlobs);
                currentDocument = session.saveDocument(currentDocument);
            }
        }
        return currentDocument;
    }

}
