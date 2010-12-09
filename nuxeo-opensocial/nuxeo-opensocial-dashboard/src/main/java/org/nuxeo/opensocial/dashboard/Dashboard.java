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

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

@Name("opensocialDashboard")
@Scope(ScopeType.SESSION)
@Startup
public class Dashboard implements Serializable {
    private static final long serialVersionUID = 8748161330761041337L;

    private static final Log log = LogFactory.getLog(Dashboard.class);

    public static final String OLD_DASHBARD_VIEWID = "user_dashboard";

    public static final String NEW_DASHBARD_VIEWID = "opensocial_dashboard";

    public static final String DASHBARD_MODE_PROPERTY = "org.nuxeo.ecm.webapp.dashboard.viewid";

    public static final String SELENIUM_USERAGENT = "Nuxeo-Selenium-Tester";

    protected String selectedDomainId;

    protected DocumentModelList domains = null;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    protected DocumentModel lastAccessedDocument;

    public String getPersonalDashboardId() {
        try {
            return DashboardSpaceProvider.getSpaceId(documentManager);
        } catch (SpaceException e) {
            log.error("Unable to access space correctly for our dashboard!", e);
        } catch (Exception e) {
            log.error("Error attempting to find the SpaceManager!", e);
        }
        return null;
    }

    public boolean isAnonymous() {
        NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
        return principal.isAnonymous();
    }

    @Factory(value = "dashboardGadgetCategories", scope = EVENT)
    public List<String> getCategories() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            FactoryManager factoryManager = Framework.getService(FactoryManager.class);
            for (String s : factoryManager.getContainerFactory().getGadgetList().keySet()) {
                result.add(s);
            }
        } catch (Exception e) {
            log.error("Unable to find factory or container manager!", e);
        }
        return result;
    }

    @Factory(value = "dashboardGadgets", scope = EVENT)
    public List<Map<String, String>> getGadgets() {
        ArrayList<Map<String, String>> result = new ArrayList<Map<String, String>>();

        try {
            FactoryManager factoryManager = Framework.getService(FactoryManager.class);
            GadgetService gadgetService = Framework.getService(GadgetService.class);
            Map<String, ArrayList<String>> gadgetList = factoryManager.getContainerFactory().getGadgetList();
            for (String category : gadgetList.keySet()) {
                ArrayList<String> inCategory = gadgetList.get(category);
                for (String gadgetName : inCategory) {
                    HashMap<String, String> gadgetText = new HashMap<String, String>();
                    gadgetText.put("name", gadgetName);
                    gadgetText.put("category", category);
                    GadgetDeclaration gadget = gadgetService.getGadget(gadgetName);
                    /*
                     * gadgetText.put("iconUrl",
                     * gadget.getIconUrl().toURI().toString());
                     */

                    gadgetText.put("iconUrl", gadget.getIconUrl());

                    result.add(gadgetText);
                }
            }

        } catch (Exception e) {
            log.error("Unable to find factory or container manager!", e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "opensocial dashboard:" + getCategories() + " categories";
    }

    @Factory(value = "opensocialNuxeoServerUrl", scope = ScopeType.APPLICATION)
    public String getNuxeoServerUrl() {
        String host = Framework.getProperty("gadgets.host", "127.0.0.1");
        String port = Framework.getProperty("gadgets.port", "8080");
        return "http://" + host + ":" + port + "/";
    }

    /** used for debug */
    public void dumpDocumentInfo(String docId) {
        try {
            DocumentRef ref = new IdRef(docId);
            DocumentModel model = documentManager.getDocument(ref);
            log.info("------------- doc id " + docId + " ---------------------");
            log.info("document type:" + model.getType());
            log.info("document path:" + model.getPathAsString());
            String[] schemaNames = model.getSchemas();
            for (String schemaName : schemaNames) {
                log.info("dumping schema:" + schemaName + " ++++++++++++++");
                Map<String, Object> propMap = model.getProperties(schemaName);
                for (String propName : propMap.keySet()) {
                    if (propMap.get(propName) == null) {
                        log.info("" + propName + "  ---> NULL");
                    } else {
                        if (propMap.get(propName) instanceof GregorianCalendar) {
                            GregorianCalendar calendar = (GregorianCalendar) propMap.get(propName);
                            log.info("" + propName + " ---> "
                                    + calendar.getTimeInMillis());
                        } else {
                            String propValue = propMap.get(propName).toString();
                            log.info("" + propName + " ---> " + propValue);
                        }
                    }
                }
            }
            if (model.getType().equals("Space")) {
                DocumentModelList list = documentManager.query("SELECT * FROM Document WHERE ecm:path "
                        + "STARTSWITH '" + model.getPathAsString() + "'");
                for (Iterator<DocumentModel> iterator = list.iterator(); iterator.hasNext();) {
                    DocumentModel documentModel = iterator.next();
                    dumpDocumentInfo(documentModel.getId());
                }
            }

        } catch (ClientException e) {
            log.error("unable to dump document information!", e);
        }
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

}
