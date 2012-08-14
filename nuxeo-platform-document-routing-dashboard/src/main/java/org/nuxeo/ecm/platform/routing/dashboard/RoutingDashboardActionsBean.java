/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.platform.routing.dashboard;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.TabActionsSelection;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * Seam Bean used to manage navigation inside the Workflow dashboard.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 */
// named "routing" instead of "workflow" to avoid confusion with old jbpm Seam
// component
@Name("routingDashboardActions")
@Scope(CONVERSATION)
public class RoutingDashboardActionsBean implements Serializable {

    private static final long serialVersionUID = 6559435574355707710L;

    public static final String WORKFLOW_ACTION_CATEGORY = "WORKFLOW_DASHBOARD";

    public static final String VIEW_WORKFLOW = "view_workflow";

    @In(create = true, required = false)
    protected WebActions webActions;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected DocumentModel lastVisitedDocument;

    public String goHome() {
        webActions.resetCurrentTabs(WORKFLOW_ACTION_CATEGORY);
        Contexts.getEventContext().remove("currentView");
        Contexts.getEventContext().remove("currentWorkflowSubView");
        return VIEW_WORKFLOW;
    }

    public String enter() {
        lastVisitedDocument = navigationContext.getCurrentDocument();
        return VIEW_WORKFLOW;
    }

    public String exit() throws ClientException {
        if (lastVisitedDocument != null) {
            return navigationContext.navigateToDocument(lastVisitedDocument);
        } else {
            return navigationContext.goHome();
        }
    }

    @Factory(value = "currentWorkflowView", scope = ScopeType.EVENT)
    public Action getCurrentView() {
        return webActions.getCurrentTabAction(WORKFLOW_ACTION_CATEGORY);
    }

    public void setCurrentView(Action currentView) {
        webActions.setCurrentTabAction(WORKFLOW_ACTION_CATEGORY, currentView);
    }

    public String getCurrentViewId() {
        return getCurrentView().getId();
    }

    public String setCurrentViewId(String currentViewId) {
        webActions.setCurrentTabId(WORKFLOW_ACTION_CATEGORY, currentViewId);
        return VIEW_WORKFLOW;
    }

    @Factory(value = "currentWorkflowSubView", scope = ScopeType.EVENT)
    public Action getCurrentSubView() {
        return webActions.getCurrentSubTabAction(getCurrentViewId());
    }

    public void setCurrentSubView(Action currentSubView) {
        webActions.setCurrentTabAction(
                TabActionsSelection.getSubTabCategory(getCurrentViewId()),
                currentSubView);
    }

    @Factory(value = "currentWorkflowSubViewId", scope = ScopeType.EVENT)
    public String getCurrentSubViewId() {
        return getCurrentSubView().getId();
    }

    public void setCurrentSubViewId(String currentSubViewId) {
        webActions.setCurrentTabId(
                TabActionsSelection.getSubTabCategory(getCurrentViewId()),
                currentSubViewId);
    }

    public List<Action> getAvailableActions() {
        return webActions.getActionsList(WORKFLOW_ACTION_CATEGORY);
    }

    public List<Action> getAvailableSubActions() {
        return webActions.getActionsList(TabActionsSelection.getSubTabCategory(getCurrentViewId()));
    }

}
