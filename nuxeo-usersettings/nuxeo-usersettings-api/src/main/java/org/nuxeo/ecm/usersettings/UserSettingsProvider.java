package org.nuxeo.ecm.usersettings;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.schema.DocumentType;

public interface UserSettingsProvider {

    public Boolean isTypeSupported(String type);

    public Boolean isTypeSupported(DocumentType type);

    public DocumentModelList getUserSettings(DocumentModel userWorkspace,
            CoreSession coreSession, String userName) throws ClientException;

}
