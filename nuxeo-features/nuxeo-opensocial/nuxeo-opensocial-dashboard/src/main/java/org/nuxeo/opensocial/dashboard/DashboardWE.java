package org.nuxeo.opensocial.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.runtime.api.Framework;

/*
 @WebObject(type = "Dashboard")
 @Produces("text/html; charset=UTF-8")
 */
public class DashboardWE extends ModuleRoot {
    private static final Log log = LogFactory.getLog(Dashboard.class);

    // protected GadgetManager gadgetManager;

    // protected ContainerManager containerManager;

    protected SpaceManager spaceManager;

    protected Space dashboard;

    // protected Container container;

    protected CoreSession session;

    /*
     * protected CoreSession getSession() { return
     * WebEngine.getActiveContext().getCoreSession(); }
     */

    // @Override
    @Create
    public void initialize(/* Object... args */) {
        /*
         * try { FactoryManager factoryManager =
         * Framework.getService(FactoryManager.class); GadgetManager
         * gadgetManager = factoryManager.getGadgetFactory();
         * log.info("gadget manager ok? " + (gadgetManager != null));
         * ContainerManager containerManager =
         * factoryManager.getContainerFactory();
         * log.info("container manager ok? " + (containerManager != null));
         */
        try {
            if (spaceManager == null) {
                spaceManager = Framework.getService(SpaceManager.class);

                if (spaceManager == null) {
                    log.warn("unable to find space manager!");
                } else {

                    Univers universe;
                    universe = spaceManager.getUnivers(
                            DashboardUniverseProvider.DASHBOARD_UNIVERSE_NAME,
                            session);
                    dashboard = spaceManager.getSpace(
                            DashboardSpaceProvider.DASHBOARD_SPACE_NAME,
                            universe, session);
                }
            }
        } catch (UniversNotFoundException e) {
            log.error("Unable to find the default universe for our space!", e);
        } catch (SpaceException e) {
            log.error("Unable to access space correctly for our dashboard!", e);
        } catch (Exception e) {
            log.error("Error attempting to find the SpaceManager!", e);
        }

    }

    /*
     * @GET public View getView() { return new
     * View(WebEngine.getActiveContext(), "dashboard"); }
     */

    public Space getDashboard() {
        initialize();
        return dashboard;
    }

    // for script compatibility
    public Space getSpace() {
        initialize();
        return getDashboard();
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

    /*
     * @Path("/resources/{name:.*}")
     * 
     * @Produces("text/plain")
     * 
     * @GET public Object getResource(@PathParam("name") String name) throws
     * Exception { String path = "resources/" + name; InputStream file =
     * getClass().getClassLoader().getResourceAsStream(path); String itWontBeBig
     * = IOUtils.toString(file); return
     * javax.ws.rs.core.Response.ok(itWontBeBig).type("text/plain").build(); }
     */
}
