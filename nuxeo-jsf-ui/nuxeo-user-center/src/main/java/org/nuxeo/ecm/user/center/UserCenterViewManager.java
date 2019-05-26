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

package org.nuxeo.ecm.user.center;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.TabActionsSelection;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * Seam Bean used to manage navigation inside the User Center.
 *
 * @author tiry
 */
@Name("userCenterViews")
@Scope(CONVERSATION)
public class UserCenterViewManager implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String USER_CENTER_ACTION_CATEGORY = "USER_CENTER";

    public static final String VIEW_HOME = "view_home";

    @In(create = true, required = false)
    protected WebActions webActions;

    @Factory(value = "currentUserCenterView", scope = ScopeType.EVENT)
    public Action getCurrentView() {
        return webActions.getCurrentTabAction(USER_CENTER_ACTION_CATEGORY);
    }

    public void setCurrentView(Action currentView) {
        webActions.setCurrentTabAction(USER_CENTER_ACTION_CATEGORY, currentView);
    }

    public String getCurrentViewId() {
        return getCurrentView().getId();
    }

    public String setCurrentViewId(String currentViewId) {
        webActions.setCurrentTabId(USER_CENTER_ACTION_CATEGORY, currentViewId);
        return VIEW_HOME;
    }

    @Factory(value = "currentUserCenterSubView", scope = ScopeType.EVENT)
    public Action getCurrentSubView() {
        return webActions.getCurrentSubTabAction(getCurrentViewId());
    }

    public void setCurrentSubView(Action currentSubView) {
        webActions.setCurrentTabAction(TabActionsSelection.getSubTabCategory(getCurrentViewId()), currentSubView);
    }

    @Factory(value = "currentUserCenterSubViewId", scope = ScopeType.EVENT)
    public String getCurrentSubViewId() {
        return getCurrentSubView().getId();
    }

    public void setCurrentSubViewId(String currentSubViewId) {
        webActions.setCurrentTabId(TabActionsSelection.getSubTabCategory(getCurrentViewId()), currentSubViewId);
    }

    public List<Action> getAvailableActions() {
        return webActions.getActionsList(USER_CENTER_ACTION_CATEGORY);
    }

    public List<Action> getAvailableSubActions() {
        return webActions.getActionsList(TabActionsSelection.getSubTabCategory(getCurrentViewId()));
    }

    public String navigateTo(Action action) {
        setCurrentView(action);
        return VIEW_HOME;
    }

}
