package org.nuxeo.ecm.usersettings;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.schema.DocumentType;

public interface UserSettingsService {

 
    public DocumentModelList getCurrentUserSettings(CoreSession coreSession)
            throws ClientException;

    public DocumentModelList getCurrentUserSettings(CoreSession coreSession,
            DocumentType filter) throws ClientException;

    public DocumentModelList getCurrentUserSettings(CoreSession coreSession,
            String type) throws ClientException;

    
    public DocumentModelList getUserSettings(CoreSession coreSession,
            String userName) throws ClientException;

    public DocumentModelList getUserSettings(CoreSession coreSession,
            String userName, DocumentType filter) throws ClientException;

    public DocumentModelList getUserSettings(CoreSession coreSession,
            String userName, String type) throws ClientException;

    
    public void registerProvider(String providerName,
            UserSettingsProvider provider) throws ClientException;

    public void unRegisterProvider(String providerName) throws ClientException;
    
    public void clearProviders() throws ClientException;

    public Map<String, UserSettingsProvider> getAllRegisteredProviders()
            throws ClientException;

}
