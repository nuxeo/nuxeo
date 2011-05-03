/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: WebActions.java 25545 2007-09-28 15:03:26Z btatar $
 */

package org.nuxeo.ecm.platform.ui.web.api;

import java.util.List;

import javax.ejb.Remote;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;

/**
 * Component that handles actions retrieval as well as current tab(s)
 * selection.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Remote
public interface WebActions {

    public static final String NULL_TAB_ID = "";

    public static final String DEFAULT_TABS_CATEGORY = "VIEW_ACTION_LIST";

    public static final String SUBTAB_CATEGORY_SUFFIX = "_sub_tab";

    /**
     * Returns all filtered actions for a given category and given resolution
     * context.
     * <p>
     * Actions are filtered according to filters set on the actions
     * definitions.
     */
    List<Action> getActionsList(String category, ActionContext context);

    /**
     * Returns all filtered actions for a given category, creating a new
     * context for the filters resolution.
     *
     * @see #getActionsList(String, ActionContext)
     */
    List<Action> getActionsList(String category);

    /**
     * Returns all actions for a given category and given resolution context.
     * <p>
     * Actions are not filtered according to filters set on the actions
     * definitions: actions that should have been removed are just marked as
     * non-available.
     */
    List<Action> getUnfiltredActionsList(String category, ActionContext context);

    /**
     * Returns all actions for a given category, creating a new context for the
     * filters resolution.
     *
     * @see #getUnfiltredActionsList(String, ActionContext)
     */
    List<Action> getUnfiltredActionsList(String category);

    /**
     * Returns all actions for a given category, without filtering.
     */
    List<Action> getAllActions(String category);

    /**
     * Returns filtered actions for the category {@link #DEFAULT_TABS_CATEGORY}
     */
    List<Action> getTabsList();

    /**
     * Returns filtered actions for a category computed from the current tab
     * action id and the suffix {@link #SUBTAB_CATEGORY_SUFFIX}.
     */
    List<Action> getSubTabsList();

    /**
     * Returns the current action for category {@link #DEFAULT_TABS_CATEGORY}
     */
    Action getCurrentTabAction();

    /**
     * Sets the current action for category {@link #DEFAULT_TABS_CATEGORY}
     */
    void setCurrentTabAction(Action tabAction);

    /**
     * Returns the current sub tab for a category computed from the current tab
     * action id and the suffix {@link #SUBTAB_CATEGORY_SUFFIX}.
     */
    Action getCurrentSubTabAction();

    /**
     * Sets the current sub tab for a category computed from the current tab
     * action id and the suffix {@link #SUBTAB_CATEGORY_SUFFIX}.
     */
    void setCurrentSubTabAction(Action tabAction);

    /**
     * Returns the current action id for category
     * {@link #DEFAULT_TABS_CATEGORY}
     */
    String getCurrentTabId();

    /**
     * Sets the current action id for category {@link #DEFAULT_TABS_CATEGORY}
     */
    void setCurrentTabId(String tabId);

    /**
     * Returns the current sub tab id for a category computed from the current
     * tab action id and the suffix {@link #SUBTAB_CATEGORY_SUFFIX}.
     */
    String getCurrentSubTabId();

    /**
     * Sets the current sub tab id for a category computed from the current tab
     * action id and the suffix {@link #SUBTAB_CATEGORY_SUFFIX}.
     */
    void setCurrentSubTabId(String tabId);

    /**
     * Resets actions resolved for category {@link #DEFAULT_TABS_CATEGORY} so
     * that they're recomputed. Also calls {@link #resetCurrentTab()}
     */
    void resetTabList();

    /**
     * Resets current tab information (includes sub tab information).
     */
    void resetCurrentTab();

    /**
     * Calls {@link #setCurrentTabAndNavigate(DocumentModel, String)} for the
     * current document.
     *
     * @see NavigationContext#getCurrentDocument()
     */
    String setCurrentTabAndNavigate(String currentTabActionId);

    /**
     * Navigate to the given document and opens the view page of the given
     * document selecting the given tab.
     *
     * @param document to document which will be shown in the view page
     * @param currentTabActionId the tab that will be selected in the view page
     * @return the JSF view for the given document.
     */
    String setCurrentTabAndNavigate(DocumentModel document,
            String currentTabActionId);

    @Deprecated
    List<Action> getSubViewActionsList();

    /**
     * @deprecated use {@link WebActions#setCurrentTabId()}
     */
    @Deprecated
    void setCurrentTabAction(String currentTabActionId);

    /**
     * @deprecated since 5.4: useless, and does nothing
     */
    @Deprecated
    void selectTabAction();

    /**
     * @deprecated should be handled by a workflow related (or at least
     *             document) action listener.
     */
    @Deprecated
    String getCurrentLifeCycleState() throws Exception;

    /**
     * @deprecated since 5.4.2: useless
     */
    @Deprecated
    void setTabsList(List<Action> tabsList);

    /**
     * @deprecated since 5.4.2: useless
     */
    @Deprecated
    void setSubTabsList(List<Action> tabsList);

}
