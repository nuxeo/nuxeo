/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.platform.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
import org.nuxeo.ecm.webapp.action.WebActionsBean;
import org.nuxeo.ecm.webapp.security.UserManagerActions;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@Name("webActions")
@Scope(CONVERSATION)
@Install(precedence = Install.APPLICATION)
public class DamWebActions extends WebActionsBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamWebActions.class);

    @In(create = true)
    protected transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient UserManagerActions userManagerActions;

    protected ActionManager actionService;

    protected boolean showList = false;

    protected boolean showThumbnail = true;

    protected boolean showAdministration = false;

    protected List<Action> adminActionsList;

    @Out(required = false)
    protected Action currentAdminTabAction;

    @Override
    @Factory(value = "tabsActionsList", scope = EVENT)
    public List<Action> getTabsList() {
        if (tabsActionsList == null) {
            actionService = getActionService();
            tabsActionsList = actionService.getActions(
                    "VIEW_ASSET_ACTION_LIST", createActionContext());
            currentTabAction = getDefaultTab();
        }
        return tabsActionsList;
    }

    @Factory(value = "adminActionsList", scope = EVENT)
    public List<Action> getAdminTabsList() {
        if (adminActionsList == null) {
            adminActionsList = getActionService().getActions(
                    "ADMIN_ACTION_LIST", createActionContext());
            currentAdminTabAction = getDefaultAdminTab();
        }
        return adminActionsList;
    }

    protected Action getDefaultAdminTab() {
        if (getAdminTabsList() == null) {
            return null;
        }
        try {
            return adminActionsList.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Action getCurrentAdminTabAction() {
        if (currentAdminTabAction == null) {
            currentAdminTabAction = getDefaultAdminTab();
        }
        return currentAdminTabAction;
    }

    public void setCurrentAdminTabAction(Action currentAdminTabAction) {
        this.currentAdminTabAction = currentAdminTabAction;
    }

    @Factory(value = "actionManager", scope = EVENT)
    public ActionManager getActionService() {
        if (actionService != null) {
            return actionService;
        }
        try {
            actionService = Framework.getService(ActionManager.class);
        } catch (Exception e) {
            log.error("Failed to lookup ActionService", e);
        }

        return actionService;
    }

    @Override
    protected ActionContext createActionContext() {
        ActionContext ctx = new ActionContext();
        ctx.setDocumentManager(documentManager);
        ctx.put("SeamContext", new SeamContextHelper());
        ctx.setCurrentPrincipal(currentNuxeoPrincipal);
        return ctx;
    }

    public String navigateToAdministration() throws ClientException {
        showAdministration = true;
        return "administration";
    }

    public String navigateToAssetManagement() throws ClientException {
        showAdministration = false;
        return navigationContext.goHome();
    }

    @Factory(value = "isInsideAdministration", scope = EVENT)
    public boolean showAdministration() {
        return showAdministration;
    }

    public void showListLink() {
        if (showList) {
            return;
        }
        showList = !showList;
        showThumbnail = !showThumbnail;
    }

    public void showThumbnailLink() {
        if (showThumbnail) {
            return;
        }
        showThumbnail = !showThumbnail;
        showList = !showList;
    }

    public boolean getShowList() {
        return showList;
    }

    public void setShowList(boolean showList) {
        this.showList = showList;
    }

    public boolean getShowThumbnail() {
        return showThumbnail;
    }

    public void setShowThumbnail(boolean showThumbnail) {
        this.showThumbnail = showThumbnail;
    }

}
