package org.nuxeo.ecm.usersettings;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public interface UserSettingsService {

    public void registerProvider(UserSettingsProviderDescriptor provider)
            throws ClientException;

    public void unRegisterProvider(String providerName) throws ClientException;

    public void clearProviders() throws ClientException;

    public Map<String, UserSettingsProviderDescriptor> getAllRegisteredProviders()
            throws ClientException;

    public Set<String> getCategories();

    public List<String> getSettingsByCategory(String category);

    public DocumentModelList getCurrentSettingsByCategory(
            CoreSession coreSession, String category) throws ClientException;

    public DocumentModel getCurrentSettingsByType(
            CoreSession coreSession, String type) throws ClientException;

    void resetSettingProvider(CoreSession session, String type)
            throws ClientException;

    void resetSettingsCategory(CoreSession session, String category)
            throws ClientException;

}
