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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.security.Principal;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;

@Name("startupHelper")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
public class StartupHelper implements Serializable {

    public static final String SERVERS_VIEW = "view_servers";

    public static final String DOMAINS_VIEW = "view_domains";

    protected static final String DOMAIN_TYPE = "Domain";

    protected static final String DOCUMENT_MANAGEMENT_TAB = WebActions.MAIN_TABS_CATEGORY + ":"
            + WebActions.DOCUMENTS_MAIN_TAB_ID;

    private static final long serialVersionUID = 3248972387619873245L;

    private static final Log log = LogFactory.getLog(StartupHelper.class);

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    @In(create = true)
    ConversationIdGenerator conversationIdGenerator;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient RestHelper restHelper;

    /**
     * Initializes the context with the principal id, and try to connect to the default server if any. If several
     * servers are available, let the user choose.
     *
     * @return the view_id of the contextually computed startup page
     */
    public String initServerAndFindStartupPage() {

        setupCurrentUser();

        // we try to select the server to go to the next screen
        if (navigationContext.getCurrentServerLocation() == null) {
            // update location
            RepositoryLocation repLoc = new RepositoryLocation(repositoryManager.getDefaultRepositoryName());
            navigationContext.setCurrentServerLocation(repLoc);
        }

        if (documentManager == null) {
            documentManager = navigationContext.getOrCreateDocumentManager();
        }

        if (Events.exists()) {
            Events.instance().raiseEvent(EventNames.USER_SESSION_STARTED, documentManager);
        }

        // select home page

        DocumentModel rootDocument = documentManager.getRootDocument();
        if (!documentManager.hasPermission(rootDocument.getRef(), SecurityConstants.READ_CHILDREN)) {
            // user cannot see the root but maybe she can see contained
            // documents thus forwarding her to her dashboard
            return dashboardNavigationHelper.navigateToDashboard();
        }

        webActions.setCurrentTabIds(DOCUMENT_MANAGEMENT_TAB);
        // if more than one repo : display the server selection screen
        if (repositoryManager.getRepositoryNames().size() > 1) {
            return SERVERS_VIEW;
        }

        // the Repository Location is initialized, skip the first screen
        return DOMAINS_VIEW;
    }

    /**
     * Initializes the context with the principal id, and tries to connect to the default server if any then: - if the
     * server has several domains, redirect to the list of domains - if the server has only one domain, select it and
     * redirect to viewId - if the server is empty, create a new domain with title 'domainTitle' and redirect to it on
     * viewId.
     * <p>
     * If several servers are available, let the user choose.
     *
     * @return the view id of the contextually computed startup page
     */
    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}", join = true)
    public String initDomainAndFindStartupPage(String domainTitle, String viewId) {

        try {
            // delegate server initialized to the default helper
            String result = initServerAndFindStartupPage();

            // more than one repo
            if (SERVERS_VIEW.equals(result)) {
                return result;
            }

            String query = "SELECT * FROM Domain WHERE " + NXQL.ECM_MIXINTYPE + " <> '"
                    + FacetNames.HIDDEN_IN_NAVIGATION + "' AND " + NXQL.ECM_LIFECYCLESTATE + " <> '"
                    + LifeCycleConstants.DELETED_STATE + "'" + " AND ecm:isCheckedInVersion = 0 "
                    + " AND ecm:isProxy = 0 ";
            DocumentModelList domains = documentManager.query(query);
            if (domains.size() == 1) {
                // select and go to the unique domain
                webActions.setCurrentTabIds(DOCUMENT_MANAGEMENT_TAB);
                return navigationContext.navigateToDocument(domains.get(0), viewId);
            }

            // zero or several domains: let the user decide what to do if he has
            // right on the Root document
            DocumentModel rootDocument = documentManager.getRootDocument();
            if (documentManager.hasPermission(rootDocument.getRef(), SecurityConstants.READ_CHILDREN)) {
                webActions.setCurrentTabIds(DOCUMENT_MANAGEMENT_TAB);
                navigationContext.navigateToDocument(rootDocument);
                return DOMAINS_VIEW;
            }

            return result;
        } catch (NuxeoException e) {
            // avoid pages.xml contribution to catch exceptions silently
            // hiding the cause of the problem to developers
            // TODO: remove this catch clause if we find a way not to make it
            // fail silently
            log.error("error while initializing the Seam context with a CoreSession instance: " + e.getMessage(), e);
            return null;
        }
    }

    public void setupCurrentUser() {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        sessionContext.set("currentUser", currentUser);
    }

}
