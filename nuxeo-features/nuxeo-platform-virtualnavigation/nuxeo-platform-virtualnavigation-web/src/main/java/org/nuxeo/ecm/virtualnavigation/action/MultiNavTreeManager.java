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

package org.nuxeo.ecm.virtualnavigation.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.virtualnavigation.service.NavTreeService;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component to handle MultiTree navigation
 */
@Name("multiNavTreeManager")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class MultiNavTreeManager implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STD_NAV_TREE = "CONTENT_TREE";

    public static final String STD_NAV_TREE_LABEL = "label.content.tree";

    protected List<NavTreeDescriptor> availableNavigationTrees;

    protected String thePath = "";

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(required = false, create = true)
    protected DirectoryTreeManager directoryTreeManager;

    private String selectedNavigationTree;

    public List<NavTreeDescriptor> getAvailableNavigationTrees() {
        if (availableNavigationTrees == null) {
            availableNavigationTrees = new ArrayList<NavTreeDescriptor>();
            // default tree
            availableNavigationTrees.add(new NavTreeDescriptor(STD_NAV_TREE,
                    STD_NAV_TREE_LABEL));

            // add registred additional tress
            NavTreeService navTreeService = Framework.getLocalService(NavTreeService.class);
            availableNavigationTrees.addAll(navTreeService.getTreeDescriptors());

        }
        return availableNavigationTrees;
    }

    @Factory(value = "selectedNavigationTree", scope = ScopeType.EVENT)
    public String getSelectedNavigationTree() {
        if (selectedNavigationTree == null) {
            setSelectedNavigationTree(STD_NAV_TREE);
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
            finalPath = finalPath + " > "
                    + resourcesAccessor.getMessages().get(aPart);
        }
        return finalPath;
    }

}
