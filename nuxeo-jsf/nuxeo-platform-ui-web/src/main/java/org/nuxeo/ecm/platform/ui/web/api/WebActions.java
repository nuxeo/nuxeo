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
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@Remote
public interface WebActions {

    String NULL_TAB_ID = "";

    List<Action> getActionsList(String category, ActionContext context);

    List<Action> getActionsList(String category);

    List<Action> getUnfiltredActionsList(String category, ActionContext context);

    List<Action> getUnfiltredActionsList(String category);

    List<Action> getAllActions(String category);

    @Deprecated
    List<Action> getSubViewActionsList();

    List<Action> getTabsList();

    void setTabsList(List<Action> tabsList);

    List<Action> getSubTabsList();

    void setSubTabsList(List<Action> tabsList);

    Action getCurrentTabAction();

    void setCurrentTabAction(Action tabAction);

    Action getCurrentSubTabAction();

    void setCurrentSubTabAction(Action tabAction);

    /**
     * @deprecated use {@link WebActions#getCurrentTabId()}
     */
    @Deprecated
    void setCurrentTabAction(String currentTabActionId);

    String getCurrentTabId();

    void setCurrentTabId(String tabId);

    String getCurrentSubTabId();

    void setCurrentSubTabId(String tabId);

    void selectTabAction();

    void resetTabList();

    void resetCurrentTab();

    String setCurrentTabAndNavigate(String currentTabActionId);

    /**
     * This method is used to nagivate to the given document and open the view page of the given document
     * in the given tab argument.
     *
     * @param document to document which will be shown in the view page
     * @param currentTabActionId the tab which will be focused in the view page
     * @return the path to the view page of the given document in the given tab.
     */
    String setCurrentTabAndNavigate(DocumentModel document, String currentTabActionId);

    /**
     * @deprecated should be handled by a workflow related (or at least
     *             document) action listener.
     */
    @Deprecated
    String getCurrentLifeCycleState() throws Exception;
}
