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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Seam component to handle MultiTree navigation
 */
@Name("multiNavTreeManager")
@Scope(CONVERSATION)
public class MultiNavTreeManager implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(MultiNavTreeManager.class);

    public final static String STD_NAV_TREE = "CONTENT_TREE";

    public final static String STD_NAV_TREE_LABEL = "label.content.tree";

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(required = false, create = true)
    protected DirectoryTreeManager directoryTreeManager;

    private static List<NavTreeDescriptor> availableNavigationTrees;

    private static String thePath = "";

    private String selectedNavigationTree;

    public List<NavTreeDescriptor> getAvailableNavigationTrees() {
        if (availableNavigationTrees == null) {
            availableNavigationTrees = new ArrayList<NavTreeDescriptor>();
            availableNavigationTrees.add(new NavTreeDescriptor(STD_NAV_TREE,
                    STD_NAV_TREE_LABEL));
            for (String dTreeName : directoryTreeManager.getDirectoryTreeNames()) {
                availableNavigationTrees.add(new NavTreeDescriptor(dTreeName,
                        "label." + dTreeName));
            }
        }
        return availableNavigationTrees;
    }

    public String getSelectedNavigationTree() {
        if (selectedNavigationTree == null) {
            setSelectedNavigationTree(STD_NAV_TREE);
        }
        return selectedNavigationTree;
    }

    public void setSelectedNavigationTree(String selectedNavigationTree) {
        directoryTreeManager.setSelectedTreeName(selectedNavigationTree);
        this.selectedNavigationTree = selectedNavigationTree;
    }

    @Observer(value = { "PATH_PROCESSED" }, create = false)
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
