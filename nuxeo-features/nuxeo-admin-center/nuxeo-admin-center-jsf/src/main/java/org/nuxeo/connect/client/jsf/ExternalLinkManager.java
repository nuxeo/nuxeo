/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean used to build/restore a JSF/Seam context from a REST call. This bean is called when a user clicks on an
 * installation link from MarketPlace.
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
    protected WebActions webActions;

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

        PackageManager pm = Framework.getService(PackageManager.class);
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

    protected void initMinimalContext() {
        setupCurrentUser();

        // we try to select the server to go to the next screen
        if (navigationContext.getCurrentServerLocation() == null) {
            // update location
            RepositoryLocation repLoc = new RepositoryLocation(repositoryManager.getRepositoryNames().get(0));
            navigationContext.setCurrentServerLocation(repLoc);
        }
        CoreSession documentManager = navigationContext.getOrCreateDocumentManager();
        DocumentModelList domains = documentManager.query("select * from Domain");
        navigationContext.setCurrentDocument(domains.get(0));
    }

    public String confirm() {
        initMinimalContext();
        webActions.setCurrentTabId(AdminViewManager.ADMIN_ACTION_CATEGORY, "ConnectApps", "ConnectAppsRemote");
        adminViews.addExternalPackageDownloadRequest(pkg.getId());
        return AdminViewManager.VIEW_ADMIN;
    }

}
