/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.userpreferences;

import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class UserPreferencesServiceImpl extends DefaultComponent implements
        UserPreferencesService {

    UserWorkspaceService userWorkspaceService;

    LocalConfigurationService localConfigurationService;

    @Override
    public SimpleUserPreferences getSimpleUserPreferences(CoreSession session)
            throws ClientException {
        DocumentModel userWorkspace = getUserWorkspaceService().getCurrentUserPersonalWorkspace(
                session, null);
        if (!userWorkspace.hasFacet(SIMPLE_CONFIGURATION_FACET)) {
            userWorkspace.addFacet(SIMPLE_CONFIGURATION_FACET);
            userWorkspace = session.saveDocument(userWorkspace);
        }
        SimpleConfiguration simpleConfiguration = getLocalConfigurationService().getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                userWorkspace);
        SimpleUserPreferences userPref = new SimpleUserPreferences(session.getDocument(simpleConfiguration.getDocumentRef()));
        return userPref;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends UserPreferences> T getUserPreferences(
            CoreSession session, Class<T> userPrefClass, String userPrefFacet)
            throws ClientException {
        DocumentModel userWorkspace = getUserWorkspaceService().getCurrentUserPersonalWorkspace(
                session, null);
        return getLocalConfigurationService().getConfiguration(userPrefClass,
                userPrefFacet, userWorkspace);
    }

    protected UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService == null) {
            userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        }
        return userWorkspaceService;
    }

    protected LocalConfigurationService getLocalConfigurationService() {
        if (localConfigurationService == null) {
            localConfigurationService = Framework.getLocalService(LocalConfigurationService.class);
        }
        return localConfigurationService;
    }

}
