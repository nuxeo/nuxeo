package org.nuxeo.opensocial.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.runtime.api.Framework;

@Name("opensocialDashboard")
@Scope(ScopeType.SESSION)
@Startup
public class Dashboard implements Serializable {
    private static final long serialVersionUID = 8748161330761041337L;

    private static final Log log = LogFactory.getLog(Dashboard.class);

    @In(create = true, required = false)
    CoreSession documentManager;

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
            Map<String, ArrayList<String>> gadgetList = factoryManager.getContainerFactory().getGadgetList();
            for (String category : gadgetList.keySet()) {
                ArrayList<String> inCategory = gadgetList.get(category);
                for (String gadgetName : inCategory) {
                    HashMap<String, String> gadgetText = new HashMap<String, String>();
                    gadgetText.put("name", gadgetName);
                    gadgetText.put("category", category);
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

}
