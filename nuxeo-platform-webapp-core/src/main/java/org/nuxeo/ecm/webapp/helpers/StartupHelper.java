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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.security.Principal;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;

@Name("startupHelper")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
public class StartupHelper implements Serializable {

    public static final String LANGUAGE_PARAMETER = "language";

    protected static final String SERVERS_VIEW = "view_servers";

    protected static final String DOMAINS_VIEW = "view_domains";

    protected static final String DOMAIN_TYPE = "Domain";

    private static final long serialVersionUID = 3248972387619873245L;

    private static final Log log = LogFactory.getLog(StartupHelper.class);

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    @In(create = true)
    ConversationIdGenerator conversationIdGenerator;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    /**
     * Initializes the context with the principal id, and try to connect to the
     * default server if any. If several servers are available, let the user
     * choose.
     *
     * @return the view_id of the contextually computed startup page
     */
    public String initServerAndFindStartupPage() throws ClientException {

        setupCurrentUser();

        // we try to select the server to go to the next screen
        if (navigationContext.getCurrentServerLocation() == null) {
            // update location
            RepositoryLocation repLoc = new RepositoryLocation(
                    repositoryManager.getRepositories().iterator().next().getName());
            navigationContext.setCurrentServerLocation(repLoc);
        }

        // if more than one repo : display the server selection screen
        if (repositoryManager.getRepositories().size() > 1) {
            return SERVERS_VIEW;
        }

        // the Repository Location is initialized, skip the first screen
        return DOMAINS_VIEW;
    }

    /**
     * Initializes the context with the principal id, and tries to connect to
     * the default server if any then: - if the server has several domains,
     * redirect to the list of domains - if the server has only one domain,
     * select it and redirect to viewId - if the server is empty, create a new
     * domain with title 'domainTitle' and redirect to it on viewId.
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

            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

            if (request.getParameter(LANGUAGE_PARAMETER) != null) {
                String localeStr = request.getParameter(LANGUAGE_PARAMETER);
                localeSelector.setLocaleString(localeStr);
            }

            if (!DOMAINS_VIEW.equals(result)) {
                // we're not redirecting to the domains view. Don't initialize
                // further,
                // we assume it has been done or something went wrong
                return result;
            }
            if (documentManager == null) {
                documentManager = navigationContext.getOrCreateDocumentManager();
            }

            // get the domains from selected server
            DocumentModel rootDocument = documentManager.getRootDocument();

            if (!documentManager.hasPermission(rootDocument.getRef(),
                    SecurityConstants.READ_CHILDREN)) {
                // user cannot see the root but maybe she can see contained
                // documents thus forwarding her to her dashboard
                return dashboardNavigationHelper.navigateToDashboard();
            }

            FacetFilter facetFilter = new FacetFilter(
                    FacetNames.HIDDEN_IN_NAVIGATION, false);
            LifeCycleFilter lcFilter = new LifeCycleFilter(
                    ClipboardActionsBean.DELETED_LIFECYCLE_STATE, false);
            CompoundFilter complexFilter = new CompoundFilter(facetFilter,
                    lcFilter);
            DocumentModelList domains = documentManager.getChildren(
                    rootDocument.getRef(), null, SecurityConstants.READ,
                    complexFilter, null);

            if (domains.size() == 1) {
                // select and go to the unique domain
                return navigationContext.navigateToDocument(domains.get(0),
                        viewId);
            }

            // zero or several domains: let the user decide what to do
            navigationContext.navigateToDocument(rootDocument);
            return DOMAINS_VIEW;
        } catch (ClientException e) {
            // avoid pages.xml contribution to catch exceptions silently
            // hiding the cause of the problem to developers
            // TODO: remove this catch clause if we find a way not to make it
            // fail silently
            log.error(
                    "error while initializing the Seam context with a CoreSession instance: "
                            + e.getMessage(), e);
            return null;
        }
    }

    public void setupCurrentUser() {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        sessionContext.set("currentUser", currentUser);
    }

}
