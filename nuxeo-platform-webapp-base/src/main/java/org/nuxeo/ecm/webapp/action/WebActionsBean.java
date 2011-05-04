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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Component that handles actions retrieval as well as current tab(s)
 * selection.
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

    protected String subTabsCategory;

    protected List<Action> subTabsActionsList;

    /**
     * Map of current tab actions, with category as key and corresponding
     * action as value.
     */
    protected Map<String, Action> currentTabActions = new HashMap<String, Action>();

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

    // actions management

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
            List<Action> actions = getActionsList(category,
                    createActionContext());
            if (actions != null && actions.size() > 0) {
                return actions.get(0);
            }
            return null;
        }

    }

    protected String getSubTabCategory(String parentActionId) {
        if (parentActionId == null) {
            return null;
        }
        return parentActionId + SUBTAB_CATEGORY_SUFFIX;
    }

    @Override
    public Action getCurrentTabAction(String category) {
        if (currentTabActions.containsKey(category)) {
            return currentTabActions.get(category);
        }
        // return default action
        return getDefaultTab(category);
    }

    @Override
    public void setCurrentTabAction(String category, Action tabAction,
            String... subTabIds) {
        if (category == null) {
            return;
        }
        if (tabAction != null) {
            String[] actionCategories = tabAction.getCategories();
            if (actionCategories != null) {
                boolean categoryFound = false;
                for (String actionCategory : actionCategories) {
                    if (category.equals(actionCategory)) {
                        categoryFound = true;
                        currentTabActions.put(category, tabAction);
                        // additional cleanup of possible subtabs
                        resetCurrentTabs(getSubTabCategory(tabAction.getIcon()));
                        // additional cleanup of this cache
                        if (category.equals(DEFAULT_TABS_CATEGORY)) {
                            subTabsCategory = null;
                            subTabsActionsList = null;
                        }
                        break;
                    }
                }
                if (!categoryFound) {
                    log.error(String.format(
                            "Cannot set current action '%s' for category"
                                    + " '%s' as this action does not "
                                    + "hold the given category.",
                            tabAction.getId(), category));
                } else if (subTabIds != null && subTabIds.length > 0) {
                    String newTabId = subTabIds[0];
                    String[] newSubTabsIds = new String[subTabIds.length - 1];
                    System.arraycopy(subTabIds, 1, newSubTabsIds, 0,
                            subTabIds.length - 1);
                    setCurrentTabId(getSubTabCategory(tabAction.getId()),
                            newTabId, newSubTabsIds);
                }
            }
        } else {
            resetCurrentTabs(category);
        }
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
    public void setCurrentTabId(String category, String tabId,
            String... subTabIds) {
        boolean set = false;
        if (tabId != null && !NULL_TAB_ID.equals(tabId)) {
            ActionContext context = createActionContext();
            if (actionManager.isEnabled(tabId, context)) {
                Action action = actionManager.getAction(tabId);
                setCurrentTabAction(category, action, subTabIds);
                set = true;
            } else {
                if (actionManager.getAction(tabId) != null) {
                    log.error(String.format(
                            "Cannot set current tab with id '%s': "
                                    + "action does not exist.", tabId));
                } else {
                    log.error(String.format(
                            "Cannot set current tab with id '%s': "
                                    + "action is not enabled.", tabId));
                }
            }
        }
        if (!set && (tabId == null || NULL_TAB_ID.equals(tabId))) {
            // reset it
            setCurrentTabAction(category, (Action) null);
        }
    }

    protected String encodeAction(Action action, String category) {
        if (action == null) {
            return null;
        }
        return String.format("%s:%s", category, action.getId());
    }

    @Override
    public String getCurrentTabIds() {
        StringBuffer builder = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, Action> currentTabAction : currentTabActions.entrySet()) {
            String encodedAction = encodeAction(currentTabAction.getValue(),
                    currentTabAction.getKey());
            if (encodedAction != null) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(encodedAction);
            }
        }
        return builder.toString();
    }

    @Override
    public void setCurrentTabIds(String tabIds) {
        if (tabIds == null) {
            return;
        }
        String[] encodedActions = tabIds.split(",");
        if (encodedActions != null && encodedActions.length != 0) {
            for (String encodedAction : encodedActions) {
                String[] actionInfo = encodedAction.trim().split(":");
                if (actionInfo != null && actionInfo.length == 1) {
                    String actionId = actionInfo[0];
                    setCurrentTabId(DEFAULT_TABS_CATEGORY, actionId);
                } else if (actionInfo != null && actionInfo.length > 1) {
                    String category = actionInfo[0];
                    String actionId = actionInfo[1];
                    String[] subTabsIds = new String[actionInfo.length - 2];
                    System.arraycopy(actionInfo, 2, subTabsIds, 0,
                            actionInfo.length - 2);
                    if (category == null || category.isEmpty()) {
                        category = DEFAULT_TABS_CATEGORY;
                    }
                    setCurrentTabId(category, actionId, subTabsIds);
                } else {
                    log.error("Cannot set current tab from given encoded action: "
                            + encodedAction);
                }
            }
        }
    }

    @Override
    public void resetCurrentTabs() {
        currentTabActions.clear();
    }

    @Override
    public void resetCurrentTabs(String category) {
        if (currentTabActions.containsKey(category)) {
            Action action = currentTabActions.get(category);
            currentTabActions.remove(category);
            if (action != null) {
                resetCurrentTabs(getSubTabCategory(action.getId()));
            }
        }
    }

    // tabs management specific to the DEFAULT_TABS_CATEGORY

    public void resetCurrentTab() {
        resetCurrentTabs(DEFAULT_TABS_CATEGORY);
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
            EventNames.LOCATION_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetTabList() {
        tabsActionsList = null;
        subTabsCategory = null;
        subTabsActionsList = null;
        resetCurrentTab();
    }

    @Factory(value = "tabsActionsList", scope = EVENT)
    public List<Action> getTabsList() {
        if (tabsActionsList == null) {
            tabsActionsList = getActionsList(DEFAULT_TABS_CATEGORY);
        }
        return tabsActionsList;
    }

    @Factory(value = "subTabsActionsList", scope = EVENT)
    public List<Action> getSubTabsList() {
        if (subTabsActionsList == null) {
            String currentTabId = getCurrentTabId();
            if (currentTabId != null) {
                subTabsCategory = getSubTabCategory(currentTabId);
                subTabsActionsList = getActionsList(subTabsCategory);
            }
        }
        return subTabsActionsList;
    }

    @Factory(value = "currentTabAction", scope = EVENT)
    @Deprecated
    public Action getCurrentTabAction() {
        return getCurrentTabAction(DEFAULT_TABS_CATEGORY);
    }

    @Deprecated
    public void setCurrentTabAction(Action currentTabAction) {
        setCurrentTabAction(DEFAULT_TABS_CATEGORY, currentTabAction);
    }

    @Factory(value = "currentSubTabAction", scope = EVENT)
    @Deprecated
    public Action getCurrentSubTabAction() {
        Action action = getCurrentTabAction();
        if (action != null) {
            return getCurrentTabAction(getSubTabCategory(action.getId()));
        }
        return null;
    }

    @Deprecated
    public void setCurrentSubTabAction(Action tabAction) {
        if (tabAction != null) {
            String[] categories = tabAction.getCategories();
            if (categories == null || categories.length == 0) {
                log.error(String.format("Cannot set subtab with id '%s' "
                        + "as this action does not hold any category",
                        tabAction.getId()));
                return;
            }
            if (categories.length != 1) {
                log.error(String.format(
                        "Setting subtab with id '%s' with category '%s': "
                                + "use webActions#setCurrentTabAction(action, category) "
                                + "to specify another category",
                        tabAction.getId(), categories[0]));
            }
            setCurrentTabAction(categories[0], tabAction);
        }
    }

    @Deprecated
    public String getCurrentTabId() {
        Action currentTab = getCurrentTabAction();
        if (currentTab != null) {
            return currentTab.getId();
        }
        return null;
    }

    @Deprecated
    public void setCurrentTabId(String tabId) {
        setCurrentTabId(DEFAULT_TABS_CATEGORY, tabId);
    }

    @Deprecated
    public String getCurrentSubTabId() {
        Action currentSubTab = getCurrentSubTabAction();
        if (currentSubTab != null) {
            return currentSubTab.getId();
        }
        return null;
    }

    @Deprecated
    public void setCurrentSubTabId(String tabId) {
        Action action = getCurrentTabAction();
        if (action != null) {
            setCurrentTabId(getSubTabCategory(action.getId()), tabId);
        }
    }

    // navigation API

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

    // deprecated API

    @Deprecated
    public List<Action> getSubViewActionsList() {
        return getActionsList("SUBVIEW_UPPER_LIST");
    }

    @Deprecated
    public void selectTabAction() {
        // if (tabAction != null) {
        // setCurrentTabAction(tabAction);
        // }
    }

    @Deprecated
    public String getCurrentLifeCycleState() throws ClientException {
        // only user of documentManager in this bean, look it up by hand
        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        return documentManager.getCurrentLifeCycleState(navigationContext.getCurrentDocument().getRef());
    }

    @Deprecated
    public void setTabsList(List<Action> tabsList) {
        tabsActionsList = tabsList;
    }

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

    @Deprecated
    public void setCurrentTabAction(String currentTabActionId) {
        setCurrentTabId(currentTabActionId);
    }

}
