/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Eugen Ionica
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * Web actions bean that manages actions.
 * <p>
 * Implements specific behavior to handle special mechanism of the documents
 * tabbed content.
 *
 * @author Eugen Ionica
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@Name("webActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class WebActionsBean implements WebActionsLocal, Serializable {

    private static final long serialVersionUID = 1959221536502251848L;

    private static final Log log = LogFactory.getLog(WebActionsBean.class);

    @In(create = true, required = false)
    protected transient ActionManager actionManager;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected List<Action> tabsActionsList;

    protected List<Action> subTabsActionsList;

    @Out(required = false)
    protected Action currentTabAction;

    @Out(required = false)
    protected Action currentSubTabAction;

    public void initialize() {
        log.debug("Initializing...");
    }

    @Destroy
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    public List<Action> getActionsList(String category, ActionContext context) {
        List<Action> list = new ArrayList<Action>();
        List<Action> actions = actionManager.getActions(category, context);
        if (actions != null) {
            list.addAll(actions);
        }
        return list;
    }

    public List<Action> getActionsList(String category) {
        return getActionsList(category, createActionContext());
    }

    public List<Action> getUnfiltredActionsList(String category,
            ActionContext context) {
        List<Action> list = new ArrayList<Action>();
        List<Action> actions = actionManager.getActions(category, context,
                false);
        if (actions != null) {
            list.addAll(actions);
        }
        return list;
    }

    public List<Action> getUnfiltredActionsList(String category) {
        return getUnfiltredActionsList(category, createActionContext());
    }

    public List<Action> getAllActions(String category) {
        return actionManager.getAllActions(category);
    }

    protected ActionContext createActionContext() {
        return actionContextProvider.createActionContext();
    }

    @Deprecated
    public List<Action> getSubViewActionsList() {
        return getActionsList("SUBVIEW_UPPER_LIST");
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
            EventNames.LOCATION_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetTabList() {
        tabsActionsList = null;
        currentTabAction = null;
        subTabsActionsList = null;
        currentSubTabAction = null;
    }

    public void resetCurrentTab() {
        currentTabAction = null;
        currentSubTabAction = null;
    }

    @Factory(value = "tabsActionsList", scope = EVENT)
    public List<Action> getTabsList() {
        if (tabsActionsList == null) {
            tabsActionsList = getActionsList("VIEW_ACTION_LIST");
            currentTabAction = getDefaultTab();
        }
        return tabsActionsList;
    }

    @Factory(value = "subTabsActionsList", scope = EVENT)
    public List<Action> getSubTabsList() {
        if (subTabsActionsList == null) {
            String currentTabId = getCurrentTabId();
            if (currentTabId != null) {
                String category = currentTabId + "_sub_tab";
                subTabsActionsList = getActionsList(category);
                currentSubTabAction = getDefaultSubTab();
            }
        }
        return subTabsActionsList;
    }

    public void setTabsList(List<Action> tabsList) {
        tabsActionsList = tabsList;
    }

    public void setSubTabsList(List<Action> tabsList) {
        subTabsActionsList = tabsList;
    }

    protected Action getDefaultTab() {
        if (getTabsList() == null) {
            return null;
        }
        try {
            return tabsActionsList.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Action getCurrentTabAction() {
        if (currentTabAction == null) {
            currentTabAction = getDefaultTab();
        }
        return currentTabAction;
    }

    public void setCurrentTabAction(Action currentTabAction) {
        this.currentTabAction = currentTabAction;
        subTabsActionsList = null;
        currentSubTabAction = null;
    }

    protected Action getDefaultSubTab() {
        if (getSubTabsList() == null) {
            return null;
        }
        try {
            return subTabsActionsList.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Action getCurrentSubTabAction() {
        if (currentSubTabAction == null) {
            currentSubTabAction = getDefaultSubTab();
        }
        return currentSubTabAction;
    }

    public void setCurrentSubTabAction(Action tabAction) {
        currentSubTabAction = tabAction;
    }

    @Deprecated
    public void setCurrentTabAction(String currentTabActionId) {
        setCurrentTabId(currentTabActionId);
    }

    public String getCurrentTabId() {
        Action currentTab = getCurrentTabAction();
        if (currentTab != null) {
            return currentTab.getId();
        }
        return null;
    }

    public void setCurrentTabId(String tabId) {
        boolean set = false;
        if (tabId != null && !NULL_TAB_ID.equals(tabId)) {
            List<Action> tabsList = getTabsList();
            if (tabsList != null) {
                for (Action a : tabsList) {
                    if (a.getId().equals(tabId)) {
                        setCurrentTabAction(a);
                        set = true;
                        break;
                    }
                }
            }
        }
        if (!set && (tabId == null || NULL_TAB_ID.equals(tabId))) {
            setCurrentTabAction((Action) null);
        }
    }

    public String getCurrentSubTabId() {
        Action currentSubTab = getCurrentSubTabAction();
        if (currentSubTab != null) {
            return currentSubTab.getId();
        }
        return null;
    }

    public void setCurrentSubTabId(String tabId) {
        boolean set = false;
        List<Action> subTabsList = getSubTabsList();
        if (subTabsList != null) {
            for (Action a : subTabsList) {
                if (a.getId().equals(tabId)) {
                    currentSubTabAction = a;
                    set = true;
                    break;
                }
            }
        }
        if (!set && (tabId == null || NULL_TAB_ID.equals(tabId))) {
            // reset it
            currentSubTabAction = null;
        }
    }

    public String setCurrentTabAndNavigate(String currentTabActionId) {
        return setCurrentTabAndNavigate(navigationContext.getCurrentDocument(),
                currentTabActionId);
    }

    public String setCurrentTabAndNavigate(DocumentModel document,
            String currentTabActionId) {
        // navigate first because it will reset the tabs list
        String viewId = null;
        try {
            viewId = navigationContext.navigateToDocument(document);
        } catch (ClientException e) {

        }
        // force creation of new actions if needed
        getTabsList();
        // set current tab
        setCurrentTabId(currentTabActionId);
        return viewId;
    }

    public void selectTabAction() {
        // if (tabAction != null) {
        // setCurrentTabAction(tabAction);
        // }
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public String getCurrentLifeCycleState() throws ClientException {
        // only user of documentManager in this bean, look it up by hand
        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        return documentManager.getCurrentLifeCycleState(navigationContext.getCurrentDocument().getRef());
    }

}
