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
package org.nuxeo.ecm.webapp.directory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for directory tree descriptors
 *
 * @since 5.6
 */
public class DirectoryTreeRegistry extends
        ContributionFragmentRegistry<DirectoryTreeDescriptor> {

    private static final Log log = LogFactory.getLog(DirectoryTreeRegistry.class);

    protected Map<String, DirectoryTreeDescriptor> registry = new HashMap<String, DirectoryTreeDescriptor>();

    @Override
    public String getContributionId(DirectoryTreeDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, DirectoryTreeDescriptor contrib,
            DirectoryTreeDescriptor newOrigContrib) {
        if (registry.containsKey(contrib.getName())) {
            DirectoryTreeDescriptor existing_descriptor = registry.get(contrib.getName());
            existing_descriptor.merge(contrib);
            log.debug("merged DirectoryTreeDescriptor: " + contrib.getName());
        } else {
            registry.put(contrib.getName(), contrib);
            log.debug("registered DirectoryTreeDescriptor: "
                    + contrib.getName());
        }
    }

    @Override
    public void contributionRemoved(String id, DirectoryTreeDescriptor contrib) {
        registry.remove(contrib.getName());
        log.debug("unregistered DirectoryTreeDescriptor: " + contrib.getName());
    }

    @Override
    public DirectoryTreeDescriptor clone(DirectoryTreeDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(DirectoryTreeDescriptor src, DirectoryTreeDescriptor dst) {
        dst.merge(src);
    }

    // API

    public List<String> getDirectoryTrees() {
        List<String> directoryTrees = new ArrayList<String>();
        for (DirectoryTreeDescriptor desc : registry.values()) {
            if (Boolean.TRUE.equals(desc.getEnabled())) {
                directoryTrees.add(desc.getName());
            }
        }
        Collections.sort(directoryTrees);
        return directoryTrees;
    }

    public DirectoryTreeDescriptor getDirectoryTreeDescriptor(String treeName) {
        DirectoryTreeDescriptor desc = registry.get(treeName);
        if (desc != null && Boolean.TRUE.equals(desc.getEnabled())) {
            return desc;
        } else {
            return null;
        }
    }

    /**
     * Returns only the enabled Directory Trees marked as being also Navigation
     * Trees.
     */
    public List<String> getNavigationDirectoryTrees() {
        List<String> directoryTrees = new ArrayList<String>();
        for (DirectoryTreeDescriptor desc : registry.values()) {
            if (Boolean.TRUE.equals(desc.getEnabled())
                    && desc.isNavigationTree()) {
                directoryTrees.add(desc.getName());
            }
        }
        Collections.sort(directoryTrees);
        return directoryTrees;
    }

}
