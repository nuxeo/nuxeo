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
 *     Nuxeo
 */

package org.nuxeo.dam.platform.context;

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
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.context.ServerContextBean;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

@Name("startupHelper")
@Scope(SESSION)
@Install(precedence = FRAMEWORK)
public class StartupHelper implements Serializable {

    private static final long serialVersionUID = 3248972387619873245L;

    public static final String LANGUAGE_PARAMETER = "language";

    public static final String ASSETS_VIEW = "assets";

    protected static final Log log = LogFactory.getLog(StartupHelper.class);

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In
    protected transient Context sessionContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient Context conversationContext;

    @In
    protected ServerContextBean serverLocator;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    @In(create = true)
    protected transient NavigationContext navigationContext;

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
    public String initDomainAndFindStartupPage() {

        setupCurrentUser();

        RepositoryLocation repLoc = new RepositoryLocation(
                repositoryManager.getRepositories().iterator().next().getName());
        serverLocator.setRepositoryLocation(repLoc);

        if (documentManager == null) {
            try {
                documentManager = navigationContext.getOrCreateDocumentManager();
            } catch (ClientException e) {
                // avoid pages.xml contribution to catch exceptions silently
                // hiding the cause of the problem to developers
                // TODO: remove the catch clause if we find a way not to make it
                // fail silently
                log.error(
                        "error while opening CoreSession to init Seam context: "
                                + e.getMessage(), e);
                return null;
            }
        }

        // DAM-173 - Set locale from login page.
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request.getParameter(LANGUAGE_PARAMETER) != null) {
            String localeStr = request.getParameter(LANGUAGE_PARAMETER);
            localeSelector.selectLanguage(localeStr);
            localeSelector.setLocaleString(localeStr);
        }

        return ASSETS_VIEW;
    }

    public void setupCurrentUser() {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        sessionContext.set("currentUser", currentUser);
    }

}
