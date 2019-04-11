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
public class DirectoryTreeRegistry extends ContributionFragmentRegistry<DirectoryTreeDescriptor> {

    private static final Log log = LogFactory.getLog(DirectoryTreeRegistry.class);

    protected Map<String, DirectoryTreeDescriptor> registry = new HashMap<>();

    @Override
    public String getContributionId(DirectoryTreeDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, DirectoryTreeDescriptor contrib, DirectoryTreeDescriptor newOrigContrib) {
        if (registry.containsKey(contrib.getName())) {
            DirectoryTreeDescriptor existing_descriptor = registry.get(contrib.getName());
            existing_descriptor.merge(contrib);
            log.debug("merged DirectoryTreeDescriptor: " + contrib.getName());
        } else {
            registry.put(contrib.getName(), contrib);
            log.debug("registered DirectoryTreeDescriptor: " + contrib.getName());
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
        List<String> directoryTrees = new ArrayList<>();
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
     * Returns only the enabled Directory Trees marked as being also Navigation Trees.
     */
    public List<String> getNavigationDirectoryTrees() {
        List<String> directoryTrees = new ArrayList<>();
        for (DirectoryTreeDescriptor desc : registry.values()) {
            if (Boolean.TRUE.equals(desc.getEnabled()) && desc.isNavigationTree()) {
                directoryTrees.add(desc.getName());
            }
        }
        Collections.sort(directoryTrees);
        return directoryTrees;
    }

}
