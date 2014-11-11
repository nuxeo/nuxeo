/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.virtualnavigation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.virtualnavigation.action.NavTreeDescriptor;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Very simple component to manage Navigation tree registration
 *
 * @author Thierry Delprat
 * @author Thierry Martins
 */
public class NavTreeService extends DefaultComponent {

    public static String NAVTREE_EP = "navigationTree";

    protected Map<String, NavTreeDescriptor> registry;

    public List<NavTreeDescriptor> getTreeDescriptors() {
        List<NavTreeDescriptor> allTrees = new ArrayList<NavTreeDescriptor>();
        allTrees.addAll(registry.values());
        List<NavTreeDescriptor> directoryTrees = getDirectoryTrees();
        if (directoryTrees != null) {
            allTrees.addAll(directoryTrees);
        }
        Collections.sort(allTrees, NavTreeDescriptorOrderComparator.INSTANCE);
        return allTrees;
    }

    protected synchronized List<NavTreeDescriptor> getDirectoryTrees() {
        DirectoryTreeService directoryTreeService = (DirectoryTreeService) Framework.getRuntime().getComponent(
                DirectoryTreeService.NAME);
        if (directoryTreeService == null) {
            return null;
        }
        List<String> treeNames = directoryTreeService.getNavigationDirectoryTrees();
        List<NavTreeDescriptor> trees = new ArrayList<NavTreeDescriptor>();
        for (String dTreeName : treeNames) {
            trees.add(new NavTreeDescriptor(dTreeName, "label." + dTreeName,
                    true));
        }
        return trees;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (NAVTREE_EP.equals(extensionPoint)) {
            NavTreeDescriptor contrib = (NavTreeDescriptor) contribution;
            if (registry.containsKey(contrib.getTreeId())) {
                registry.remove(contrib.getTreeId());
            }
            if (contrib.isEnabled()) {
                registry.put(contrib.getTreeId(), contrib);
            }
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        registry = new HashMap<String, NavTreeDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    /**
     * Comparator of {@link NavTreeDescriptor}s according to their order..
     */
    public static class NavTreeDescriptorOrderComparator implements
            Comparator<NavTreeDescriptor> {

        public static final NavTreeDescriptorOrderComparator INSTANCE = new NavTreeDescriptorOrderComparator();

        @Override
        public int compare(NavTreeDescriptor descriptor1,
                NavTreeDescriptor descriptor2) {
            return descriptor1.getOrder() - descriptor2.getOrder();
        }
    }

}
