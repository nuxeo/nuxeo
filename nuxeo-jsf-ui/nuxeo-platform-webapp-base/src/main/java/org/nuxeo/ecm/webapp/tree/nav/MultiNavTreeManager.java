/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree.nav;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.action.WebActionsBean;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeDescriptor;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Seam component to handle MultiTree navigation
 * <p>
 * Moved from module nuxeo-platform-virtual-navigation-web, added in 5.6.
 *
 * @since 6.0
 */
@Name("multiNavTreeManager")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class MultiNavTreeManager implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STD_NAV_TREE = "CONTENT_TREE";

    protected String thePath = "";

    @In(required = false, create = true)
    protected WebActionsBean webActions;

    @In(required = false, create = true)
    protected DirectoryTreeManager directoryTreeManager;

    @In(create = true)
    protected Map<String, String> messages;

    public void setSelectedNavigationTree(String selectedNavigationTree) {
        webActions.setCurrentTabId(DirectoryTreeDescriptor.NAV_ACTION_CATEGORY, selectedNavigationTree);
    }

    public String getSelectedNavigationTree() {
        String id = webActions.getCurrentTabId(DirectoryTreeDescriptor.NAV_ACTION_CATEGORY);
        if (id != null) {
            if (id.startsWith(DirectoryTreeDescriptor.ACTION_ID_PREFIX)) {
                return id.substring(DirectoryTreeDescriptor.ACTION_ID_PREFIX.length());
            }
            if (id.startsWith(DirectoryTreeDescriptor.ACTION_ID_PREFIX)) {
                return id.substring(DirectoryTreeDescriptor.ACTION_ID_PREFIX.length());
            }
            return id;
        }
        return null;
    }

    @Observer(value = { WebActions.CURRENT_TAB_CHANGED_EVENT + "_" + DirectoryTreeDescriptor.NAV_ACTION_CATEGORY,
            WebActions.CURRENT_TAB_CHANGED_EVENT + "_" + DirectoryTreeDescriptor.DIR_ACTION_CATEGORY }, create = true)
    public void onCurrentTreeChange(String category, String tabId) {
        // raise this event in order to reset the documents lists from
        // 'conversationDocumentsListsManager'
        Events.instance().raiseEvent(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED,
                DocumentModelFactory.createDocumentModel("Folder"));
        if (tabId != null) {
            if (tabId.startsWith(DirectoryTreeDescriptor.ACTION_ID_PREFIX)) {
                directoryTreeManager.setSelectedTreeName(tabId.substring(DirectoryTreeDescriptor.ACTION_ID_PREFIX.length()));
            }
            if (tabId.startsWith(DirectoryTreeDescriptor.DIR_ACTION_CATEGORY)) {
                directoryTreeManager.setSelectedTreeName(tabId.substring(DirectoryTreeDescriptor.DIR_ACTION_CATEGORY.length()));
            }
        }
    }

    @Observer(value = { "PATH_PROCESSED" }, create = false)
    @BypassInterceptors
    public void setThePath(String myPath) {
        thePath = myPath;
    }

    public String getVirtualNavPath() {
        String[] partOfPath = thePath.split("/");
        String finalPath = "";
        for (String aPart : partOfPath) {
            if (!StringUtils.isBlank(aPart)) {
                finalPath = finalPath + " > " + messages.get(aPart);
            }
        }
        return finalPath;
    }

}
