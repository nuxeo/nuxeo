/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.connect.client.jsf;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.ecm.admin.AdminViewManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean used to build/restore a JSF/Seam context from a REST call. This
 * bean is called when a user clicks on an installation link from MarketPlace.
 *
 * @author tiry
 */
@Name("externalLinkManager")
@Scope(SESSION)
public class ExternalLinkManager implements Serializable {

    private static final long serialVersionUID = 1L;

    @RequestParameter
    protected String packageId;

    protected DownloadablePackage pkg;

    @In(create = true, required = false)
    protected AdminViewManager adminViews;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    protected static NuxeoPrincipal getUser() {
        return (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
    }

    protected void setupCurrentUser() {
        sessionContext.set("currentUser", getUser());
    }

    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}", join = true)
    public String startDownload() {
        if (packageId == null) {
            return null;
        }

        PackageManager pm = Framework.getLocalService(PackageManager.class);
        pkg = pm.getPackage(packageId);

        if (getUser().isAdministrator() && pkg != null) {
            return "confirm_download";
        } else {
            return "can_not_download";
        }
    }

    public DownloadablePackage getPkg() {
        return pkg;
    }

    protected void initMinimalContext() throws Exception {
        setupCurrentUser();

        // we try to select the server to go to the next screen
        if (navigationContext.getCurrentServerLocation() == null) {
            // update location
            RepositoryLocation repLoc = new RepositoryLocation(
                    repositoryManager.getRepositories().iterator().next().getName());
            navigationContext.setCurrentServerLocation(repLoc);
        }
        CoreSession documentManager = navigationContext.getOrCreateDocumentManager();
        DocumentModelList domains = documentManager.query("select * from Domain");
        navigationContext.setCurrentDocument(domains.get(0));
    }

    public String confirm() throws Exception {
        initMinimalContext();

        adminViews.setCurrentViewId("ConnectApps");
        adminViews.setCurrentSubViewId("ConnectAppsRemote");

        adminViews.addExternalPackageDownloadRequest(pkg.getId());

        return "view_admin";
    }

}
