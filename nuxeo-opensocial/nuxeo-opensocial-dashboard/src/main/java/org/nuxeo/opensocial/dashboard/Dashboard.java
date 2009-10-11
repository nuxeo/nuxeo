package org.nuxeo.opensocial.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
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

    protected DocumentModelList domains=null;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(required = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    protected DocumentModel lastAccessedDocument;

    public boolean isAnonymous() {
        NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
        return principal.isAnonymous();
    }

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

    @Factory(value="opensocialNuxeoServerUrl", scope = ScopeType.APPLICATION)
    public String getNuxeoServerUrl() {
        String host = Framework.getProperty("gadgets.host","127.0.0.1");
        String port = Framework.getProperty("gadgets.port","8080");

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
        if (lastAccessedDocument==null) {
            return navigationContext.goHome();
        } else {
            return navigationContext.navigateToDocument(lastAccessedDocument);
        }
    }

    public List<SelectItem> getDomainsSelectItems() throws ClientException {

        List<SelectItem> items = new ArrayList<SelectItem>();
        DocumentModelList domains = getAccessibleDomains();

        for (DocumentModel domain : domains) {
            SelectItem item = new SelectItem(domain.getName(), domain.getTitle());
            items.add(item);
        }
        return items;
    }

    protected DocumentModelList getAccessibleDomains() throws ClientException {
        if (domains==null) {
            domains = documentManager.query("select * from Domain order by dc:created");
        }
        return domains;
    }

    public String getCurrentDashboardDomainName() throws ClientException {

        DocumentModel currentDomain = navigationContext.getCurrentDomain();
        if (currentDomain==null) {
            DocumentModelList domains = getAccessibleDomains();
            if (domains.size()>0) {
                currentDomain = domains.get(0);
            }
        }
        if (currentDomain==null) {
            return "";
        } else {
            return currentDomain.getName();
        }
    }


}
