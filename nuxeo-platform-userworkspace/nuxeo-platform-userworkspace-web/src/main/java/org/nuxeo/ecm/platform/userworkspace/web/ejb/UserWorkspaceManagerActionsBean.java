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

package org.nuxeo.ecm.platform.userworkspace.web.ejb;

import static org.jboss.seam.ScopeType.SESSION;

import javax.ejb.Remove;

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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceManagerActions;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Personal user workspace manager actions bean.
 *
 * @author btatar
 *
 */
@Name("userWorkspaceManagerActions")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
@Startup
public class UserWorkspaceManagerActionsBean implements
        UserWorkspaceManagerActions {

    private static final long serialVersionUID = 1828552026739219850L;

    private static final Log log = LogFactory.getLog(UserWorkspaceManagerActions.class);

    public static final String DOCUMENT_VIEW = "view_documents";

    protected boolean showingPersonalWorkspace;

    protected boolean initialized;

    protected DocumentModel lastAccessedDocument;

    // Rux INA-252: very likely cause of passivation error
    protected transient UserWorkspaceService userWorkspaceService;

    // Rux INA-252: another cause of passivation error
    @In(required = true)
    protected transient NavigationContext navigationContext;

    @In(required = false, create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    public void initialize() {
        log.debug("Initializing user workspace manager actions bean");
        try {
            // Rux INA-252: use a getter
            // userWorkspaceService =
            // Framework.getLocalService(UserWorkspaceService.class);
            showingPersonalWorkspace = false;
            initialized = true;
        } catch (Exception e) {
            log.error(
                    "There was an error while trying to get UserWorkspaceService",
                    e);
        }
    }

    @Destroy
    @Remove
    public void destroy() {
        userWorkspaceService = null;
        log.debug("Removing user workspace actions bean");
    }

    private UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService != null) {
            return userWorkspaceService;
        }
        userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        return userWorkspaceService;
    }

    public DocumentModel getCurrentUserPersonalWorkspace()
            throws ClientException {
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
        return getUserWorkspaceService().getCurrentUserPersonalWorkspace(
                documentManager, navigationContext.getCurrentDocument());
    }

    public String navigateToCurrentUserPersonalWorkspace()
            throws ClientException {
        if (!initialized) {
            initialize();
        }
        String returnView = DOCUMENT_VIEW;

        // Rux INA-221: separated links for going to workspaces
        DocumentModel currentUserPersonalWorkspace = getCurrentUserPersonalWorkspace();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isShowingPersonalWorkspace() && currentDocument != null
                && currentDocument.getPath().segment(0) != null) {
            lastAccessedDocument = navigationContext.getCurrentDocument();
        }
        navigationContext.setCurrentDocument(currentUserPersonalWorkspace);
        showingPersonalWorkspace = true;


        Events.instance().raiseEvent(EventNames.GO_PERSONAL_WORKSPACE);


        return returnView;
    }

    // Rux INA-221: create a new method for the 2 separated links
    public String navigateToOverallWorkspace() throws ClientException {
        if (!initialized) {
            initialize();
        }
        String returnView = DOCUMENT_VIEW;

        if (lastAccessedDocument != null) {
            navigationContext.setCurrentDocument(lastAccessedDocument);
        } else if (navigationContext.getCurrentDomain() != null) {
            navigationContext.setCurrentDocument(navigationContext.getCurrentDomain());
        } else {
            navigationContext.setCurrentDocument(null);
            returnView = dashboardNavigationHelper.navigateToDashboard();
        }
        showingPersonalWorkspace = false;

        Events.instance().raiseEvent(EventNames.GO_HOME);

        return returnView;
    }

    @Factory(value = "isInsidePersonalWorkspace", scope = ScopeType.EVENT)
    public boolean isShowingPersonalWorkspace() {
        if (!initialized) {
            initialize();
        }

        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (currentDoc == null || currentDoc.getPath().segmentCount() < 2) {
            return false;
        }

        String secondSegment = currentDoc.getPath().segment(1);
        showingPersonalWorkspace = secondSegment != null
                && secondSegment.startsWith(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
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
