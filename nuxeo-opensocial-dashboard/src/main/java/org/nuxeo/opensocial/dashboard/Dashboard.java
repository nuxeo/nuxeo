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

package org.nuxeo.opensocial.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.runtime.api.Framework;

@Name("opensocialDashboard")
@Scope(ScopeType.SESSION)
@Startup
public class Dashboard implements Serializable {
    private static final long serialVersionUID = 8748161330761041337L;

    private static final Log log = LogFactory.getLog(Dashboard.class);

    protected String selectedDomainId;

    protected DocumentModelList domains = null;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    protected DocumentModel lastAccessedDocument;

    public boolean isAnonymous() {
        NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
        return principal.isAnonymous();
    }

    @Factory(value = "opensocialNuxeoServerUrl", scope = ScopeType.APPLICATION)
    public String getNuxeoServerUrl() {
        String host = Framework.getProperty("gadgets.host", "127.0.0.1");
        String port = Framework.getProperty("gadgets.port", "8080");
        return "http://" + host + ":" + port + "/";
    }

    public String goToDashBoard() {
        String dashboardViewId = dashboardNavigationHelper.navigateToDashboard();
        if (OpensocialDashboardNavigationHelper.NEW_DASHBARD_VIEWID.equals(dashboardViewId)) {
            lastAccessedDocument = navigationContext.getCurrentDocument();
        }
        return dashboardViewId;
    }

    public String exit() throws Exception {
        if (lastAccessedDocument == null) {
            return navigationContext.goHome();
        } else {
            return navigationContext.navigateToDocument(lastAccessedDocument);
        }
    }

    public String getSelectedDomain() throws ClientException {
        if (selectedDomainId == null) {
            selectedDomainId = getCurrentDashboardDomainName();
        }
        return selectedDomainId;
    }

    public void setSelectedDomain(String selectedDomain) {
        this.selectedDomainId = selectedDomain;
    }

    public List<SelectItem> getDomainsSelectItems() throws ClientException {
        List<SelectItem> items = new ArrayList<SelectItem>();

        SelectItem rootitem = new SelectItem("*", "All");
        items.add(rootitem);

        DocumentModelList domains = getAccessibleDomains();
        for (DocumentModel domain : domains) {
            SelectItem item = new SelectItem(domain.getName(),
                    domain.getTitle());
            items.add(item);
        }
        return items;
    }

    protected DocumentModelList getAccessibleDomains() throws ClientException {
        if (domains == null) {
            domains = documentManager.query("select * from Domain order by dc:created");
        }
        return domains;
    }

    public String submitSelectedDomainChange() throws ClientException {
        return null;
    }

    public String getCurrentDashboardDomainName() throws ClientException {
        if (selectedDomainId == null) {
            DocumentModel currentDomain = navigationContext.getCurrentDomain();
            if (currentDomain == null) {
                DocumentModelList domains = getAccessibleDomains();
                if (domains.size() > 0) {
                    currentDomain = domains.get(0);
                }
            }
            if (currentDomain == null) {
                return "*";
            } else {
                return currentDomain.getName();
            }
        } else {
            return selectedDomainId;
        }
    }

    public String initializeSpace(String spaceName, String spaceProviderName)
            throws Exception {
        SpaceManager spaceManager = Framework.getService(SpaceManager.class);
        Space space = spaceManager.getSpace(spaceProviderName,
                navigationContext.getCurrentDocument(), spaceName);
        return space.getId();
    }

    public String getSpaceId(String spaceName, String spaceProviderName,
            DocumentModel contextDocument) throws Exception {
        SpaceManager spaceManager = Framework.getService(SpaceManager.class);

        Space space = spaceManager.getSpace(spaceProviderName, contextDocument,
                spaceName);

        return space.getId();
    }

}
