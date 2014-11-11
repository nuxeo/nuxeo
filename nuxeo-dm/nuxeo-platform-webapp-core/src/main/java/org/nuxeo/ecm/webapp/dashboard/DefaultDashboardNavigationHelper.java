package org.nuxeo.ecm.webapp.dashboard;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("dashboardNavigationHelper")
@Scope(ScopeType.STATELESS)
@Install(precedence = Install.FRAMEWORK)
public class DefaultDashboardNavigationHelper implements
        DashboardNavigationHelper {

    public static final String DASHBOARD_VIEW = "user_dashboard";

    public String navigateToDashboard() {
        return DASHBOARD_VIEW;
    }

}
