/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Moved from module nuxeo-platform-virtual-navigation-web, originally added in 5.6.
 *
 * @since 6.0
 */
public class NavTreeRegistry extends SimpleContributionRegistry<NavTreeDescriptor> {

    @Override
    public String getContributionId(NavTreeDescriptor contrib) {
        return contrib.getTreeId();
    }

    @Override
    public void contributionUpdated(String id, NavTreeDescriptor contrib, NavTreeDescriptor newOrigContrib) {
        if (currentContribs.containsKey(id)) {
            currentContribs.remove(id);
        }
        if (contrib.isEnabled()) {
            currentContribs.put(id, contrib);
        }
    }

    // API

    public List<NavTreeDescriptor> getTreeDescriptors(DirectoryTreeService directoryTreeService) {
        List<NavTreeDescriptor> allTrees = new ArrayList<>();
        allTrees.addAll(currentContribs.values());
        List<NavTreeDescriptor> directoryTrees = getDirectoryTrees(directoryTreeService);
        if (directoryTrees != null) {
            allTrees.addAll(directoryTrees);
        }
        Collections.sort(allTrees);
        return allTrees;
    }

    protected List<NavTreeDescriptor> getDirectoryTrees(DirectoryTreeService directoryTreeService) {
        if (directoryTreeService == null) {
            return null;
        }
        List<String> treeNames = directoryTreeService.getNavigationDirectoryTrees();
        List<NavTreeDescriptor> trees = new ArrayList<>();
        for (String dTreeName : treeNames) {
            DirectoryTreeDescriptor desc = directoryTreeService.getDirectoryTreeDescriptor(dTreeName);
            trees.add(new NavTreeDescriptor(dTreeName, desc.getLabel(), true));
        }
        return trees;
    }

}
