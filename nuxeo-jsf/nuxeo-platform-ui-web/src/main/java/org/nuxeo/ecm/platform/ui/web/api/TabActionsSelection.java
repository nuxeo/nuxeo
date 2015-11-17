/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;

/**
 * Handles selected action tabs and raised events on current tab change.
 *
 * @see WebActions#CURRENT_TAB_CHANGED_EVENT
 * @since 5.4.2
 */
public class TabActionsSelection implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TabActionsSelection.class);

    /**
     * Map of current tab actions, with category as key and corresponding action as value.
     * <p>
     * Use a linked has map to preserve order when using several selections as sub tabs management needs order to be
     * preserved.
     */
    protected Map<String, Action> currentTabActions = new LinkedHashMap<String, Action>();

    /**
     * Returns the current action for given category.
     */
    public Action getCurrentTabAction(String category) {
        if (currentTabActions.containsKey(category)) {
            return currentTabActions.get(category);
        }
        return null;
    }

    /**
     * Sets the current action for given category, with additional sub tabs.
     * <p>
     * If given action is null, it resets the current action for this category.
     */
    public void setCurrentTabAction(String category, Action tabAction) {
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
                        Action oldAction = currentTabActions.get(category);
                        currentTabActions.put(category, tabAction);
                        raiseEventOnCurrentTabSelected(category, tabAction.getId());
                        if (oldAction == null || !oldAction.getId().equals(tabAction.getId())) {
                            // only raise the event if action actually changed
                            raiseEventOnCurrentTabChange(category, tabAction.getId());
                        }
                        if (oldAction != null) {
                            // additional cleanup of possible sub tabs
                            resetCurrentTabs(getSubTabCategory(oldAction.getId()));
                        }
                        break;
                    }
                }
                if (!categoryFound) {
                    log.error("Cannot set current action '" + tabAction.getId() + "' for category '" + category
                            + "' as this action does not hold the given category.");
                }
            }
        } else {
            resetCurrentTabs(category);
        }
    }

    public String getCurrentTabId(String category) {
        Action action = getCurrentTabAction(category);
        if (action != null) {
            return action.getId();
        }
        return null;
    }

    public void setCurrentTabId(ActionManager actionManager, ActionContext actionContext, String category,
            String tabId, String... subTabIds) {
        boolean set = false;
        if (tabId != null && !WebActions.NULL_TAB_ID.equals(tabId)) {
            if (actionManager.isEnabled(tabId, actionContext)) {
                Action action = actionManager.getAction(tabId);
                setCurrentTabAction(category, action);
                if (subTabIds != null && subTabIds.length > 0) {
                    String newTabId = subTabIds[0];
                    String[] newSubTabsIds = new String[subTabIds.length - 1];
                    System.arraycopy(subTabIds, 1, newSubTabsIds, 0, subTabIds.length - 1);
                    setCurrentTabId(actionManager, actionContext, getSubTabCategory(tabId), newTabId, newSubTabsIds);
                }
                set = true;
            } else {
                if (actionManager.getAction(tabId) != null) {
                    log.warn("Cannot set current tab with id '" + tabId + "': action is not enabled.");
                } else {
                    log.error("Cannot set current tab with id '" + tabId + "': action does not exist.");
                }
            }
        }
        if (!set && (tabId == null || WebActions.NULL_TAB_ID.equals(tabId))) {
            resetCurrentTabs(category);
        }
    }

    /**
     * Returns current tab ids as a string, encoded as is:
     * CATEGORY_1:ACTION_ID_1,CATEGORY_2:ACTION_ID_2:SUBTAB_ACTION_ID_2,...
     *
     * @since 5.4.2
     */
    public String getCurrentTabIds() {
        StringBuffer builder = new StringBuffer();
        boolean first = true;
        // resolve sub tabs
        Map<String, List<Action>> actionsToEncode = new LinkedHashMap<String, List<Action>>();
        Map<String, String> subTabToCategories = new HashMap<String, String>();
        for (Map.Entry<String, Action> currentTabAction : currentTabActions.entrySet()) {
            String category = currentTabAction.getKey();
            Action action = currentTabAction.getValue();
            subTabToCategories.put(getSubTabCategory(action.getId()), category);
            if (subTabToCategories.containsKey(category)) {
                // this is a sub action, parent already added
                String cat = subTabToCategories.get(category);
                List<Action> subActions = actionsToEncode.get(cat);
                if (subActions == null) {
                    subActions = new ArrayList<Action>();
                    actionsToEncode.put(cat, subActions);
                }
                subActions.add(action);
            } else {
                List<Action> actionsList = new ArrayList<Action>();
                actionsList.add(action);
                actionsToEncode.put(category, actionsList);
            }
        }
        for (Map.Entry<String, List<Action>> item : actionsToEncode.entrySet()) {
            String encodedActions = encodeActions(item.getKey(), item.getValue());
            if (encodedActions != null) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(encodedActions);
            }
        }
        return builder.toString();
    }

    /**
     * Sets current tab ids as a String, splitting on commas ',' and parsing each action information as is:
     * CATEGORY:[ACTION_ID[:OPTIONAL_SUB_ACTION_ID [:OPTIONAL_SUB_ACTION_ID]...]]
     * <p>
     * If category is omitted or empty, the category {@link #DEFAULT_TABS_CATEGORY} will be used (if there is no subtab
     * information).
     * <p>
     * If no action id is given, the corresponding category is reset (for instance using 'CATEGORY:').
     * <p>
     * If the action information is '*:', all categories will be reset.
     * <p>
     * The resulting string looks like: CATEGORY_1:ACTION_ID_1,CATEGORY_2:ACTION_ID_2_SUB_ACTION_ID_2,...
     *
     * @since 5.4.2
     */
    public void setCurrentTabIds(ActionManager actionManager, ActionContext actionContext, String tabIds) {
        if (tabIds == null) {
            return;
        }
        String[] encodedActions = tabIds.split(",");
        if (encodedActions != null && encodedActions.length != 0) {
            for (String encodedAction : encodedActions) {
                encodedAction = encodedAction.trim();
                if ((":").equals(encodedAction)) {
                    // reset default actions
                    resetCurrentTabs(WebActions.DEFAULT_TABS_CATEGORY);
                } else {
                    String[] actionInfo = encodedAction.split(":");
                    // XXX: "*:" vs ":TRUC"
                    if (actionInfo != null && actionInfo.length == 1) {
                        if (encodedAction.startsWith(":")) {
                            // it's a default action
                            setCurrentTabId(actionManager, actionContext, WebActions.DEFAULT_TABS_CATEGORY,
                                    actionInfo[0]);
                        } else {
                            String category = actionInfo[0];
                            // it's a category, and it needs to be reset
                            if ("*".equals(category)) {
                                resetCurrentTabs();
                            } else {
                                resetCurrentTabs(category);
                            }
                        }
                    } else if (actionInfo != null && actionInfo.length > 1) {
                        String category = actionInfo[0];
                        String actionId = actionInfo[1];
                        String[] subTabsIds = new String[actionInfo.length - 2];
                        System.arraycopy(actionInfo, 2, subTabsIds, 0, actionInfo.length - 2);
                        if (category == null || category.isEmpty()) {
                            category = WebActions.DEFAULT_TABS_CATEGORY;
                        }
                        setCurrentTabId(actionManager, actionContext, category, actionId, subTabsIds);
                    } else {
                        log.error("Cannot set current tab from given encoded action: '" + encodedAction + "'");
                    }
                }
            }
        }
    }

    /**
     * Resets all current tabs information.
     *
     * @since 5.4.2
     */
    public void resetCurrentTabs() {
        Set<String> categories = currentTabActions.keySet();
        currentTabActions.clear();
        for (String category : categories) {
            raiseEventOnCurrentTabSelected(category, null);
            raiseEventOnCurrentTabChange(category, null);
        }
    }

    /**
     * Resets current tabs for given category, taking subtabs into account by resetting actions in categories computed
     * from reset actions id with suffix {@link #SUBTAB_CATEGORY_SUFFIX}.
     */
    public void resetCurrentTabs(String category) {
        if (currentTabActions.containsKey(category)) {
            Action action = currentTabActions.get(category);
            currentTabActions.remove(category);
            raiseEventOnCurrentTabSelected(category, null);
            raiseEventOnCurrentTabChange(category, null);
            if (action != null) {
                resetCurrentTabs(getSubTabCategory(action.getId()));
            }
        }
    }

    protected String encodeActions(String category, List<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(WebActions.DEFAULT_TABS_CATEGORY.equals(category) ? "" : category);
        for (int i = 0; i < actions.size(); i++) {
            builder.append(":" + actions.get(i).getId());
        }
        return builder.toString();
    }

    public static String getSubTabCategory(String parentActionId) {
        if (parentActionId == null) {
            return null;
        }
        return parentActionId + WebActions.SUBTAB_CATEGORY_SUFFIX;
    }

    /**
     * Raises a seam event when current tab changes for a given category.
     * <p>
     * Actually raises 2 events: one with name WebActions#CURRENT_TAB_CHANGED_EVENT and another with name
     * WebActions#CURRENT_TAB_CHANGED_EVENT + '_' + category to optimize observers declarations.
     * <p>
     * The event is always sent with 2 parameters: the category and tab id (the tab id can be null when resetting
     * current tab for given category).
     *
     * @see WebActions#CURRENT_TAB_CHANGED_EVENT
     * @since 5.4.2
     */
    protected void raiseEventOnCurrentTabChange(String category, String tabId) {
        if (Events.exists()) {
            Events.instance().raiseEvent(WebActions.CURRENT_TAB_CHANGED_EVENT, category, tabId);
            Events.instance().raiseEvent(WebActions.CURRENT_TAB_CHANGED_EVENT + "_" + category, category, tabId);
        }
    }

    /**
     * Raises a seam event when current tab is selected for a given category. Fired also when current tab did not
     * change.
     * <p>
     * Actually raises 2 events: one with name WebActions#CURRENT_TAB_SELECTED_EVENT and another with name
     * WebActions#CURRENT_TAB_SELECTED_EVENT + '_' + category to optimize observers declarations.
     * <p>
     * The event is always sent with 2 parameters: the category and tab id (the tab id can be null when resetting
     * current tab for given category).
     *
     * @see WebActions#CURRENT_TAB_SELECTED_EVENT
     * @since 5.6
     */
    protected void raiseEventOnCurrentTabSelected(String category, String tabId) {
        if (Events.exists()) {
            Events.instance().raiseEvent(WebActions.CURRENT_TAB_SELECTED_EVENT, category, tabId);
            Events.instance().raiseEvent(WebActions.CURRENT_TAB_SELECTED_EVENT + "_" + category, category, tabId);
        }
    }

}
