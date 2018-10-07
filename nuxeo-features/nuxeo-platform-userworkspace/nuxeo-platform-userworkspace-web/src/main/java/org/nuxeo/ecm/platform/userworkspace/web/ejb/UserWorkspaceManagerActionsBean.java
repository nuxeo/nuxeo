/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.web.ejb;

import static org.jboss.seam.ScopeType.SESSION;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceManagerActions;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webapp.action.MainTabsActions;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Personal user workspace manager actions bean.
 *
 * @author btatar
 */
@Name("userWorkspaceManagerActions")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
@Startup
public class UserWorkspaceManagerActionsBean implements UserWorkspaceManagerActions {

    public static final String DOCUMENT_VIEW = "view_documents";

    public static final String DOCUMENT_MANAGEMENT_ACTION = "documents";

    private static final long serialVersionUID = 1828552026739219850L;

    private static final Log log = LogFactory.getLog(UserWorkspaceManagerActions.class);

    protected static final String DOCUMENT_MANAGEMENT_TAB = WebActions.MAIN_TABS_CATEGORY + ":"
            + WebActions.DOCUMENTS_MAIN_TAB_ID;

    protected boolean showingPersonalWorkspace;

    protected boolean initialized;

    protected DocumentModel lastAccessedDocument;

    // Rux INA-252: very likely cause of passivation error
    protected transient UserWorkspaceService userWorkspaceService;

    // Rux INA-252: another cause of passivation error
    @In(required = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient NuxeoPrincipal currentUser;

    @In(required = false, create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DashboardNavigationHelper dashboardNavigationHelper;

    @In(create = true)
    protected transient MainTabsActions mainTabsActions;

    @In(create = true)
    protected transient WebActions webActions;

    public void initialize() {
        log.debug("Initializing user workspace manager actions bean");
        showingPersonalWorkspace = false;
        initialized = true;
    }

    @Destroy
    public void destroy() {
        userWorkspaceService = null;
        log.debug("Removing user workspace actions bean");
    }

    private UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService != null) {
            return userWorkspaceService;
        }
        userWorkspaceService = Framework.getService(UserWorkspaceService.class);
        return userWorkspaceService;
    }

    public DocumentModel getCurrentUserPersonalWorkspace() {
        if (!initialized) {
            initialize();
        }
        // protection in case we have not yet chosen a repository. if not
        // repository, then there is no documentManager(session)
        if (documentManager == null) {
            return null;// this is ok because it eventually will be
            // dealt with by setCurrentDocument, which will deal with
            // the lack of a documentManager
        }
        return getUserWorkspaceService().getCurrentUserPersonalWorkspace(documentManager,
                navigationContext.getCurrentDocument());
    }

    public String navigateToCurrentUserPersonalWorkspace() {
        if (!initialized) {
            initialize();
        }
        String returnView = DOCUMENT_VIEW;

        // force return to Documents tab
        webActions.setCurrentTabId(WebActions.MAIN_TABS_CATEGORY, DOCUMENT_MANAGEMENT_ACTION);

        // Rux INA-221: separated links for going to workspaces
        DocumentModel currentUserPersonalWorkspace = getCurrentUserPersonalWorkspace();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isShowingPersonalWorkspace() && currentDocument != null && currentDocument.getPath().segment(0) != null) {
            lastAccessedDocument = mainTabsActions.getDocumentFor(DOCUMENT_MANAGEMENT_ACTION,
                    navigationContext.getCurrentDocument());
        }
        navigationContext.setCurrentDocument(currentUserPersonalWorkspace);
        showingPersonalWorkspace = true;

        Events.instance().raiseEvent(EventNames.GO_PERSONAL_WORKSPACE);

        return returnView;
    }

    // Rux INA-221: create a new method for the 2 separated links
    public String navigateToOverallWorkspace() {
        if (!initialized) {
            initialize();
        }
        String returnView = DOCUMENT_VIEW;

        // force return to Documents tab
        webActions.setCurrentTabIds(DOCUMENT_MANAGEMENT_TAB);

        if (lastAccessedDocument != null) {
            navigationContext.setCurrentDocument(lastAccessedDocument);
        } else if (navigationContext.getCurrentDomain() != null) {
            navigationContext.setCurrentDocument(navigationContext.getCurrentDomain());
        } else if (documentManager.hasPermission(documentManager.getRootDocument().getRef(),
                SecurityConstants.READ_CHILDREN)) {
            navigationContext.setCurrentDocument(documentManager.getRootDocument());
        } else {
            navigationContext.setCurrentDocument(null);
            returnView = dashboardNavigationHelper.navigateToDashboard();
        }
        showingPersonalWorkspace = false;

        Events.instance().raiseEvent(EventNames.GO_HOME);

        return returnView;
    }

    @Override
    @Factory(value = "isInsidePersonalWorkspace", scope = ScopeType.EVENT)
    public boolean isShowingPersonalWorkspace() {
        if (!initialized) {
            initialize();
        }
        if (mainTabsActions.isOnMainTab(DOCUMENT_MANAGEMENT_ACTION)) {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            showingPersonalWorkspace = getUserWorkspaceService().isUnderUserWorkspace(currentUser, null, currentDoc);
        }
        return showingPersonalWorkspace;
    }

    public void setShowingPersonalWorkspace(boolean showingPersonalWorkspace) {
        this.showingPersonalWorkspace = showingPersonalWorkspace;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

}
