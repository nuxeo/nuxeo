/*
 * (C) Copyright 2011-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */
package org.nuxeo.ecm.platform.userpreferences;

import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.localconf.SimpleConfiguration;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class UserPreferencesServiceImpl extends DefaultComponent implements UserPreferencesService {

    UserWorkspaceService userWorkspaceService;

    LocalConfigurationService localConfigurationService;

    @Override
    public SimpleUserPreferences getSimpleUserPreferences(CoreSession session) {
        DocumentModel userWorkspace = getUserWorkspaceService().getCurrentUserPersonalWorkspace(session, null);
        if (!userWorkspace.hasFacet(SIMPLE_CONFIGURATION_FACET)) {
            userWorkspace.addFacet(SIMPLE_CONFIGURATION_FACET);
            userWorkspace = session.saveDocument(userWorkspace);
        }
        SimpleConfiguration simpleConfiguration = getLocalConfigurationService().getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET, userWorkspace);
        SimpleUserPreferences userPref = new SimpleUserPreferences(
                session.getDocument(simpleConfiguration.getDocumentRef()));
        return userPref;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends UserPreferences> T getUserPreferences(CoreSession session, Class<T> userPrefClass,
            String userPrefFacet) {
        DocumentModel userWorkspace = getUserWorkspaceService().getCurrentUserPersonalWorkspace(session, null);
        return getLocalConfigurationService().getConfiguration(userPrefClass, userPrefFacet, userWorkspace);
    }

    protected UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService == null) {
            userWorkspaceService = Framework.getService(UserWorkspaceService.class);
        }
        return userWorkspaceService;
    }

    protected LocalConfigurationService getLocalConfigurationService() {
        if (localConfigurationService == null) {
            localConfigurationService = Framework.getService(LocalConfigurationService.class);
        }
        return localConfigurationService;
    }

}
