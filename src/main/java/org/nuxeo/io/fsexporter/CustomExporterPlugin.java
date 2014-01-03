package org.nuxeo.io.fsexporter;

import java.io.File;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public class CustomExporterPlugin implements FSExporterPlugin {

	@Override
	public DocumentModelList getChildren(CoreSession session,
			DocumentModel doc, boolean ExportDeletedDocuments)
			throws ClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File serialize(DocumentModel docfrom, String fsPath)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
