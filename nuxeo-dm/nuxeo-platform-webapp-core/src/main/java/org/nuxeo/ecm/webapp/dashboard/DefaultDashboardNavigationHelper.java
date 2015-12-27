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

package org.nuxeo.ecm.webapp.dashboard;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

@Name("dashboardNavigationHelper")
@Scope(ScopeType.STATELESS)
@Install(precedence = Install.FRAMEWORK)
public class DefaultDashboardNavigationHelper implements DashboardNavigationHelper {

    public static final String HOME_TAB = "MAIN_TABS:home";

    public static final String DASHBOARD_VIEW = "view_home";

    @In(create = true)
    protected transient WebActions webActions;

    @Override
    public String navigateToDashboard() {
        webActions.setCurrentTabIds(HOME_TAB);
        return DASHBOARD_VIEW;
    }

}
