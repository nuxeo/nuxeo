package org.nuxeo.io.fsexporter;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public class DefaultExporterPlugin implements FSExporterPlugin {

    @Override
    public DocumentModelList getChildren(CoreSession session,
            DocumentModel doc, boolean ExportDeletedDocuments)
            throws ClientException {

        String queryToMake;
        if (ExportDeletedDocuments == true) {
            queryToMake = String.format(
                    "SELECT * FROM Document WHERE ecm:parentId = '%s'",
                    doc.getId());
        } else {
            queryToMake = String.format(
                    "SELECT * FROM Document WHERE ecm:parentId = '%s' AND ecm:currentLifeCycleState != 'deleted'",
                    doc.getId());
        }
        DocumentModelList children = session.query(queryToMake);
        return children;
    }

    @Override
    public File serialize(DocumentModel docfrom, String fsPath)
            throws Exception {
        File folder = null;
        File newFolder = null;
        String prefix = "";
        folder = new File(fsPath);

        if (docfrom.hasFacet("Folderish")) {
            newFolder = new File(fsPath + "/" + docfrom.getName());
            newFolder.mkdir();
            prefix = docfrom.getTitle();
        }

        // blobholder

        // => save file with prefix for folder
        BlobHolder myblobholder = docfrom.getAdapter(BlobHolder.class);
        if (myblobholder != null) {
            List<Blob> listblobs = myblobholder.getBlobs();
            int i = 1;
            for (Blob blob : listblobs) {
                // before creating a new file, verify that a file doesn't
                // already exist with the same name
                File alreadyExistingBlob = new File(folder, prefix
                        + blob.getFilename());

                prefix = "";
                // if principal, prefix = "timestamp"
                if (alreadyExistingBlob.exists() && i == 1) {
                    prefix = "timestamp-";
                }
                // if not principal file, prefix = name of the file which
                // contains the blobs

                if (alreadyExistingBlob.exists() && i != 1) {
                    prefix = docfrom.getName() + "-";
                }
                File target = new File(folder, prefix + blob.getFilename());
                blob.transferTo(target);
                i++;
            }
        }
        if (newFolder != null)
            folder = newFolder;
        return folder;
    }

}
