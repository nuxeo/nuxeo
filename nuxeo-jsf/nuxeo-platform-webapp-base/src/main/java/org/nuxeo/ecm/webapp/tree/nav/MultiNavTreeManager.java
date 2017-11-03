/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree.nav;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
