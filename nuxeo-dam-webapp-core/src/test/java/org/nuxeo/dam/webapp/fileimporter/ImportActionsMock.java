package org.nuxeo.dam.webapp.fileimporter;

import org.nuxeo.dam.platform.context.ImportActionsBean;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;

public class ImportActionsMock extends ImportActionsBean {

    private static final long serialVersionUID = 1L;

    public ImportActionsMock(CoreSession coreSession, FileManager fileManager) {
        documentManager = coreSession;
        this.fileManagerService = fileManager;
    }

    public void setFileManagerService(FileManager fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    // don't need UI messages
    @Override
    public void logDocumentWithTitle(String facesMessage, String someLogString,
            DocumentModel document) {

    }

    @Override
    protected void sendImportSetCreationEvent() {
    }

}
