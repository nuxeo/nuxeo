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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.common.utils.UserAgentMatcher;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.TabActionsSelection;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Component that handles actions retrieval as well as current tab(s) selection.
 *
 * @author Eugen Ionica
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@Name("webActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class WebActionsBean implements WebActions, Serializable {

    private static final long serialVersionUID = 1959221536502251848L;

    private static final Log log = LogFactory.getLog(WebActionsBean.class);

    @In(create = true, required = false)
    protected transient ActionManager actionManager;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected List<Action> tabsActionsList;

    protected String subTabsCategory;

    protected List<Action> subTabsActionsList;

    protected TabActionsSelection currentTabActions = new TabActionsSelection();

    // actions management

    @Override
    public List<Action> getDocumentActions(DocumentModel document, String category, boolean removeFiltered,
            boolean postFilter) {
        ActionContext context = postFilter ? null : createActionContext(document);
        return getActions(context, category, removeFiltered, postFilter);
    }

    @Override
    public Action getDocumentAction(DocumentModel document, String actionId, boolean removeFiltered, boolean postFilter) {
        ActionContext context = postFilter ? null : createActionContext(document);
        return getAction(context, actionId, removeFiltered, postFilter);
    }

    @Override
    public List<Action> getActions(ActionContext context, String category, boolean removeFiltered, boolean postFilter) {
        List<Action> list = new ArrayList<Action>();
        List<String> categories = new ArrayList<String>();
        if (category != null) {
            String[] split = category.split(",|\\s");
            if (split != null) {
                for (String item : split) {
                    if (!StringUtils.isBlank(item)) {
                        categories.add(item.trim());
                    }
                }
            }
        }
        for (String cat : categories) {
            List<Action> actions;
            if (postFilter) {
                actions = actionManager.getAllActions(cat);
            } else {
                actions = actionManager.getActions(cat, context, removeFiltered);
            }
            if (actions != null) {
                list.addAll(actions);
            }
        }
        return list;
    }

    @Override
    public Action getAction(ActionContext context, String actionId, boolean removeFiltered, boolean postFilter) {
        if (postFilter) {
            return actionManager.getAction(actionId);
        }
        return actionManager.getAction(actionId, context, removeFiltered);

    }

    @Override
    public boolean isAvailableForDocument(DocumentModel document, Action action) {
        return isAvailable(createActionContext(document), action);
    }

    @Override
    public boolean isAvailable(ActionContext context, Action action) {
        if (action == null) {
            return false;
        }
        if (action.isFiltered()) {
            return action.getAvailable();
        }
        return actionManager.checkFilters(action, context);
    }

    @Override
    public List<Action> getActionsList(String category, ActionContext context, boolean removeFiltered) {
        return getActions(context, category, removeFiltered, false);
    }

    @Override
    public List<Action> getActionsList(String category, Boolean removeFiltered) {
        return getActions(createActionContext(), category, removeFiltered, false);
    }

    @Override
    public List<Action> getActionsList(String category, ActionContext context) {
        return getActions(context, category, true, false);
    }

    @Override
    public List<Action> getActionsListForDocument(String category, DocumentModel document, boolean removeFiltered) {
        return getActions(createActionContext(document), category, removeFiltered, false);
    }

    @Override
    public List<Action> getActionsList(String category) {
        return getActions(createActionContext(), category, true, false);
    }

    @Override
    public List<Action> getUnfiltredActionsList(String category, ActionContext context) {
        return getActions(context, category, false, false);
    }

    @Override
    public List<Action> getUnfiltredActionsList(String category) {
        return getActions(createActionContext(), category, false, false);
    }

    @Override
    public List<Action> getAllActions(String category) {
        return actionManager.getAllActions(category);
    }

    protected ActionContext createActionContext() {
        return actionContextProvider.createActionContext();
    }

    protected ActionContext createActionContext(DocumentModel document) {
        return actionContextProvider.createActionContext(document);
    }

    @Override
    public Action getAction(String actionId, boolean removeFiltered) {
        return getAction(createActionContext(), actionId, removeFiltered, false);
    }

    @Override
    public Action getActionForDocument(String actionId, DocumentModel document, boolean removeFiltered) {
        return getAction(createActionContext(document), actionId, removeFiltered, false);
    }

    @Override
    public Action getAction(String actionId, ActionContext context, boolean removeFiltered) {
        return getAction(context, actionId, removeFiltered, false);
    }

    @Override
    public boolean checkFilter(String filterId) {
        return actionManager.checkFilter(filterId, createActionContext());
    }

    // tabs management

    protected Action getDefaultTab(String category) {
        if (DEFAULT_TABS_CATEGORY.equals(category)) {
            if (getTabsList() == null) {
                return null;
            }
            try {
                return tabsActionsList.get(0);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        } else {
            // check if it's a subtab
            if (subTabsCategory != null && subTabsCategory.equals(category)) {
                if (getSubTabsList() == null) {
                    return null;
                }
                try {
                    return subTabsActionsList.get(0);
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
            // retrieve actions in given category and take the first one found
            List<Action> actions = getActionsList(category, createActionContext());
            if (actions != null && actions.size() > 0) {
                // make sure selection event is sent
                Action action = actions.get(0);
                setCurrentTabAction(category, action);
                return action;
            }
            return null;
        }

    }

    @Override
    public Action getCurrentTabAction(String category) {
        Action action = currentTabActions.getCurrentTabAction(category);
        if (action == null) {
            // return default action
            action = getDefaultTab(category);
        }
        return action;
    }

    @Override
    public void setCurrentTabAction(String category, Action tabAction) {
        currentTabActions.setCurrentTabAction(category, tabAction);
        // additional cleanup of this cache
        if (WebActions.DEFAULT_TABS_CATEGORY.equals(category)) {
            resetSubTabs();
        }
    }

    @Override
    public Action getCurrentSubTabAction(String parentActionId) {
        return getCurrentTabAction(TabActionsSelection.getSubTabCategory(parentActionId));
    }

    @Override
    public String getCurrentTabId(String category) {
        Action action = getCurrentTabAction(category);
        if (action != null) {
            return action.getId();
        }
        return null;
    }

    @Override
    public boolean hasCurrentTabId(String category) {
        if (currentTabActions.getCurrentTabAction(category) == null) {
            return false;
        }
        return true;
    }

    @Override
    public void setCurrentTabId(String category, String tabId, String... subTabIds) {
        currentTabActions.setCurrentTabId(actionManager, createActionContext(), category, tabId, subTabIds);
        // additional cleanup of this cache
        if (WebActions.DEFAULT_TABS_CATEGORY.equals(category)) {
            resetSubTabs();
        }
    }

    @Override
    public String getCurrentTabIds() {
        return currentTabActions.getCurrentTabIds();
    }

    @Override
    public void setCurrentTabIds(String tabIds) {
        currentTabActions.setCurrentTabIds(actionManager, createActionContext(), tabIds);
        // reset subtabs just in case
        resetSubTabs();
    }

    @Override
    public void resetCurrentTabs() {
        currentTabActions.resetCurrentTabs();
    }

    @Override
    public void resetCurrentTabs(String category) {
        currentTabActions.resetCurrentTabs(category);
    }

    // tabs management specific to the DEFAULT_TABS_CATEGORY

    @Override
    public void resetCurrentTab() {
        resetCurrentTabs(DEFAULT_TABS_CATEGORY);
    }

    protected void resetSubTabs() {
        subTabsCategory = null;
        subTabsActionsList = null;
        // make sure event context is cleared so that factory is called again
        Contexts.getEventContext().remove("subTabsActionsList");
        Contexts.getEventContext().remove("currentSubTabAction");
    }

    @Override
    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
            EventNames.LOCATION_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetTabList() {
        tabsActionsList = null;
        resetSubTabs();
        resetCurrentTab();
        // make sure event context is cleared so that factory is called again
        Contexts.getEventContext().remove("tabsActionsList");
        Contexts.getEventContext().remove("currentTabAction");
    }

    @Override
    @Factory(value = "tabsActionsList", scope = EVENT)
    public List<Action> getTabsList() {
        if (tabsActionsList == null) {
            tabsActionsList = getActionsList(DEFAULT_TABS_CATEGORY);
        }
        return tabsActionsList;
    }

    @Override
    @Factory(value = "subTabsActionsList", scope = EVENT)
    public List<Action> getSubTabsList() {
        if (subTabsActionsList == null) {
            String currentTabId = getCurrentTabId();
            if (currentTabId != null) {
                subTabsCategory = TabActionsSelection.getSubTabCategory(currentTabId);
                subTabsActionsList = getActionsList(subTabsCategory);
            }
        }
        return subTabsActionsList;
    }

    @Override
    @Factory(value = "currentTabAction", scope = EVENT)
    public Action getCurrentTabAction() {
        return getCurrentTabAction(DEFAULT_TABS_CATEGORY);
    }

    @Override
    public void setCurrentTabAction(Action currentTabAction) {
        setCurrentTabAction(DEFAULT_TABS_CATEGORY, currentTabAction);
    }

    @Override
    @Factory(value = "currentSubTabAction", scope = EVENT)
    public Action getCurrentSubTabAction() {
        Action action = getCurrentTabAction();
        if (action != null) {
            return getCurrentTabAction(TabActionsSelection.getSubTabCategory(action.getId()));
        }
        return null;
    }

    @Override
    public void setCurrentSubTabAction(Action tabAction) {
        if (tabAction != null) {
            String[] categories = tabAction.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot set subtab with id '" + tabAction.getId()
                        + "' as this action does not hold any category");
                return;
            }
            if (categories.length != 1) {
                log.error("Setting subtab with id '" + tabAction.getId() + "' with category '" + categories[0]
                        + "': use webActions#setCurrentTabAction(action, category) to specify another category");
            }
            setCurrentTabAction(categories[0], tabAction);
        }
    }

    @Override
    public String getCurrentTabId() {
        Action currentTab = getCurrentTabAction();
        if (currentTab != null) {
            return currentTab.getId();
        }
        return null;
    }

    @Override
    public void setCurrentTabId(String tabId) {
        if (tabId != null) {
            // do not reset tab when not set as this method
            // is used for compatibility in default url pattern
            setCurrentTabId(DEFAULT_TABS_CATEGORY, tabId);
        }
    }

    @Override
    public String getCurrentSubTabId() {
        Action currentSubTab = getCurrentSubTabAction();
        if (currentSubTab != null) {
            return currentSubTab.getId();
        }
        return null;
    }

    @Override
    public void setCurrentSubTabId(String tabId) {
        if (tabId != null) {
            // do not reset tab when not set as this method
            // is used for compatibility in default url pattern
            Action action = getCurrentTabAction();
            if (action != null) {
                setCurrentTabId(TabActionsSelection.getSubTabCategory(action.getId()), tabId);
            }
        }
    }

    // navigation API

    @Override
    public String setCurrentTabAndNavigate(String currentTabActionId) {
        return setCurrentTabAndNavigate(navigationContext.getCurrentDocument(), currentTabActionId);
    }

    @Override
    public String setCurrentTabAndNavigate(DocumentModel document, String currentTabActionId) {
        // navigate first because it will reset the tabs list
        String viewId = null;
        try {
            viewId = navigationContext.navigateToDocument(document);
        } catch (NuxeoException e) {
            log.error("Failed to navigate to " + document, e);
        }
        // force creation of new actions if needed
        getTabsList();
        // set current tab
        setCurrentTabId(currentTabActionId);
        return viewId;
    }

    // deprecated API

    @Override
    @Deprecated
    public List<Action> getSubViewActionsList() {
        return getActionsList("SUBVIEW_UPPER_LIST");
    }

    @Override
    @Deprecated
    public void selectTabAction() {
        // if (tabAction != null) {
        // setCurrentTabAction(tabAction);
        // }
    }

    @Override
    @Deprecated
    public String getCurrentLifeCycleState() {
        // only user of documentManager in this bean, look it up by hand
        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        return documentManager.getCurrentLifeCycleState(navigationContext.getCurrentDocument().getRef());
    }

    @Override
    @Deprecated
    public void setTabsList(List<Action> tabsList) {
        tabsActionsList = tabsList;
    }

    @Override
    @Deprecated
    public void setSubTabsList(List<Action> tabsList) {
        subTabsActionsList = tabsList;
        subTabsCategory = null;
        if (tabsList != null) {
            // BBB code
            for (Action action : tabsList) {
                if (action != null) {
                    String[] categories = action.getCategories();
                    if (categories != null && categories.length > 0) {
                        subTabsCategory = categories[0];
                        break;
                    }
                }
            }
        }
    }

    @Override
    @Deprecated
    public void setCurrentTabAction(String currentTabActionId) {
        setCurrentTabId(currentTabActionId);
    }

    @Override
    @Factory(value = "useAjaxTabs", scope = ScopeType.SESSION)
    public boolean useAjaxTabs() {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        if (configurationService.isBooleanPropertyTrue(AJAX_TAB_PROPERTY)) {
            return canUseAjaxTabs();
        }
        return false;
    }

    @Override
    @Factory(value = "canUseAjaxTabs", scope = ScopeType.SESSION)
    public boolean canUseAjaxTabs() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext econtext = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) econtext.getRequest();
        String ua = request.getHeader("User-Agent");
        return UserAgentMatcher.isHistoryPushStateSupported(ua);
    }

    /**
     * Returns true if configuration property to remove optimizations around actions (for compatibility) has been
     * enabled.
     *
     * @since 8.2
     */
    @Factory(value = "removeActionOptims", scope = ScopeType.SESSION)
    public boolean removeActionOptims() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return cs.isBooleanPropertyTrue("nuxeo.jsf.actions.removeActionOptims");
    }

    @Observer(value = { EventNames.FLUSH_EVENT }, create = false)
    @BypassInterceptors
    public void onHotReloadFlush() {
        // reset above caches
        Context seamContext = Contexts.getSessionContext();
        seamContext.remove("useAjaxTabs");
        seamContext.remove("canUseAjaxTabs");
    }

}
