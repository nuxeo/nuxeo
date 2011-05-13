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

package org.nuxeo.ecm.webapp.dashboard;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;import org.nuxeo.ecm.platform.ui.web.api.WebActions;

@Name("dashboardNavigationHelper")
@Scope(ScopeType.STATELESS)
@Install(precedence = Install.FRAMEWORK)
public class DefaultDashboardNavigationHelper implements
        DashboardNavigationHelper {

    public static final String HOME_TAB = "MAIN_TABS:home";

    public static final String DASHBOARD_VIEW = "view_home";

    @In(create = true)
    protected transient WebActions webActions;

    public String navigateToDashboard() {
        webActions.setCurrentTabIds(HOME_TAB);
        return DASHBOARD_VIEW;
    }

}
