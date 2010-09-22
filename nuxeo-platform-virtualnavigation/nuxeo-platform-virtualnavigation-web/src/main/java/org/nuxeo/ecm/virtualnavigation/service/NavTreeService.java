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
import java.util.List;

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
 */
public class NavTreeService extends DefaultComponent {

    public static String NAVTREE_EP = "navigationTree";

    protected List<NavTreeDescriptor> descriptors;

    protected boolean directoryTreesFetched;

    public List<NavTreeDescriptor> getTreeDescriptors() {
        maybeFetchDirectoryTrees();
        return descriptors;
    }

    protected synchronized void maybeFetchDirectoryTrees() {
        if (directoryTreesFetched) {
            return;
        }
        DirectoryTreeService directoryTreeService = (DirectoryTreeService) Framework.getRuntime().getComponent(
                DirectoryTreeService.NAME);
        if (directoryTreeService == null) {
            return;
        }
        List<String> treeNames = directoryTreeService.getNavigationDirectoryTrees();
        for (String dTreeName : treeNames) {
            descriptors.add(new NavTreeDescriptor(dTreeName, "label."
                    + dTreeName, true));
        }
        directoryTreesFetched = true;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (NAVTREE_EP.equals(extensionPoint)) {
            descriptors.add((NavTreeDescriptor) contribution);
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        directoryTreesFetched = false;
        descriptors = new ArrayList<NavTreeDescriptor>();
    }

}
