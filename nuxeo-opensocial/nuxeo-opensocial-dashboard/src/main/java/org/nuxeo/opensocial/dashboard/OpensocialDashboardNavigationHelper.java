package org.nuxeo.opensocial.dashboard;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.runtime.api.Framework;

@Name("dashboardNavigationHelper")
@Scope(ScopeType.SESSION)
public class OpensocialDashboardNavigationHelper implements
        DashboardNavigationHelper {

    public static final String OLD_DASHBARD_VIEWID = "user_dashboard";

    public static final String NEW_DASHBARD_VIEWID = "opensocial_dashboard";

    public static final String DASHBARD_MODE_PROPERTY = "org.nuxeo.ecm.webapp.dashboard.mode";

    public static final String DASHBARD_MODE_AUTO = "auto";

    public static final String DASHBARD_MODE_OS = "opensocial";

    public static final String DASHBARD_MODE_OLD = "old";

    public static final String SELENIUM_USERAGENT = "Nuxeo-Selenium-Tester";

    public static final String MSIE_USERAGENT = "MSIE";
    public static final String MSIE7_USERAGENT = "MSIE 7.";
    public static final String MSIE8_USERAGENT = "MSIE 8.";
    public static final String SAFARI_USERAGENT = "Safari";

    private static final Log log = LogFactory.getLog(OpensocialDashboardNavigationHelper.class);

    protected String dashBoardViewId = null;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    protected boolean personnalDashboardCreated = false;

    public String navigateToDashboard() {
        createPersonnalDashbordIfNeeded();
        return computeDashbordView();
    }

    protected void createPersonnalDashbordIfNeeded() {
        if (!personnalDashboardCreated) {
            String containerId = getPersonalDashboardContainerIdRef();
            // creation if neeeded !!!
        }
    }

    @Factory(value = "personalDashboardContainerIdRef", scope = ScopeType.SESSION)
    public String getPersonalDashboardContainerIdRef() {
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
                        documentManager).getId();
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

    protected String computeDashbordView() {
        if (dashBoardViewId == null) {
            String userAgent = null;
            FacesContext fContext = FacesContext.getCurrentInstance();
            if (fContext == null) {
                log.error("unable to fetch facesContext, can not detect client type");
            } else {
                userAgent = fContext.getExternalContext().getRequestHeaderMap().get(
                        "User-Agent");
            }

            // force old dashboard for Selenium tests
            if (userAgent != null && userAgent.contains(SELENIUM_USERAGENT)) {
                return OLD_DASHBARD_VIEWID;
            }

            String mode = Framework.getProperty(DASHBARD_MODE_PROPERTY,
                    DASHBARD_MODE_AUTO);
            if (DASHBARD_MODE_AUTO.equals(mode)) {
                // force old dashboard for MSIE
                if (userAgent != null && userAgent.contains(MSIE_USERAGENT)) {

                    if (userAgent.contains(MSIE7_USERAGENT)) {
                        dashBoardViewId = NEW_DASHBARD_VIEWID;
                    }
                    else if (userAgent.contains(MSIE8_USERAGENT)) {
                        dashBoardViewId = NEW_DASHBARD_VIEWID;
                    } else {
                        // IE 4, IE 5 , IE 5.5, IE6
                        dashBoardViewId = OLD_DASHBARD_VIEWID;
                    }
                }
                else if (userAgent != null && userAgent.contains(SAFARI_USERAGENT)) {
                    // Safari work only when sending MSIE or FF UserAgent to GWT and RichFaces
                    dashBoardViewId = OLD_DASHBARD_VIEWID;
                }
                else {
                    dashBoardViewId = NEW_DASHBARD_VIEWID;
                }
            } else if (DASHBARD_MODE_OS.equals(mode)) {
                dashBoardViewId = NEW_DASHBARD_VIEWID;
            } else if (DASHBARD_MODE_OLD.equals(mode)) {
                dashBoardViewId = OLD_DASHBARD_VIEWID;
            }
        }
        return dashBoardViewId;
    }
}
