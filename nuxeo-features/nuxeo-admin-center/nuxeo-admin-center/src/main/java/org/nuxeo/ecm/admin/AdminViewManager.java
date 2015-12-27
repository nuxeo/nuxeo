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

package org.nuxeo.ecm.admin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.TabActionsSelection;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean used to manage navigation inside the Admin Center.
 *
 * @author tiry
 */
@Name("adminViews")
@Scope(CONVERSATION)
public class AdminViewManager implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String externalPackageDownloadRequest;

    public static final String ADMIN_ACTION_CATEGORY = "NUXEO_ADMIN";

    public static final String VIEW_ADMIN = "view_admin";

    @In(create = true, required = false)
    protected WebActions webActions;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected DocumentModel lastVisitedDocument;

    public String goHome() {
        webActions.resetCurrentTabs(ADMIN_ACTION_CATEGORY);
        Contexts.getEventContext().remove("currentView");
        Contexts.getEventContext().remove("currentAdminSubView");
        return VIEW_ADMIN;
    }

    public String enter() {
        lastVisitedDocument = navigationContext.getCurrentDocument();
        return VIEW_ADMIN;
    }

    public String exit() {
        if (lastVisitedDocument != null) {
            return navigationContext.navigateToDocument(lastVisitedDocument);
        } else {
            return navigationContext.goHome();
        }
    }

    @Factory(value = "currentAdminView", scope = ScopeType.EVENT)
    public Action getCurrentView() {
        return webActions.getCurrentTabAction(ADMIN_ACTION_CATEGORY);
    }

    public void setCurrentView(Action currentView) {
        webActions.setCurrentTabAction(ADMIN_ACTION_CATEGORY, currentView);
    }

    public String getCurrentViewId() {
        return getCurrentView().getId();
    }

    public String setCurrentViewId(String currentViewId) {
        webActions.setCurrentTabId(ADMIN_ACTION_CATEGORY, currentViewId);
        return VIEW_ADMIN;
    }

    @Factory(value = "currentAdminSubView", scope = ScopeType.EVENT)
    public Action getCurrentSubView() {
        return webActions.getCurrentSubTabAction(getCurrentViewId());
    }

    public void setCurrentSubView(Action currentSubView) {
        webActions.setCurrentTabAction(TabActionsSelection.getSubTabCategory(getCurrentViewId()), currentSubView);
    }

    @Factory(value = "currentAdminSubViewId", scope = ScopeType.EVENT)
    public String getCurrentSubViewId() {
        return getCurrentSubView().getId();
    }

    public void setCurrentSubViewId(String currentSubViewId) {
        webActions.setCurrentTabId(TabActionsSelection.getSubTabCategory(getCurrentViewId()), currentSubViewId);
    }

    public List<Action> getAvailableActions() {
        return webActions.getActionsList(ADMIN_ACTION_CATEGORY);
    }

    public List<Action> getAvailableSubActions() {
        return webActions.getActionsList(TabActionsSelection.getSubTabCategory(getCurrentViewId()));
    }

    public boolean hasExternalPackageDownloadRequest() {
        return externalPackageDownloadRequest != null;
    }

    public void addExternalPackageDownloadRequest(String pkgId) {
        this.externalPackageDownloadRequest = pkgId;
    }

    public String getExternalPackageDownloadRequest() {
        String id = externalPackageDownloadRequest;
        externalPackageDownloadRequest = null;
        return id;
    }

    public boolean isUpdateCenterDisabled() {
        return Framework.isBooleanPropertyTrue("nuxeo.updatecenter.disabled");
    }

    public boolean isProductionServer() {
        return Framework.isBooleanPropertyTrue("org.nuxeo.prod");
    }

}
