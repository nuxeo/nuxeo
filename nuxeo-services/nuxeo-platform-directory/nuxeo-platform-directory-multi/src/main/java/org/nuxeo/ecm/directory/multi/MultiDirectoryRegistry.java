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
package org.nuxeo.ecm.directory.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class MultiDirectoryRegistry extends
        ContributionFragmentRegistry<MultiDirectoryDescriptor> {

    private static final Log log = LogFactory.getLog(MultiDirectoryRegistry.class);

    protected Map<String, MultiDirectoryDescriptor> descriptors = new HashMap<String, MultiDirectoryDescriptor>();

    // cache of directories
    protected Map<String, MultiDirectory> directories = new HashMap<String, MultiDirectory>();

    @Override
    public String getContributionId(MultiDirectoryDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id,
            MultiDirectoryDescriptor descriptor,
            MultiDirectoryDescriptor newOrigContrib) {
        String name = descriptor.name;
        if (descriptor.remove) {
            contributionRemoved(id, descriptor);
        } else {
            if (descriptors.containsKey(name)) {
                log.info("Directory registration updated: " + name);
            } else {
                log.info("Directory registered: " + name);
            }
            descriptors.put(id, descriptor);
            MultiDirectory directory = new MultiDirectory(descriptor);
            directories.put(name, directory);
        }
    }

    @Override
    public void contributionRemoved(String id,
            MultiDirectoryDescriptor origContrib) {
        descriptors.remove(id);
        Directory dir = directories.remove(id);
        if (dir != null) {
            try {
                dir.shutdown();
            } catch (DirectoryException e) {
                log.error(String.format(
                        "Error while shutting down directory '%s'", id), e);
            }
        }
        log.info("Directory removed: " + id);
    }

    @Override
    public MultiDirectoryDescriptor clone(MultiDirectoryDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(MultiDirectoryDescriptor src, MultiDirectoryDescriptor dst) {
        boolean remove = src.remove;
        // keep old remove info: if old contribution was removed, new one
        // should replace the old one completely
        boolean wasRemoved = dst.remove;
        if (remove) {
            dst.remove = remove;
            // don't bother merging
            return;
        }

        dst.merge(src, wasRemoved);
    }

    // API

    public MultiDirectory getDirectory(String name) {
        return directories.get(name);
    }

    public List<Directory> getDirectories() {
        List<Directory> res = new ArrayList<Directory>();
        res.addAll(directories.values());
        return res;
    }

}
