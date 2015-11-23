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

package org.nuxeo.ecm.user.center;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.actions.Action;
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

    protected Action currentView;

    protected Map<String, Action> currentSubViews = new HashMap<String, Action>();

    public static final String USER_CENTER_ACTION_CATEGORY = "USER_CENTER";

    @In(create = true, required = false)
    protected WebActions webActions;

    @Factory(value = "currentUserCenterView", scope = ScopeType.EVENT)
    public Action getCurrentView() {
        if (currentView == null) {
            List<Action> availableActions = getAvailableActions();
            if (!availableActions.isEmpty()) {
                currentView = availableActions.get(0);
            }
        }
        return currentView;
    }

    public void setCurrentView(Action currentView) {
        this.currentView = currentView;
    }

    public String getCurrentViewId() {
        return getCurrentView().getId();
    }

    public void setCurrentViewId(String currentViewId) {
        for (Action action : getAvailableActions()) {
            if (action.getId().equals(currentViewId)) {
                currentView = action;
                return;
            }
        }
    }

    @Factory(value = "currentUserCenterSubView", scope = ScopeType.EVENT)
    public Action getCurrentSubView() {
        if (currentSubViews.get(getCurrentViewId()) == null) {
            List<Action> availableSubActions = getAvailableSubActions();
            if (!availableSubActions.isEmpty()) {
                currentSubViews.put(getCurrentViewId(),
                        availableSubActions.get(0));
            }
        }
        return currentSubViews.get(getCurrentViewId());
    }

    public void setCurrentSubView(Action currentSubView) {
        currentSubViews.put(getCurrentViewId(), currentSubView);
    }

    @Factory(value = "currentUserCenterSubViewId", scope = ScopeType.EVENT)
    public String getCurrentSubViewId() {
        return getCurrentSubView().getId();
    }

    public void setCurrentSubViewId(String currentSubViewId) {
        for (Action action : getAvailableSubActions()) {
            if (action.getId().equals(currentSubViewId)) {
                setCurrentSubView(action);
                return;
            }
        }
    }

    public List<Action> getAvailableActions() {
        return webActions.getActionsList(USER_CENTER_ACTION_CATEGORY);
    }

    public List<Action> getAvailableSubActions() {
        return webActions.getActionsList(USER_CENTER_ACTION_CATEGORY + "_"
                + getCurrentViewId());
    }

    public String navigateTo(Action action) {
        setCurrentView(action);
        return "user_center";
    }

}
