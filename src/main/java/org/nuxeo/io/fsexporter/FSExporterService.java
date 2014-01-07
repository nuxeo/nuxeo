package org.nuxeo.io.fsexporter;

import java.io.IOException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

public interface FSExporterService {

    void export(CoreSession session, String rootPath, String fsPath,
            boolean ExportDeletedDocuments, String PageProvider) throws ClientException,
            IOException, Exception;

}
