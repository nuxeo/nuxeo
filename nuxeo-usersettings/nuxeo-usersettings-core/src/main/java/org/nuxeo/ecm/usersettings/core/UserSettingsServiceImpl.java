/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.usersettings.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.usersettings.UserSettingsProviderDescriptor;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.ecm.usersettings.UserSettingsType;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * Default implementation of the {@link UserPreferencesService}
 * 
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 * 
 */
public class UserSettingsServiceImpl implements UserSettingsService {

    private static final long serialVersionUID = -2643607647191744171L;

    private static final Log log = LogFactory.getLog(UserSettingsServiceImpl.class);

    private Map<String, UserSettingsProviderDescriptor> userSettingsProviders = new HashMap<String, UserSettingsProviderDescriptor>();

    private Map<String, List<String>> providerTypeByCategory = new HashMap<String, List<String>>();

    @Override
    public void registerProvider(UserSettingsProviderDescriptor provider)
            throws ClientException {
        String providerType = provider.getType();
        if (userSettingsProviders.containsKey(providerType)) {

            log.error(String.format(
                    "%s UserSettings provider already registered. Ignoring.",
                    providerType));

        } else {
            String category = provider.getCategory();
            List<String> providerTypes = providerTypeByCategory.get(category);
            if (providerTypes == null) {
                providerTypes = new LinkedList<String>();
            }
            providerTypes.add(providerType);
            providerTypeByCategory.put(category, providerTypes);

            userSettingsProviders.put(providerType, provider);
            log.info(String.format("%s UserSettings provider registered",
                    providerType));

        }

    }

    @Override
    public void unRegisterProvider(String providerName) throws ClientException {
        if (!userSettingsProviders.containsKey(providerName)) {
            log.error(String.format(
                    "Cannot remove %s UserSettings provider : unknown. Ignoring.",
                    providerName));
        } else {
            userSettingsProviders.remove(providerName);
            log.info(String.format("%s UserSettings provider removed",
                    providerName));
        }
    }

    /**
     * Return the personal Workspace.
     * 
     * @param session Current CoreSession
     * @return The personal Workspace
     * @throws ClientException
     */
    protected DocumentModel getUserWorkspace(CoreSession session)
            throws ClientException {
        try {
            String userName = session.getPrincipal().getName();
            UserWorkspaceService uws = Framework.getService(UserWorkspaceService.class);
            DocumentModel userWorkspace = uws.getCurrentUserPersonalWorkspace(
                    userName, session.getRootDocument());
            return userWorkspace;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

    protected DocumentModel getUserSettingsRoot(CoreSession session,
            DocumentModel userWorkspace) throws ClientException {
        DocumentModel userSettingsRoot = null;
        DocumentModelList list = session.getChildren(userWorkspace.getRef(),
                UserSettingsType.DOCTYPE);

        if (list.isEmpty()) {
            userSettingsRoot = createUserSettingsRootDocument(session,
                    userWorkspace);
        } else {
            userSettingsRoot = list.get(0);
        }
        return userSettingsRoot;
    }

    protected DocumentModel createUserSettingsRootDocument(CoreSession session,
            DocumentModel userWorkspace) throws ClientException {
        String userName = session.getPrincipal().getName();
        log.info(String.format(
                "Creating user settings document for user %s in %s", userName,
                userWorkspace.getPathAsString()));

        String parent = userWorkspace.getPathAsString();

        DocumentModel doc = session.createDocumentModel(parent,
                UserSettingsImplConstants.DOC_PREFIX + userName, "UserSettings");

        String title = UserSettingsImplConstants.TITLE;

        try {
            title = Framework.getProperty(UserSettingsImplConstants.TITLE);
        } catch (Exception e) {
            log.warn(String.format("Unable to get %s message, defaulting.",
                    UserSettingsImplConstants.TITLE));
        }

        doc.setProperty("dublincore", "title", title);
        doc.setProperty("usersettings", "user", userName);

        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        return doc;
    }

    @Override
    public Map<String, UserSettingsProviderDescriptor> getAllRegisteredProviders()
            throws ClientException {
        return userSettingsProviders;
    }

    @Override
    public void clearProviders() throws ClientException {
        try {
            List<String> names = new ArrayList<String>();
            Iterator<Entry<String, UserSettingsProviderDescriptor>> it = getAllRegisteredProviders().entrySet().iterator();
            while (it.hasNext())
                names.add(it.next().getKey());
            for (String name : names) {
                log.info(String.format("Cleaning settings provider %s", name));
                unRegisterProvider(name);
            }
        } catch (Exception e) {
            throw new ClientException("Failed to clear providers", e);
        }
    }

    @Override
    public void resetSettingProvider(CoreSession session, String type)
            throws ClientException {
        DocumentModel provider = getCurrentSettingsByType(session, type);
        session.removeDocument(provider.getRef());
    }

    @Override
    public void resetSettingsCategory(CoreSession session, String category)
            throws ClientException {
        DocumentModelList providers = getCurrentSettingsByCategory(session,
                category);
        for (DocumentModel provider : providers) {
            session.removeDocument(provider.getRef());
        }

    }

    @Override
    public Set<String> getCategories() {
        return providerTypeByCategory.keySet();
    }

    @Override
    public List<String> getSettingsByCategory(String category) {
        return providerTypeByCategory.get(category);
    }

    @Override
    public DocumentModelList getCurrentSettingsByCategory(
            CoreSession coreSession, String category) throws ClientException {
        return findUserSettings(coreSession, category, null);
    }

    @Override
    public DocumentModel getCurrentSettingsByType(CoreSession coreSession,
            String type) throws ClientException {
        UserSettingsProviderDescriptor provider = userSettingsProviders.get(type);
        if (provider == null) {
            log.warn("No provider has been registered in the UserSettings service");
            return null;
        }
        DocumentModelList settings = findUserSettings(coreSession,
                provider.getCategory(), type);
        return settings.get(0);
    }

    private DocumentModelList findUserSettings(CoreSession session,
            String category, String type) throws ClientException {
        DocumentModelList result = new DocumentModelListImpl();
        if (userSettingsProviders.size() == 0) {
            log.warn("No provider has been registered in the UserSettings service");
            return result;
        }

        DocumentModel userWorkspace = getUserWorkspace(session);
        DocumentModel userSettingsRoot = getUserSettingsRoot(session,
                userWorkspace);
        DocumentModel settingProviderDoc;
        UserSettingsProviderDescriptor provider;
        if (type != null && !type.equals("")) {
            provider = userSettingsProviders.get(type);
            settingProviderDoc = getUserSettingProviderDoc(provider,
                    userSettingsRoot, session);
            result.add(settingProviderDoc);
        } else {
            List<String> providers = providerTypeByCategory.get(category);
            if (providers == null) {
                log.warn("This category has no assocated providers: "
                        + category);
                return result;
            }
            for (String providerType : providers) {
                provider = userSettingsProviders.get(providerType);
                settingProviderDoc = getUserSettingProviderDoc(provider,
                        userSettingsRoot, session);
                result.add(settingProviderDoc);
            }
        }
        return result;
    }

    protected DocumentModel getUserSettingProviderDoc(
            UserSettingsProviderDescriptor provider,
            DocumentModel userSettingsRoot, CoreSession session)
            throws ClientException {
        String providerType = provider.getType();
        String category = provider.getCategory();
        if (providerType == null || "".equals(providerType)) {
            return null;
        }
        DocumentModel categoryRoot = getCategoryRoot(session, userSettingsRoot,
                category);
        DocumentModelList settingProviderDocList = session.getChildren(
                categoryRoot.getRef(), providerType);
        DocumentModel settingProviderDoc;
        if (settingProviderDocList.isEmpty()) {
            settingProviderDoc = session.createDocumentModel(
                    categoryRoot.getPathAsString(), providerType, providerType);
            settingProviderDoc.setProperty("dublincore", "title", providerType);
            settingProviderDoc = session.createDocument(settingProviderDoc);
            settingProviderDoc = session.saveDocument(settingProviderDoc);
            settingProviderDocList.add(settingProviderDoc);
        } else {
            settingProviderDoc = settingProviderDocList.get(0);
        }
        return settingProviderDoc;
    }

    protected DocumentModel getCategoryRoot(CoreSession session,
            DocumentModel userSettingsRoot, String category)
            throws ClientException {
        DocumentRef categoryRootPath = new PathRef(
                userSettingsRoot.getPathAsString() + "/"
                        + category.toLowerCase());
        DocumentModel categoryRoot;
        if (session.exists(categoryRootPath)) {
            categoryRoot = session.getDocument(categoryRootPath);
        } else {
            categoryRoot = session.createDocumentModel(
                    userSettingsRoot.getPathAsString(), category.toLowerCase(),
                    "Folder");
            categoryRoot.setProperty("dublincore", "title", category);
            categoryRoot = session.createDocument(categoryRoot);
            categoryRoot = session.saveDocument(categoryRoot);
        }
        return categoryRoot;
    }
}
