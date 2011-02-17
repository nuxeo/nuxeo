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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.usersettings.UserSettingsProvider;
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

    private Map<String, UserSettingsProvider> UserSettingsProviders = new HashMap<String, UserSettingsProvider>();

    private DocumentModel root;

    public void registerProvider(String providerName,
            UserSettingsProvider provider) throws ClientException {

        if (UserSettingsProviders.containsKey(providerName)) {

            log.error(String.format(
                    "%s UserSettings provider already registered. Ignoring.",
                    providerName));

        } else {

            UserSettingsProviders.put(providerName, provider);

            log.info(String.format("%s UserSettings provider registered",
                    providerName));

        }

    }

    public void unRegisterProvider(String providerName) throws ClientException {

        if (!UserSettingsProviders.containsKey(providerName)) {

            log.error(String.format(
                    "Cannot remove %s UserSettings provider : unkown. Ignoring.",
                    providerName));

        } else {

            UserSettingsProviders.remove(providerName);

            log.info(String.format("%s UserSettings provider removed",
                    providerName));

        }

    }

    public DocumentModelList getUserSettings(CoreSession userSession,
            String userName) throws ClientException {

        DocumentModelList result = findUserSettings(userSession, userName,
                getUserWorkspace(userSession, userName), null);

        return result;

    }

    public DocumentModelList getUserSettings(CoreSession userSession,
            String userName, DocumentType filter) throws ClientException {

        DocumentModelList result = findUserSettings(userSession, userName,
                getUserWorkspace(userSession, userName), filter.getName());

        return result;
    }

    public DocumentModelList getUserSettings(CoreSession userSession,
            String userName, String type) throws ClientException {

        DocumentModelList result = findUserSettings(userSession, userName,
                getUserWorkspace(userSession, userName), type);

        return result;
    }

    private DocumentModelList findUserSettings(CoreSession userSession,
            String userName, DocumentModel userWorkspace, String type)
            throws ClientException {

        initRoot(userSession, userName, userWorkspace);

        DocumentModelList result = new DocumentModelListImpl();

        if (UserSettingsType.DOCTYPE.equals(type)) {
            result.add(root);
            return result;
        }

        if (UserSettingsProviders.size() == 0) {
            log.warn("No provider has been registered in the UserSettings service");
            return result;
        }

        Iterator<Entry<String, UserSettingsProvider>> it = UserSettingsProviders.entrySet().iterator();

        while (it.hasNext()) {

            UserSettingsProvider prov = it.next().getValue();

            if (type == null || prov.isTypeSupported(type)) {
                result.addAll(prov.getUserSettings(root, userSession, userName));
            }

        }

        userSession.save();

        return result;
    }

    /**
     * Return the personal Workspace.
     * 
     * @param session Current CoreSession
     * @return The personal Workspace
     * @throws ClientException
     */

    protected DocumentModel getUserWorkspace(CoreSession session,
            String userName) throws ClientException {

        try {

            UserWorkspaceService uws = Framework.getService(UserWorkspaceService.class);

            DocumentModel userWorkspace = uws.getCurrentUserPersonalWorkspace(
                    userName, session.getRootDocument());

            session.save();

            return userWorkspace;

        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

    protected void initRoot(CoreSession userCoreSession, String userName,
            DocumentModel userWorkspace) throws ClientException {

        DocumentModelList list = userCoreSession.getChildren(
                userWorkspace.getRef(), UserSettingsType.DOCTYPE);

        if (list.isEmpty())
            root = createUserSettingsRootDocument(userCoreSession, userName,
                    userWorkspace);
        else
            root = list.get(0);

    }

    protected DocumentModel createUserSettingsRootDocument(
            CoreSession userSession, String userName,
            DocumentModel userWorkspace) throws ClientException {

        log.info(String.format(
                "Creating user settings document for user %s in %s", userName,
                userWorkspace.getPathAsString()));

        String parent = userWorkspace.getPathAsString();

        DocumentModel doc = userSession.createDocumentModel(parent,
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

        doc = userSession.createDocument(doc);
        doc = userSession.saveDocument(doc);

        userSession.save();

        return doc;

    }

    public DocumentModelList getCurrentUserSettings(CoreSession userSession)
            throws ClientException {

        return getUserSettings(userSession,
                userSession.getPrincipal().getName());

    }

    public DocumentModelList getCurrentUserSettings(CoreSession userSession,
            DocumentType filter) throws ClientException {

        return getUserSettings(userSession,
                userSession.getPrincipal().getName(), filter);

    }

    public DocumentModelList getCurrentUserSettings(CoreSession userSession,
            String type) throws ClientException {

        return getUserSettings(userSession,
                userSession.getPrincipal().getName(), type);

    }

    public Map<String, UserSettingsProvider> getAllRegisteredProviders()
            throws ClientException {
        return UserSettingsProviders;
    }

    public void clearProviders() throws ClientException {

        try {

            List<String> names = new ArrayList<String>();
            Iterator<Entry<String, UserSettingsProvider>> it = getAllRegisteredProviders().entrySet().iterator();
            while (it.hasNext())
                names.add(it.next().getKey());
            for (String name : names) {
                log.info(String.format(
                        "Cleaning settings provider %s", name));
                unRegisterProvider(name);
            }

        } catch (Exception e) {
            throw new ClientException(
                    "Failed to clear providers", e);
        }
    }

}
