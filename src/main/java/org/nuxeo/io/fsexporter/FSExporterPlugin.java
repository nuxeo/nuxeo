package org.nuxeo.io.fsexporter;

import java.io.File;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

public interface FSExporterPlugin {

    DocumentModelList getChildren(CoreSession session, DocumentModel doc,
            boolean ExportDeletedDocuments) throws ClientException;

    File serialize(DocumentModel docfrom, String fsPath) throws Exception;

}
