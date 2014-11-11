/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.tree.nav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.webapp.directory.DirectoryTreeDescriptor;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeService;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for nav trees.
 * <p>
 * Moved from module nuxeo-platform-virtual-navigation-web, originally added in
 * 5.6.
 *
 * @since 6.0
 */
public class NavTreeRegistry extends
        SimpleContributionRegistry<NavTreeDescriptor> {

    @Override
    public String getContributionId(NavTreeDescriptor contrib) {
        return contrib.getTreeId();
    }

    @Override
    public void contributionUpdated(String id, NavTreeDescriptor contrib,
            NavTreeDescriptor newOrigContrib) {
        if (currentContribs.containsKey(id)) {
            currentContribs.remove(id);
        }
        if (contrib.isEnabled()) {
            currentContribs.put(id, contrib);
        }
    }

    // API

    public List<NavTreeDescriptor> getTreeDescriptors(
            DirectoryTreeService directoryTreeService) {
        List<NavTreeDescriptor> allTrees = new ArrayList<NavTreeDescriptor>();
        allTrees.addAll(currentContribs.values());
        List<NavTreeDescriptor> directoryTrees = getDirectoryTrees(directoryTreeService);
        if (directoryTrees != null) {
            allTrees.addAll(directoryTrees);
        }
        Collections.sort(allTrees);
        return allTrees;
    }

    protected List<NavTreeDescriptor> getDirectoryTrees(
            DirectoryTreeService directoryTreeService) {
        if (directoryTreeService == null) {
            return null;
        }
        List<String> treeNames = directoryTreeService.getNavigationDirectoryTrees();
        List<NavTreeDescriptor> trees = new ArrayList<NavTreeDescriptor>();
        for (String dTreeName : treeNames) {
            DirectoryTreeDescriptor desc = directoryTreeService.getDirectoryTreeDescriptor(dTreeName);
            trees.add(new NavTreeDescriptor(dTreeName, desc.getLabel(), true));
        }
        return trees;
    }

}
