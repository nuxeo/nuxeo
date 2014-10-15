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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.seam.NuxeoSeamHotReloader;
import org.nuxeo.runtime.api.Framework;

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

    protected List<NavTreeDescriptor> availableNavigationTrees;

    protected Long availableNavigationTreesTimestamp;

    protected String selectedNavigationTree;

    protected String thePath = "";

    @In(required = false, create = true)
    protected DirectoryTreeManager directoryTreeManager;

    @In(create = true)
    protected NuxeoSeamHotReloader seamReload;

    @In(create = true)
    protected Map<String, String> messages;

    public List<NavTreeDescriptor> getAvailableNavigationTrees() {
        if (availableNavigationTrees == null || shouldResetCache()) {
            availableNavigationTrees = new ArrayList<NavTreeDescriptor>();

            // add registered additional tress
            NavTreeService navTreeService = Framework.getLocalService(NavTreeService.class);
            availableNavigationTrees.addAll(navTreeService.getTreeDescriptors());
            availableNavigationTreesTimestamp = navTreeService.getLastModified();
        }
        return availableNavigationTrees;
    }

    /**
     * Checks timestamp on service to handle cache reset when using hot reload
     *
     * @since 5.6
     */
    protected boolean shouldResetCache() {
        NavTreeService navTreeService = Framework.getLocalService(NavTreeService.class);
        if (seamReload.isDevModeSet()
                && seamReload.shouldResetCache(navTreeService,
                        availableNavigationTreesTimestamp)) {
            return true;
        }
        return false;
    }

    @Factory(value = "selectedNavigationTree", scope = ScopeType.EVENT)
    public String getSelectedNavigationTree() {
        if (selectedNavigationTree == null) {
            List<NavTreeDescriptor> trees = getAvailableNavigationTrees();
            if (trees != null && trees.size() > 0) {
                setSelectedNavigationTree(trees.get(0).getTreeId());
            } else {
                setSelectedNavigationTree(STD_NAV_TREE); // !
            }
        }
        return selectedNavigationTree;
    }

    @Factory(value = "selectedNavigationTreeDescriptor", scope = ScopeType.EVENT)
    public NavTreeDescriptor getSelectedNavigationTreeDescriptor() {
        String navTreeName = getSelectedNavigationTree();
        for (NavTreeDescriptor desc : getAvailableNavigationTrees()) {
            if (desc.getTreeId().equals(navTreeName)) {
                return desc;
            }
        }

        return null;
    }

    public void setSelectedNavigationTree(String selectedNavigationTree) {
        directoryTreeManager.setSelectedTreeName(selectedNavigationTree);
        this.selectedNavigationTree = selectedNavigationTree;
        // raise this event in order to reset the documents lists from
        // 'conversationDocumentsListsManager'
        Events.instance().raiseEvent(
                EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED,
                new DocumentModelImpl("Folder"));
        // since JSF2 upgrade, both variables are retrieved at restore phase,
        // and action is called after => need to force re-compute of variables
        // after action
        Context eventContext = Contexts.getEventContext();
        eventContext.remove("selectedNavigationTree");
        eventContext.remove("selectedNavigationTreeDescriptor");
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
            finalPath = finalPath + " > " + messages.get(aPart);
        }
        return finalPath;
    }

}
