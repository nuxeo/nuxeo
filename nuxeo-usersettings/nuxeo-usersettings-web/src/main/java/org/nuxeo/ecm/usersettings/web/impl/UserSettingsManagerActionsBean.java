/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.usersettings.web.impl;

import static org.jboss.seam.ScopeType.CONVERSATION;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.ecm.usersettings.web.api.UserSettingsManagerActions;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Personal user workspace manager actions bean.
 * 
 * @author btatar
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 * 
 */
@Name("userSettingsActions")
@Scope(CONVERSATION)
public class UserSettingsManagerActionsBean implements
        UserSettingsManagerActions {

    private static final long serialVersionUID = 1828592026739219850L;

    private static final Log log = LogFactory.getLog(UserSettingsManagerActions.class);

    private static final String DOCUMENT_VIEW = "view_documents";

    private boolean initialized;

    // Rux INA-252: very likely cause of passivation error
    private transient UserSettingsService userPreferencesService;

    // Rux INA-252: another cause of passivation error
    @In(required = true)
    private transient NavigationContext navigationContext;

    @In(required = true)
    private transient CoreSession documentManager;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    private DocumentModel previousDocument = null;

    public void initialize() {
        log.debug("Initializing user preferences manager actions bean");
        try {
            initialized = true;
        } catch (Exception e) {
            log.error(
                    "There was an error while trying to get UserWorkspaceService",
                    e);
        }
    }

    @Destroy
    public void destroy() {
        userPreferencesService = null;
        log.debug("Removing user workspace actions bean");
    }

    private UserSettingsService getUserSettingsService() {
        if (userPreferencesService != null) {
            return userPreferencesService;
        }
        try {
            userPreferencesService = Framework.getService(UserSettingsService.class);
            return userPreferencesService;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public DocumentModel getCurrentUserSettings() throws Exception {

        return getUserSettingsService().getCurrentUserSettings(documentManager,
                "UserSettings").get(0);

    }

    public void populateCurrentUserSettings() throws Exception {

        getUserSettingsService().getCurrentUserSettings(documentManager);

    }

    public String navigateToCurrentUserSettings() throws Exception {

        if (!initialized) {
            initialize();
        }
        String returnView = DOCUMENT_VIEW;

        backupContext();

        populateCurrentUserSettings();

        DocumentModel currentUserPreferences = getCurrentUserSettings();

        navigationContext.setCurrentDocument(currentUserPreferences);

        return returnView;
    }

    private void backupContext() {

        if (previousDocument == null
                || !previousDocument.getPathAsString().equals(
                        navigationContext.getCurrentDocument().getPathAsString())) {
            previousDocument = navigationContext.getCurrentDocument();
        }

    }

    public String update(DocumentActions docActions) throws ClientException {

        docActions.updateDocument();

        restoreContext();

        return DOCUMENT_VIEW;

    }

    private void restoreContext() {
        try {
            navigationContext.setCurrentDocument(previousDocument);
        } catch (ClientException e) {
            // TODO Auto-generated catch block
        }
    }

}
