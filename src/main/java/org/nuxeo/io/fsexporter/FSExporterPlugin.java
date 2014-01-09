package org.nuxeo.io.fsexporter;

import java.io.File;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public interface FSExporterPlugin {

    DocumentModelList getChildren(CoreSession session, DocumentModel doc,
            boolean ExportDeletedDocuments, String PageProvider) throws ClientException, Exception;

    File serialize(CoreSession session, DocumentModel docfrom, String fsPath) throws Exception;

}
