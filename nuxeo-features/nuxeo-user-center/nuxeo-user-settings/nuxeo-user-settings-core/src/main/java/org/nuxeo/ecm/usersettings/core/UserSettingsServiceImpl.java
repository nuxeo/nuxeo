/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Christophe Capon
 *
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
import org.nuxeo.ecm.usersettings.UserSettingsConstants;
import org.nuxeo.ecm.usersettings.UserSettingsDescriptor;
import org.nuxeo.ecm.usersettings.UserSettingsProviderDescriptor;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.ecm.usersettings.UserSettingsType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This component is used to register the service that provide the userworkspace
 * service support.
 * 
 * @author btatar
 * @author Damien METZLER (damien.metzler@leroymerlin.fr)
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 */
public class UserSettingsServiceImpl extends DefaultComponent implements UserSettingsService {

    public static final String NAME = "com.nuxeo.vilogia.usersettings.UserSettingsServiceComponent";

    private static final Log log = LogFactory.getLog(UserSettingsService.class);

    public static final String DOC_PREFIX = "nuxeoUserSettings_";

    public static final String TITLE = "usersettings.title";

    private static UserSettingsDescriptor descriptor;

    private static UserSettingsService userSettingsService;

    private Map<String, UserSettingsProviderDescriptor> userSettingsProviders = new HashMap<String, UserSettingsProviderDescriptor>();

    private Map<String, List<String>> providerTypeByCategory = new HashMap<String, List<String>>();

    @Override
    public void activate(ComponentContext context) {
        log.info("UserSettingsService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("UserSettingsService deactivated");
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {

        if (UserSettingsConstants.SETTINGS_PROVIDER_EXTENSION_POINT.equals(extensionPoint)) {
            registerUserSettingsProvider(contribution, extensionPoint,
                    contributor);
        } else {
            throw new ClientException(extensionPoint
                    + " is not a valid extension point.");
        }

    }

    private void registerUserSettingsProvider(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {
        if (contribution instanceof UserSettingsProviderDescriptor) {
            UserSettingsProviderDescriptor desc = (UserSettingsProviderDescriptor) contribution;
            registerProvider(desc);
            log.info(String.format(
                    "Registered %s extension point with %s name.",
                    extensionPoint, desc.getType()));
        }
    }

    private void unregisterPendingProviders() throws ClientException {
        clearProviders();
    }

    private void unregisterUserSettingsProvider(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {
        if (contribution instanceof UserSettingsProviderDescriptor) {
            UserSettingsProviderDescriptor desc = (UserSettingsProviderDescriptor) contribution;
            try {
                unRegisterProvider(desc.getType());
                log.info(String.format("Unregistering settings provider %s",
                        desc.getType()));
            } catch (Exception e) {
                throw new ClientException(
                        "Failed to unregister UserSettingsService", e);
            }
        }

    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {

        if (UserSettingsConstants.BASE_EXTENSION_POINT.equals(extensionPoint)) {
            unregisterPendingProviders();
            try {
            } catch (Exception e) {
                log.error("Error when terminating service", e);
            }
            descriptor = null;
        } else if (UserSettingsConstants.SETTINGS_PROVIDER_EXTENSION_POINT.equals(extensionPoint)) {
            unregisterUserSettingsProvider(contribution, extensionPoint,
                    contributor);
        } else {
            throw new ClientException(extensionPoint
                    + " is not a valid extension point.");
        }

    }

    public static void reset() {
        userSettingsService = null;
    }

    public static Class<? extends UserSettingsService> getUserSettingsClass() {
        return descriptor.getUserSettingsClass();
    }

    @Override
    public void registerProvider(UserSettingsProviderDescriptor provider)
            throws ClientException {
        boolean isEnabled = provider.isEnabled();
        String providerType = provider.getType();
        if (userSettingsProviders.containsKey(providerType)) {
            if (!isEnabled) {
                userSettingsProviders.remove(providerType);
            } else {
                log.error(String.format(
                        "%s UserSettings provider already registered. Overriding.",
                        providerType));
            }
        } else if (isEnabled) {
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
                DOC_PREFIX + userName, "UserSettings");

        String title = TITLE;

        try {
            title = Framework.getProperty(TITLE);
        } catch (Exception e) {
            log.warn(String.format("Unable to get %s message, defaulting.",
                    TITLE));
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
        return findUserSettings(coreSession, category, null, true);
    }

    @Override
    public DocumentModelList getCurrentSettingsByCategoryUnfiltered(
            CoreSession coreSession, String category) throws ClientException {
        return findUserSettings(coreSession, category, null, false);
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
                provider.getCategory(), type, false);
        return settings.get(0);
    }

    private DocumentModelList findUserSettings(CoreSession session,
            String category, String type, boolean hiddenInSettingsFilter)
            throws ClientException {
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
                if (!(hiddenInSettingsFilter && settingProviderDoc.hasFacet(UserSettingsConstants.HIDDEN_IN_SETTINGS_FACET))) {
                    result.add(settingProviderDoc);
                }
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