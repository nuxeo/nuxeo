package org.nuxeo.opensocial.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
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

    protected String dashBoardViewId = null;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(required = true)
    protected transient NavigationContext navigationContext;

    protected DocumentModel lastAccessedDocument;

    public String getPersonalDashboardId() {
        SpaceManager spaceManager;
        try {
            spaceManager = Framework.getService(SpaceManager.class);
            if (spaceManager == null) {
                log.warn("unable to find space manager!");
            } else {

                Univers universe;
                universe = spaceManager.getUnivers(
                        DashboardUniverseProvider.DASHBOARD_UNIVERSE_NAME,
                        documentManager);
                return spaceManager.getSpace(
                        DashboardSpaceProvider.DASHBOARD_SPACE_NAME, universe,
                        documentManager).getId().toString();
            }
        } catch (UniversNotFoundException e) {
            log.error("Unable to find the default universe for our space!", e);
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
        if (dashBoardViewId==null) {
            String userAgent =null;
            FacesContext fContext = FacesContext.getCurrentInstance();
            if (fContext == null) {
                log.error("unable to fetch facesContext, can not detect client type");
            } else {
                userAgent = fContext.getExternalContext().getRequestHeaderMap().get("User-Agent");
            }

            if (userAgent!=null && userAgent.contains(SELENIUM_USERAGENT)) {
                dashBoardViewId =  OLD_DASHBARD_VIEWID;
            }
            else {
                dashBoardViewId = Framework.getProperty(DASHBARD_MODE_PROPERTY, NEW_DASHBARD_VIEWID);
            }
        }

        if (NEW_DASHBARD_VIEWID.equals(dashBoardViewId)) {
            lastAccessedDocument = navigationContext.getCurrentDocument();
        }

        return dashBoardViewId;
    }


    public String exit() throws Exception {

        if (lastAccessedDocument==null) {
            return navigationContext.goHome();
        } else {
            return navigationContext.navigateToDocument(lastAccessedDocument);
        }
    }

}
