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
 *     Laurent Doguin
 *
 */
package org.nuxeo.ecm.usersettings;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public interface UserSettingsService {

    /**
     * register the setting providers contributed to userSettingsProvider XP.
     * 
     * @param provider
     * @throws ClientException
     */
    void registerProvider(UserSettingsProviderDescriptor provider)
            throws ClientException;

    /**
     * Unregister the given documentType from the service registry.
     * 
     * @param docType
     * @throws ClientException
     */
    void unRegisterProvider(String docType) throws ClientException;

    void clearProviders() throws ClientException;

    /**
     * @return all registered Providers as UserSettingsProviderDescriptor.
     * @throws ClientException
     */
    Map<String, UserSettingsProviderDescriptor> getAllRegisteredProviders()
            throws ClientException;

    /**
     * @return all found category.
     */
    Set<String> getCategories();

    /**
     * Get all document type registered as providers for the given category.
     * 
     * @param category
     * @return
     */
    List<String> getSettingsByCategory(String category);

    /**
     * Provider with the HiddenInSettings facet won't be returned.
     *
     * @param coreSession
     * @param category
     * @return all the settings Provider of the given category from the
     *         session's user.
     * @throws ClientException
     */
    DocumentModelList getCurrentSettingsByCategory(CoreSession coreSession,
            String category) throws ClientException;

    /**
     * 
     * @param coreSession
     * @param category
     * @return all the settings Provider of the given category from the
     *         session's user.
     * @throws ClientException
     */
    DocumentModelList getCurrentSettingsByCategoryUnfiltered(
            CoreSession coreSession, String category) throws ClientException;

    /**
     *
     * @param coreSession
     * @param type
     * @return the setting provider from the session's user.
     * @throws ClientException
     */
    DocumentModel getCurrentSettingsByType(CoreSession coreSession, String type)
            throws ClientException;

    /**
     * Remove the setting provider from the session's user.
     *
     * @param session
     * @param type
     * @throws ClientException
     */
    void resetSettingProvider(CoreSession session, String type)
            throws ClientException;

    /**
     * Remove all settings provider of the given category from the session's
     * user.
     *
     * @param session
     * @param category
     * @throws ClientException
     */
    void resetSettingsCategory(CoreSession session, String category)
            throws ClientException;

}
